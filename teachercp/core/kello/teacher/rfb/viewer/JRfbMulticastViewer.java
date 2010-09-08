/*
 * Copyright (C) 2006 Lorenzo Keller <lorenzo.keller@gmail.com>
 * Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
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

package kello.teacher.rfb.viewer;

/**
 * This class implements a Swing component that shows a remote frame buffer multicasted
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import javax.swing.JPanel;

import kello.teacher.protocol.RfbServerDescription;
import kello.teacher.rfb.MulticastRfbOptions;
import kello.teacher.rfb.RfbOptions;
import kello.teacher.rfb.RfbProto;

public class JRfbMulticastViewer extends JPanel implements java.lang.Runnable {

  /**
   * 
   */
  private static final long serialVersionUID = -4617906378752870836L;

  /** should be updates directly be painted on the panel (incrementally) */
  private boolean paint;

  private ColorModel cm;

  private Color[] colors;

  private Graphics sg;

  private Image paintImage;

  private Graphics pig;

  private boolean needToResetClip;

  private Thread rfbThread;

  private RfbOptions options;

  private boolean scaled;

  private RfbServerDescription desc;

  private MulticastSocket socket;

  /**
   * Creates a viewer
   * 
   * @param scaled
   *          true if the viewer will be scaled, false otherwise
   */
  public JRfbMulticastViewer(boolean scaled, RfbServerDescription desc, MulticastRfbOptions mo) throws IOException, UnknownHostException {
    this.options = new RfbOptions();
    this.scaled = scaled;
    this.desc = desc;

    this.socket = new MulticastSocket(mo.getMulticastPort());
    this.socket.joinGroup(InetAddress.getByName(mo.getMulticastGroup()));
    this.socket.setReceiveBufferSize(65000);
  }

  /**
   * Connect to the remote host and starts showing the remote framebuffer
   * 
   */
  public synchronized void start() {
    if (this.rfbThread == null) {
      this.rfbThread = new Thread(this);
      this.rfbThread.start();
    }
  }

  /**
   * Closes the connection to the remote framebuffer
   * 
   */
  public synchronized void stop() {
    if (this.rfbThread != null) {
      this.socket.close();
      this.rfbThread = null;
    }
  }

  /**
   * Does the intialisation of the structures needed to display the remote
   * framebuffer
   * 
   * @throws IOException
   *           in case it is impossible to send the pixel format to the server
   */
  private void setupPanel() throws IOException {

    this.framebufferWidth = this.desc.getFramebufferWidth();
    this.framebufferHeight = this.desc.getFramebufferHeight();

    /*
     * always disable incremental painting if the viewer is scaled ,
     * (this prevents aliased images)
     */
    if (this.scaled) {
      this.paint = false;
    } else {
      this.paint = true;
    }

    this.cm = new DirectColorModel(8, 7, (7 << 3), (3 << 6));

    this.colors = new Color[256];

    for (int i = 0; i < 256; i++) {
      this.colors[i] = new Color(this.cm.getRGB(i));
    }

    this.paintImage = createImage(this.framebufferWidth, this.framebufferHeight);

    this.pig = this.paintImage.getGraphics();

    this.setPreferredSize(new Dimension(this.framebufferWidth, this.framebufferHeight));

    if (!this.scaled) {
      this.setMinimumSize(new Dimension(this.framebufferWidth, this.framebufferHeight));
    }

    revalidate();
    repaint();

  }

  /**
   * The main method responsible to update the local copy of the remote
   * framebuffer.
   * It first connect and authenticate to the remote then it process incoming
   * rects.
   */
  @Override
  public void run() {

    try {
      setupPanel();

      processNormalProtocol();

    } catch (SocketException e) {
      System.out.println("Disconnected");
    } catch (Exception e) {
      e.printStackTrace();
      // the connection with remote was interruped
      stop();
    }

  }

  void getDatagram() throws IOException {
    int size = this.socket.getReceiveBufferSize();
    this.msg = new byte[size];
    this.cp = 0;
    DatagramPacket packet = new DatagramPacket(this.msg, this.msg.length);
    this.socket.receive(packet);
  }

  public boolean isStopped() {
    return this.rfbThread == null;
  }

  @Override
  public void paintComponent(Graphics g) {
    if (this.paintImage != null) {
      this.lastRepaint = new Date().getTime();
      if (this.scaled) {
        g.drawImage(this.paintImage, 0, 0, getWidth(), getHeight(), 0, 0, this.framebufferWidth,
            this.framebufferHeight, this);
      } else {
        g.drawImage(this.paintImage, 0, 0, this);
      }
    } else {
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
    }
  }

  private int framebufferWidth;
  private int framebufferHeight;
  private byte msg[];
  int cp;

  /**
   * Executed by the rfbThread to deal with the RFB socket.
   */
  public void processNormalProtocol() throws IOException {

    this.needToResetClip = false;

    //
    // main dispatch loop
    //

    while (true) {

      getDatagram();

      if (this.paint) {
        this.sg = getGraphics();
      } else {
        this.sg = null;
      }

      int msgType = (0xFF & this.msg[this.cp++]);
      /* skip padding byte */
      this.cp++;
      switch (msgType) {
        case RfbProto.FramebufferUpdate:

          int updateNRects = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);

          for (int i = 0; i < updateNRects; i++) {

            int updateRectX = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
            int updateRectY = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
            int updateRectW = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
            int updateRectH = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);

            int updateRectEncoding = ((0xFF & this.msg[this.cp++]) << 24) + ((0xFF & this.msg[this.cp++]) << 16) + ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);

            if (this.needToResetClip
                && (updateRectEncoding != RfbProto.EncodingRaw)) {
              try {
                if (this.paint) {
                  this.sg.setClip(0, 0, this.framebufferWidth,
                      this.framebufferHeight);
                }
                this.pig.setClip(0, 0, this.framebufferWidth,
                    this.framebufferHeight);
              } catch (NoSuchMethodError e) {
              }
              this.needToResetClip = false;
            }

            switch (updateRectEncoding) {

              case RfbProto.EncodingRaw:

                drawRawRect(updateRectX, updateRectY,
                    updateRectW, updateRectH);

                break;

              case RfbProto.EncodingCopyRect:

                int copyRectSrcX = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
                int copyRectSrcY = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);

                this.pig.copyArea(copyRectSrcX, copyRectSrcY,
                    updateRectW, updateRectH,
                    updateRectX - copyRectSrcX,
                    updateRectY - copyRectSrcY);
                if (this.options.isCopyRectFast()) {
                  if (this.paint) {
                    this.sg.copyArea(copyRectSrcX,
                        copyRectSrcY,
                        updateRectW,
                        updateRectH, updateRectX
                            - copyRectSrcX,
                        updateRectY - copyRectSrcY);
                  }
                } else {
                  if (this.paint) {
                    this.sg.drawImage(this.paintImage,
                        this.framebufferWidth,
                        this.framebufferHeight,
                        0, 0, this);
                  }
                }
                break;

              case RfbProto.EncodingRRE: {

                int nSubrects = ((0xFF & this.msg[this.cp++]) << 24) + ((0xFF & this.msg[this.cp++]) << 16) + ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
                int bg = (0xFF & this.msg[this.cp++]);
                int pixel, x, y, w, h;
                if (this.paint) {
                  this.sg.translate(updateRectX,
                      updateRectY);
                }
                if (this.paint) {
                  this.sg.setColor(this.colors[bg]);
                }
                if (this.paint) {
                  this.sg.fillRect(0, 0, updateRectW,
                      updateRectH);
                }
                this.pig.translate(updateRectX, updateRectY);
                this.pig.setColor(this.colors[bg]);
                this.pig.fillRect(0, 0, updateRectW, updateRectH);
                for (int j = 0; j < nSubrects; j++) {
                  pixel = (0xFF & this.msg[this.cp++]);
                  x = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
                  y = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
                  w = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
                  h = ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
                  if (this.paint) {
                    this.sg.setColor(this.colors[pixel]);
                  }
                  if (this.paint) {
                    this.sg.fillRect(x, y, w, h);
                  }
                  this.pig.setColor(this.colors[pixel]);
                  this.pig.fillRect(x, y, w, h);
                }
                if (this.paint) {
                  this.sg.translate(-updateRectX,
                      -updateRectY);
                }
                this.pig.translate(-updateRectX, -updateRectY);

                break;
              }

              case RfbProto.EncodingCoRRE: {

                int nSubrects = ((0xFF & this.msg[this.cp++]) << 24) + ((0xFF & this.msg[this.cp++]) << 16) + ((0xFF & this.msg[this.cp++]) << 8) + (0xFF & this.msg[this.cp++]);
                int bg = (0xFF & this.msg[this.cp++]);
                int pixel, x, y, w, h;

                if (this.paint) {
                  this.sg.translate(updateRectX,
                      updateRectY);
                }
                if (this.paint) {
                  this.sg.setColor(this.colors[bg]);
                }
                if (this.paint) {
                  this.sg.fillRect(0, 0, updateRectW,
                      updateRectH);
                }
                this.pig.translate(updateRectX, updateRectY);
                this.pig.setColor(this.colors[bg]);
                this.pig.fillRect(0, 0, updateRectW, updateRectH);

                for (int j = 0; j < nSubrects; j++) {
                  pixel = (0xFF & this.msg[this.cp++]);
                  x = (0xFF & this.msg[this.cp++]);
                  y = (0xFF & this.msg[this.cp++]);
                  w = (0xFF & this.msg[this.cp++]);
                  h = (0xFF & this.msg[this.cp++]);

                  if (this.paint) {
                    this.sg.setColor(this.colors[pixel]);
                  }
                  if (this.paint) {
                    this.sg.fillRect(x, y, w, h);
                  }
                  this.pig.setColor(this.colors[pixel]);
                  this.pig.fillRect(x, y, w, h);
                }
                if (this.paint) {
                  this.sg.translate(-updateRectX,
                      -updateRectY);
                }
                this.pig.translate(-updateRectX, -updateRectY);

                break;
              }

              case RfbProto.EncodingHextile: {

                int bg = 0, fg = 0, sx, sy, sw, sh;

                for (int ty = updateRectY; ty < updateRectY
                    + updateRectH; ty += 16) {
                  for (int tx = updateRectX; tx < updateRectX
                      + updateRectW; tx += 16) {

                    int tw = 16, th = 16;

                    if (updateRectX + updateRectW - tx < 16) {
                      tw = updateRectX + updateRectW - tx;
                    }
                    if (updateRectY + updateRectH - ty < 16) {
                      th = updateRectY + updateRectH - ty;
                    }

                    int subencoding = (0xFF & this.msg[this.cp++]);

                    if ((subencoding & RfbProto.HextileRaw) != 0) {

                      drawRawRect(tx, ty, tw, th);
                      continue;
                    }

                    if (this.needToResetClip) {

                      try {
                        if (this.paint) {
                          this.sg.setClip(0, 0,
                              this.framebufferWidth,
                              this.framebufferHeight);
                        }
                        this.pig.setClip(0, 0, this.framebufferWidth,
                            this.framebufferHeight);
                      } catch (NoSuchMethodError e) {
                      }
                      this.needToResetClip = false;
                    }

                    if ((subencoding & RfbProto.HextileBackgroundSpecified) != 0) {
                      bg = (0xFF & this.msg[this.cp++]);
                    }

                    if (this.paint) {
                      this.sg.setColor(this.colors[bg]);
                    }
                    if (this.paint) {
                      this.sg.fillRect(tx, ty, tw, th);
                    }
                    this.pig.setColor(this.colors[bg]);
                    this.pig.fillRect(tx, ty, tw, th);

                    if ((subencoding & RfbProto.HextileForegroundSpecified) != 0) {
                      fg = (0xFF & this.msg[this.cp++]);
                    }

                    if ((subencoding & RfbProto.HextileAnySubrects) == 0) {
                      continue;
                    }

                    int nSubrects = (0xFF & this.msg[this.cp++]);

                    if (this.paint) {
                      this.sg.translate(tx, ty);
                    }
                    this.pig.translate(tx, ty);

                    if ((subencoding & RfbProto.HextileSubrectsColoured) != 0) {

                      for (int j = 0; j < nSubrects; j++) {

                        fg = (0xFF & this.msg[this.cp++]);
                        int b1 = (0xFF & this.msg[this.cp++]);
                        int b2 = (0xFF & this.msg[this.cp++]);
                        sx = b1 >> 4;
                        sy = b1 & 0xf;
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;

                        if (this.paint) {
                          this.sg.setColor(this.colors[fg]);
                        }
                        if (this.paint) {
                          this.sg.fillRect(sx, sy, sw,
                              sh);
                        }
                        this.pig.setColor(this.colors[fg]);
                        this.pig.fillRect(sx, sy, sw, sh);
                      }

                    } else {

                      if (this.paint) {
                        this.sg.setColor(this.colors[fg]);
                      }
                      this.pig.setColor(this.colors[fg]);

                      for (int j = 0; j < nSubrects; j++) {

                        int b1 = (0xFF & this.msg[this.cp++]);
                        int b2 = (0xFF & this.msg[this.cp++]);
                        sx = b1 >> 4;
                        sy = b1 & 0xf;
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;

                        if (this.paint) {
                          this.sg.fillRect(sx, sy, sw,
                              sh);
                        }
                        this.pig.fillRect(sx, sy, sw, sh);
                      }
                    }

                    if (this.paint) {
                      this.sg.translate(-tx, -ty);
                    }
                    this.pig.translate(-tx, -ty);
                  }
                }
                break;
              }

              default:
                throw new IOException("Unknown RFB rectangle encoding "
                    + updateRectEncoding);
            }
          }

          if (this.scaled) {
            repaintWidget();
          }

          break;

        case RfbProto.SetColourMapEntries:
          throw new IOException(
              "Can't handle SetColourMapEntries message");

        case RfbProto.Bell:
          System.out.print((char) 7);
          break;

        case RfbProto.ServerCutText:
          break;

        default:
          throw new IOException("Unknown RFB message type " + msgType);
      }
    }

  }

  /**
   * Draw a raw rectangle.
   */
  void drawRawRect(int x, int y, int w, int h) throws IOException {
    if (this.options.isDrawEachPixelForRawRects()) {

      for (int j = y; j < (y + h); j++) {
        for (int k = x; k < (x + w); k++) {
          int pixel = (0xFF & this.msg[this.cp++]);
          if (this.paint) {
            this.sg.setColor(this.colors[pixel]);
          }
          if (this.paint) {
            this.sg.fillRect(k, j, 1, 1);
          }
          this.pig.setColor(this.colors[pixel]);
          this.pig.fillRect(k, j, 1, 1);
        }
      }
      return;
    }

  }

  long lastRepaint = 0;

  private void repaintWidget() {

    long now = new Date().getTime();

    if (now - this.lastRepaint < 100) {
      return;
    }

    this.lastRepaint = now;

    repaint();

  }

}
