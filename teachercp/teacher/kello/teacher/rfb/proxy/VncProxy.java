package kello.teacher.rfb.proxy;

/*
 * Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
 * Copyright 2006 Lorenzo Keller <lorenzo.keller@gmail.com>
 * 
 *     This file is part of Teacher.
 *
 *   Teacher is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   Teacher is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Teacher; if not, write to the Free Software
 *   Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 */

import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import kello.teacher.protocol.RfbServerDescription;
import kello.teacher.rfb.DH;
import kello.teacher.rfb.DesCipher;
import kello.teacher.rfb.DesCipher2;
import kello.teacher.rfb.MulticastRfbOptions;
import kello.teacher.rfb.RfbProto;
import kello.teacher.util.ExceptionDialogBox;

public class VncProxy implements java.lang.Runnable {

  /** socket used to send the packets */
  private MulticastSocket socket;

  private MulticastRfbOptions options;

  InetAddress dest;

  /**
   * this variable is set to true if a new client has attached
   * to the proxy and it needs a fullscreen update
   */
  private boolean newClient = false;

  /**
   * Stores the time at which the new client was notified
   */
  private long newClientTime;

  private String host;

  private int port;

  private String username;
  private String password;

  private RfbProto rfb;

  private Thread rfbThread;

  private boolean started;

  private long lastNonIncrementalUpdate;

  private final int partitions = 12;

  private int part;

  private Vector rects = new Vector();

  /** stores all the tiles of the current line while repacking Hextile */
  private byte[] line = new byte[1000];

  /** this buffer will store lines that fit in a packet while repacking Hextile */
  private byte[] lines = new byte[1000];

  /** background for Hextile repack */
  private byte background = 60;
  /** foreground for Hextile repack */
  private byte foreground = 18;

  /** this buffer stores a packet that will be sent */
  private byte[] packet = new byte[65000];

  /** this buffer is used by some methods as temporary buffer */
  private byte[] pixels;

  public VncProxy(String host, int port, String username, String password,
      String multicastGroup, int multicastPort, int ttl) throws UnknownHostException {

    this.host = host;
    this.port = port;
    this.password = password;
    this.username = username;

    this.dest = InetAddress.getByName(multicastGroup);
    this.options = new MulticastRfbOptions();
    this.options.setMulticastGroup(multicastGroup);
    this.options.setMulticastPort(multicastPort);
    this.options.setTtl(ttl);

    try {
      this.socket = new MulticastSocket();
      this.socket.setTimeToLive(ttl);
      this.socket.setSendBufferSize(65500);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public synchronized void stop() {
    if (this.rfbThread != null) {
      if (this.rfb != null) {
        this.rfb.close();
      }
      this.rfbThread = null;
      this.socket.close();
    }
  }

  public synchronized void start() {

    if (this.started) {
      return;
    }

    try {
      connectAndAuthenticate();

      doProtocolInitialisation();

      this.rfb.writeSetPixelFormat(8, 8, false, true, 7, 7, 3, 0, 3, 6);

      this.rfb.writeFramebufferUpdateRequest(0, 0, this.rfb.framebufferWidth,
          this.rfb.framebufferHeight, false);

      this.started = true;

      this.rfbThread = new Thread(this);
      this.rfbThread.start();

    } catch (EOFException ex) {
      System.out.println("Multicast stopped");
    } catch (SocketException ex) {
      System.out.println("Multicast stopped");
    } catch (Exception e) {
      ExceptionDialogBox.displayExceptionDialog(null, e, "Error while communicationg with the Teacher screen");
    }

  }

  public void writeRectHeader(byte[] rect) {
    writeRectHeader(rect, this.rfb.updateRectX, this.rfb.updateRectY, this.rfb.updateRectW, this.rfb.updateRectH);
  }

  public void writeRectHeader(byte[] rect, int x, int y, int w, int h) {

    rect[0] = (byte) ((x & 0xFF00) >> 8);
    rect[1] = (byte) ((x & 0x00FF));

    rect[2] = (byte) ((y & 0xFF00) >> 8);
    rect[3] = (byte) ((y & 0x00FF));

    rect[4] = (byte) ((w & 0xFF00) >> 8);
    rect[5] = (byte) ((w & 0x00FF));

    rect[6] = (byte) ((h & 0xFF00) >> 8);
    rect[7] = (byte) ((h & 0x00FF));

    rect[8] = (byte) ((this.rfb.updateRectEncoding & 0xFF000000) >> 24);
    rect[9] = (byte) ((this.rfb.updateRectEncoding & 0x00FF0000) >> 16);
    rect[10] = (byte) ((this.rfb.updateRectEncoding & 0x0000FF00) >> 8);
    rect[11] = (byte) ((this.rfb.updateRectEncoding & 0x000000FF));

  }

  /* send multicast datagram */
  public void send(byte[] b, int off, int len) {
    DatagramPacket packet = new DatagramPacket(b, len, this.dest, this.options.getMulticastPort());
    try {
      this.socket.send(packet);
    } catch (IOException e) {
      System.err.println(e);
      e.printStackTrace();
    }
  }

  //
  // run() - executed by the rfbThread to deal with the RFB socket.
  //

  @Override
  public void run() {

    try {

      /* initialise the pixels buffer with a sufficiently large size */
      this.pixels = new byte[this.rfb.framebufferWidth * this.rfb.framebufferHeight];

      /* initialise the packet buffer with a valid header */
      /* -- type FramebufferUpdate */
      this.packet[0] = 0;
      /* -- padding */
      this.packet[1] = 0;
      /* -- number of rectangles */
      this.packet[2] = 0;
      this.packet[3] = 1;

      /* initialise the non incremental update timer */
      this.lastNonIncrementalUpdate = new Date().getTime();

      /* initialise the parttion count to the first partition */
      this.part = 0;

      while (true) {

        int msgType = this.rfb.readServerMessageType();

        switch (msgType) {
          case RfbProto.FramebufferUpdate:

            processFramebufferUpdate();
            break;

          case RfbProto.SetColourMapEntries:

            throw new IOException("Can't handle SetColourMapEntries message");

          case RfbProto.Bell:

            break;

          case RfbProto.ServerCutText:

            this.rfb.readServerCutText();
            break;

          default:
            throw new IOException("Unknown RFB message type " + msgType);
        }
      }
    } catch (EOFException ex) {
      System.out.println("Multicast stopped");
    } catch (SocketException ex) {
      System.out.println("Multicast stopped");
    } catch (Exception e) {
      ExceptionDialogBox.displayExceptionDialog(null, e, "Error broadcasting the teacher screen");
    }
  }

  private long lastRequest = 0;

  private void processFramebufferUpdate() throws IOException, InterruptedException {

    this.rfb.readFramebufferUpdate();

    for (int i = 0; i < this.rfb.updateNRects; i++) {

      this.rfb.readFramebufferUpdateRectHdr();
      byte[] rect;

      switch (this.rfb.updateRectEncoding) {

        case RfbProto.EncodingRaw:

          rect = parseRawRect();
          this.rects.add(rect);
          break;

        case RfbProto.EncodingCopyRect:

          rect = parseCopyRect();
          this.rects.add(rect);
          break;

        case RfbProto.EncodingRRE: {

          rect = parseRRERect();
          this.rects.add(rect);
          break;
        }

        case RfbProto.EncodingCoRRE: {

          rect = parseCoRRERect();
          this.rects.add(rect);
          break;
        }

        case RfbProto.EncodingHextile: {

          Object tiles[][];
          tiles = parseHextileRect(this.pixels);
          repackHextileRect(tiles);
          break;
        }

        default:
          throw new IOException("Unknown RFB rectangle encoding " + this.rfb.updateRectEncoding);
      }
    }

    sendRects();

    requestUpdate();
  }

  private void sendRects() {
    Iterator i = this.rects.iterator();

    while (i.hasNext()) {
      byte[] rect = (byte[]) (i.next());
      // packet[1] = (byte) id;
      // id = (short) ((id + 1) % 256);
      if (rect.length < this.packet.length - 4) {
        System.arraycopy(rect, 0, this.packet, 4, rect.length);
        send(this.packet, 0, 4 + rect.length);
      } else {
        System.err.println("*** LONG PACKET (" + rect.length + ") *****");
      }

    }

    this.rects.clear();
  }

  private void repackHextileRect(Object[][] tiles) {
    byte[] rect;
    /* stores the y origin of the lines stored in lines */
    int linesY = this.rfb.updateRectY;

    /* pointer to the next free position in the lines buffer */
    int lp = 0;

    for (int y = 0; y < tiles[0].length; y++) {

      /* pointer to the last free position in the line buffer */
      int cp = 0;

      int th = 16;

      /* x origin of the tiles stored in line */
      int lineX = this.rfb.updateRectX;

      /* the tile may be shorter than 16 pixels */
      if (this.rfb.updateRectH - y * 16 < 16) {
        th = this.rfb.updateRectH - y * 16;
      } else {
        th = 16;
      }

      /* compute the length of the next line of tiles */
      int lineLength = 0;
      for (int x = 0; x < tiles.length; x++) {
        byte[] tile = (byte[]) tiles[x][y];
        lineLength += tile.length;
      }

      /*
       * the new line don't fit in the packet create a rectangle with the
       * previous lines
       */
      if (lineLength <= this.line.length && lp + lineLength > this.lines.length) {
        // System.out.println("page break");
        rect = new byte[12 + lp];
        writeRectHeader(rect, this.rfb.updateRectX, linesY, this.rfb.updateRectW, y * 16 - (linesY - this.rfb.updateRectY));
        linesY = y * 16 + this.rfb.updateRectY;
        System.arraycopy(this.lines, 0, rect, 12, lp);
        lp = 0;
        this.rects.add(rect);

        fixTileBackground(tiles, y, this.background, 0);
        fixTileForeground(tiles, y, this.foreground, 0);

        /* recompute the line length because it has changed */
        for (int x = 0; x < tiles.length; x++) {
          byte[] tile = (byte[]) tiles[x][y];
          lineLength += tile.length;
        }

      }

      /* previous lines as a rectangle if the next line has to be splitted */
      if (lineLength > this.line.length && lp > 0) {
        // System.out.println("previous lines");
        rect = new byte[12 + lp];
        writeRectHeader(rect, this.rfb.updateRectX, linesY, this.rfb.updateRectW, y * 16 - (linesY - this.rfb.updateRectY));
        linesY = y * 16 + this.rfb.updateRectY;
        System.arraycopy(this.lines, 0, rect, 12, lp);
        lp = 0;

        fixTileBackground(tiles, y, this.background, 0);
        fixTileForeground(tiles, y, this.foreground, 0);

        this.rects.add(rect);
      }

      for (int x = 0; x < tiles.length; x++) {

        byte[] tile = (byte[]) tiles[x][y];

        if ((tile[0] & RfbProto.HextileRaw) == 0 && (tile[0] & RfbProto.HextileBackgroundSpecified) != 0) {
          this.background = tile[1];
        }
        if ((tile[0] & RfbProto.HextileRaw) == 0 && (tile[0] & RfbProto.HextileForegroundSpecified) != 0) {
          this.foreground = tile[((tile[0] & RfbProto.HextileBackgroundSpecified) != 0 ? 2 : 1)];
        }

        /*
         * if the current line of tiles can't fit in a packet
         * split it in two and create a rectangle with the previous lines
         */
        if (cp + tile.length > this.line.length) {

          // System.out.println("line break");

          /* add the line as a rectangle */
          rect = new byte[12 + cp];
          writeRectHeader(rect, lineX, this.rfb.updateRectY + y * 16, 16 * x - (lineX - this.rfb.updateRectX), th);
          lineX = this.rfb.updateRectX + 16 * x;
          System.arraycopy(this.line, 0, rect, 12, cp);
          cp = 0;
          this.rects.add(rect);

          /*
           * add the background data if is not set to the next tile because
           * it will not be set otherwise (we are splitting the packet)
           */

          fixTileBackground(tiles, y, this.background, x);
          fixTileForeground(tiles, y, this.foreground, x);

          /* reload the right tile from the array */
          tile = (byte[]) tiles[x][y];

        }

        System.arraycopy(tile, 0, this.line, cp, tile.length);
        cp += tile.length;

      }

      /*
       * if the line was splitted create a rectangle with the remaining part of
       * the line
       */
      if (lineX != this.rfb.updateRectX) {

        // System.out.println("finish line break");
        rect = new byte[12 + cp];
        writeRectHeader(rect, lineX, this.rfb.updateRectY + y * 16, this.rfb.updateRectW - (lineX - this.rfb.updateRectX), th);
        linesY += th;
        System.arraycopy(this.line, 0, rect, 12, cp);

        if (y < tiles[0].length) {
          fixTileBackground(tiles, y + 1, this.background, 0);
          fixTileForeground(tiles, y + 1, this.foreground, 0);
        }

        this.rects.add(rect);

      } else {

        System.arraycopy(this.line, 0, this.lines, lp, cp);
        lp += cp;
      }
    }
    if (lp > 0) {
      // System.out.println("residual");
      rect = new byte[12 + lp];
      writeRectHeader(rect, this.rfb.updateRectX, linesY, this.rfb.updateRectW, this.rfb.updateRectH - (linesY - this.rfb.updateRectY));
      System.arraycopy(this.lines, 0, rect, 12, lp);
      this.rects.add(rect);
    }
  }

  private Object[][] parseHextileRect(byte[] pixels) throws IOException {
    Object[][] tiles;
    tiles = new Object[(this.rfb.updateRectW / 16) + (this.rfb.updateRectW % 16 == 0 ? 0 : 1)][(this.rfb.updateRectH / 16) + (this.rfb.updateRectH % 16 == 0 ? 0 : 1)];

    for (int ty = this.rfb.updateRectY; ty < this.rfb.updateRectY + this.rfb.updateRectH; ty += 16) {
      for (int tx = this.rfb.updateRectX; tx < this.rfb.updateRectX + this.rfb.updateRectW; tx += 16) {

        byte[] tile;

        int tw = 16, th = 16;

        if (this.rfb.updateRectX + this.rfb.updateRectW - tx < 16) {
          tw = this.rfb.updateRectX + this.rfb.updateRectW
              - tx;
        }
        if (this.rfb.updateRectY + this.rfb.updateRectH - ty < 16) {
          th = this.rfb.updateRectY + this.rfb.updateRectH
              - ty;
        }

        int subencoding = this.rfb.is.read();

        if ((subencoding & RfbProto.HextileRaw) != 0) {

          tile = new byte[tw * th + 1];
          // the pixels are not read correctly in the pixels buffer but
          // these are not used so it is not important
          this.rfb.is.readFully(tile, 1, tw * th);

          tile[0] = (byte) (subencoding & 0xFF);
          tiles[(tx - this.rfb.updateRectX) / 16][(ty - this.rfb.updateRectY) / 16] = tile;
          continue;
        }

        tile = pixels;
        tile[0] = (byte) (subencoding & 0xFF);

        int cp = 1;

        if ((subencoding & RfbProto.HextileBackgroundSpecified) != 0) {
          tile[cp++] = (byte) this.rfb.is.read();
        }

        if ((subencoding & RfbProto.HextileForegroundSpecified) != 0) {
          tile[cp++] = (byte) this.rfb.is.read();
        }

        if ((subencoding & RfbProto.HextileAnySubrects) != 0) {

          int nSubrects = this.rfb.is.read();

          tile[cp++] = (byte) nSubrects;

          if ((subencoding & RfbProto.HextileSubrectsColoured) != 0) {

            this.rfb.is.readFully(tile, cp, nSubrects * 3);

            cp += nSubrects * 3;

          } else {

            this.rfb.is.readFully(tile, cp, nSubrects * 2);
            cp += nSubrects * 2;

          }
        }

        byte[] realTile = new byte[cp];
        System.arraycopy(tile, 0, realTile, 0, cp);

        tiles[(tx - this.rfb.updateRectX) / 16][(ty - this.rfb.updateRectY) / 16] = realTile;

      }

    }
    return tiles;
  }

  private byte[] parseCoRRERect() throws IOException {
    byte[] rect;
    // FIXME: the protocol is not correctly implemented (bytes per pixel only 8)
    int nSubrects = this.rfb.is.readInt();

    rect = new byte[12 + 5 + 5 * nSubrects];

    writeRectHeader(rect);

    rect[12] = (byte) ((nSubrects & 0xFF000000) >> 24);
    rect[13] = (byte) ((nSubrects & 0x00FF0000) >> 16);
    rect[14] = (byte) ((nSubrects & 0x0000FF00) >> 8);
    rect[15] = (byte) ((nSubrects & 0x000000FF));

    this.rfb.is.readFully(rect, 12 + 4, 1 + 5 * nSubrects);
    return rect;
  }

  private byte[] parseRRERect() throws IOException {
    byte[] rect;
    // FIXME: the protocol is not correctly implemented (bytes per pixel only 8)
    int nSubrects = this.rfb.is.readInt();

    rect = new byte[12 + 5 + 9 * nSubrects];

    writeRectHeader(rect);

    rect[12] = (byte) ((nSubrects & 0xFF000000) >> 24);
    rect[13] = (byte) ((nSubrects & 0x00FF0000) >> 16);
    rect[14] = (byte) ((nSubrects & 0x0000FF00) >> 8);
    rect[15] = (byte) ((nSubrects & 0x000000FF));

    this.rfb.is.readFully(rect, 12 + 4, 1 + 9 * nSubrects);
    return rect;
  }

  private byte[] parseCopyRect() throws IOException {
    byte[] rect;
    rect = new byte[12 + 4];

    writeRectHeader(rect);

    this.rfb.readCopyRect();
    // System.out.println("copyrect");

    rect[12] = (byte) ((this.rfb.copyRectSrcX & 0xFF00) >> 8);
    rect[13] = (byte) ((this.rfb.copyRectSrcX & 0xFF));

    rect[14] = (byte) ((this.rfb.copyRectSrcY & 0xFF00) >> 8);
    rect[15] = (byte) ((this.rfb.copyRectSrcY & 0xFF));
    return rect;
  }

  private byte[] parseRawRect() throws IOException {
    byte[] rect;
    rect = new byte[12 + this.rfb.updateRectH * this.rfb.updateRectW];

    writeRectHeader(rect);

    this.rfb.is.readFully(rect, 12, this.rfb.updateRectH * this.rfb.updateRectW);
    return rect;
  }

  private synchronized void requestUpdate() throws IOException, InterruptedException {

    long now = new Date().getTime();

    /* make sure the maximum update request rate is 1 frame every 200 ms */
    if (now - this.lastRequest < 200) {
      Thread.sleep(200 - (now - this.lastRequest));
    }

    this.lastRequest = now;

    long deltaInc = (now - this.lastNonIncrementalUpdate);

    if (this.newClient && (now - this.newClientTime > 2000)) {
      this.rfb.writeFramebufferUpdateRequest(0, 0,
          this.rfb.framebufferWidth, this.rfb.framebufferHeight, false);
      this.newClient = false;
      this.lastNonIncrementalUpdate = new Date().getTime();
      System.out.println("*********** FULL REFRESH ************");
    } else if (deltaInc > 1000) {
      this.rfb.writeFramebufferUpdateRequest(0, this.rfb.framebufferHeight / this.partitions * this.part,
          this.rfb.framebufferWidth, this.rfb.framebufferHeight / this.partitions, false);
      if (++this.part == this.partitions) {
        this.part = 0;
      }
      this.lastNonIncrementalUpdate = new Date().getTime();
    } else {
      this.rfb.writeFramebufferUpdateRequest(0, 0,
          this.rfb.framebufferWidth, this.rfb.framebufferHeight, true);
    }
  }

  private boolean fixTileBackground(Object[][] tiles, int y, byte background, int x) {
    int yy = y;
    int xx = x;

    while (xx < tiles.length && yy < tiles[0].length) {

      byte[] tile1 = (byte[]) tiles[xx][yy];

      if ((tile1[0] & RfbProto.HextileRaw) != 1) {

        if ((tile1[0] & RfbProto.HextileBackgroundSpecified) != 0) {
          return false;
        }

        byte[] temp = new byte[tile1.length + 1];
        temp[0] = (byte) (tile1[0] | RfbProto.HextileBackgroundSpecified);
        temp[1] = background;
        System.arraycopy(tile1, 1, temp, 2, tile1.length - 1);
        tiles[xx][yy] = temp;
        return true;

      }

      if (xx == tiles.length - 1) {
        xx = 0;
        yy++;
      }
      xx++;
    }
    return false;
  }

  private boolean fixTileForeground(Object[][] tiles, int y, byte foreground, int x) {
    int yy = y;
    int xx = x;

    while (xx < tiles.length && yy < tiles[0].length) {

      byte[] tile1 = (byte[]) tiles[xx][yy];

      if ((tile1[0] & RfbProto.HextileRaw) != 1) {

        if ((tile1[0] & RfbProto.HextileForegroundSpecified) != 0) {
          return false;
        }

        byte[] temp = new byte[tile1.length + 1];
        temp[0] = (byte) (tile1[0] | RfbProto.HextileForegroundSpecified);
        if ((tile1[0] & RfbProto.HextileBackgroundSpecified) != 0) {
          temp[1] = tile1[1];
          temp[2] = foreground;
          System.arraycopy(tile1, 2, temp, 3, tile1.length - 2);
        } else {
          temp[1] = foreground;
          System.arraycopy(tile1, 1, temp, 2, tile1.length - 1);
        }

        tiles[xx][yy] = temp;
        return true;

      }

      if (xx == tiles.length - 1) {
        xx = 0;
        yy++;
      }
      xx++;
    }

    return false;
  }

  //
  // Connect to the RFB server and authenticate the user.
  //

  void connectAndAuthenticate() throws Exception {

    boolean authenticationDone = false;

    while (!authenticationDone) {

      this.rfb = new RfbProto(this.host, this.port, this.options);

      this.rfb.readVersionMsg();

      System.out.println("RFB server supports protocol version "
          + this.rfb.serverMajor + "." + this.rfb.serverMinor);

      this.rfb.writeVersionMsg();

      switch (this.rfb.readAuthScheme()) {

        case RfbProto.NoAuth:
          System.out.println("No authentication needed");
          authenticationDone = true;
          break;

        case RfbProto.VncAuth:

          byte[] challenge = new byte[16];
          this.rfb.is.readFully(challenge);

          String pw = this.password;
          if (pw.length() > 8) {
            pw = pw.substring(0, 8); // truncate to 8 chars
          }

          byte[] key = new byte[8];

          System.arraycopy(pw.getBytes(), 0, key, 0, Math.min(key.length, pw.length()));

          for (int i = pw.length(); i < 8; i++) {
            key[i] = (byte) 0;
          }

          DesCipher des = new DesCipher(key);

          des.encrypt(challenge, 0, challenge, 0);
          des.encrypt(challenge, 8, challenge, 8);

          this.rfb.os.write(challenge);

          int authResult = this.rfb.is.readInt();

          switch (authResult) {
            case RfbProto.VncAuthOK:
              System.out.println("VNC authentication succeeded");
              authenticationDone = true;
              break;
            case RfbProto.VncAuthFailed:
              System.out.println("VNC authentication failed");
              throw new IOException("VNC authentication failed");
            case RfbProto.VncAuthTooMany:
              throw new IOException("VNC authentication failed - "
                  + "too many tries");
            default:
              throw new IOException("Unknown VNC authentication result "
                  + authResult);
          }
          break;

        case RfbProto.MsLogon:
          byte user[] = new byte[256];
          byte passwd[] = new byte[64];

          long gen = this.rfb.is.readLong();
          long mod = this.rfb.is.readLong();
          long resp = this.rfb.is.readLong();

          DH dh = new DH(gen, mod);
          long pub = dh.createInterKey();

          this.rfb.os.write(DH.longToBytes(pub));

          long ekey = dh.createEncryptionKey(resp);
          System.out.println("gen=" + gen + ", mod=" + mod
              + ", pub=" + pub + ", key=" + ekey);

          DesCipher2 des2 = new DesCipher2(DH.longToBytes(ekey));

          System.arraycopy(this.username.getBytes(), 0, user, 0, this.username.length());
          // and pad it with Null
          int i;
          if (this.username.length() < 256) {
            for (i = this.username.length(); i < 256; i++) {
              user[i] = 0;
            }
          }

          // copy the pw (password) parameter into the password Byte formated
          // variable
          System.arraycopy(this.password.getBytes(), 0, passwd, 0, this.password.length());
          // and pad it with Null
          if (this.password.length() < 32) {
            for (i = this.password.length(); i < 32; i++) {
              passwd[i] = 0;
            }
          }

          // user = domain + "\\" + user;

          des2.encryptText(user, user, DH.longToBytes(ekey));
          des2.encryptText(passwd, passwd, DH.longToBytes(ekey));

          this.rfb.os.write(user);
          this.rfb.os.write(passwd);

          authResult = this.rfb.is.readInt();

          switch (authResult) {
            case RfbProto.VncAuthOK:
              System.out.println("MS-Logon (DH) authentication succeeded");
              authenticationDone = true;
              break;
            case RfbProto.VncAuthFailed:
              System.out.println("MS-Logon (DH) authentication failed");
              break;
            case RfbProto.VncAuthTooMany:
              throw new Exception("MS-Logon (DH) authentication failed - too many tries");
            default:
              throw new Exception("Unknown MS-Logon (DH) authentication result " + authResult);
          }
          break;

      }
    }
  }

  //
  // Do the rest of the protocol initialisation.
  //

  void doProtocolInitialisation() throws IOException {
    System.out.println("sending client init");

    this.rfb.writeClientInit();

    this.rfb.readServerInit();

    System.out.println("Desktop name is " + this.rfb.desktopName);
    System.out.println("Desktop size is " + this.rfb.framebufferWidth + " x "
        + this.rfb.framebufferHeight);

    setEncodings();
  }

  //
  // setEncodings() - send the current encodings from the options frame
  // to the RFB server.
  //

  void setEncodings() {
    try {
      if ((this.rfb != null) && this.rfb.inNormalProtocol) {
        this.rfb.writeSetEncodings(this.options.getEncodings(), this.options.getNEncodings());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //
  // encryptBytes() - encrypt some bytes in memory using a password. Note that
  // the mapping from password to key must be the same as that used on the rfb
  // server side.
  //
  // Note also that IDEA encrypts data in 8-byte blocks, so here we will
  // ignore
  // any data beyond the last 8-byte boundary leaving it to the calling
  // function to pad the data appropriately.
  //

  void encryptBytes(byte[] bytes, String passwd) {
    byte[] key = new byte[8];

    System.arraycopy(passwd.getBytes(), 0, key, 0, Math.min(key.length, passwd.length()));

    for (int i = passwd.length(); i < 8; i++) {
      key[i] = (byte) 0;
    }

    DesCipher des = new DesCipher(key);

    des.encrypt(bytes, 0, bytes, 0);
    des.encrypt(bytes, 8, bytes, 8);
  }

  public synchronized void notifyNewClient() {

    /*
     * overrides previous requests in order to update all
     * new clients in one run, this implies that the request
     * will be fullfilled in a non deterministic time
     */
    this.newClientTime = new Date().getTime();
    this.newClient = true;
  }

  public RfbServerDescription getDesc() {
    return new RfbServerDescription(this.rfb.framebufferWidth,
        this.rfb.framebufferHeight, this.rfb.bitsPerPixel, this.rfb.depth,
        this.rfb.bigEndian, this.rfb.trueColour, this.rfb.redMax, this.rfb.greenMax,
        this.rfb.blueMax, this.rfb.redShift, this.rfb.greenShift, this.rfb.blueShift,
        this.rfb.desktopName);
  }
}
