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
 * This class implements a Swing component that shows a remote frame buffer
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.io.IOException;
import java.net.SocketException;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import kello.teacher.rfb.DH;
import kello.teacher.rfb.DesCipher;
import kello.teacher.rfb.RfbOptions;
import kello.teacher.rfb.RfbProto;

public class JRfbViewer extends JPanel implements java.lang.Runnable {

  /**
   * 
   */
  private static final long serialVersionUID = 2517145248658261687L;

  /** should be updates directly be painted on the panel (incrementally) */
  private boolean paint;

  private ColorModel cm;

  private Color[] colors;

  private Graphics sg;

  private Image paintImage;

  private Graphics pig;

  private boolean needToResetClip;

  private String host;

  private String username;
  private String password;

  private int port;

  private RfbProto rfb;

  private Thread rfbThread;

  private RfbOptions options;

  private boolean scaled;

  private boolean send_events;

  /**
   * Creates a viewer with scaled image
   * 
   */
  public JRfbViewer() {
    this(true);
  }

  /**
   * Creates a viewer
   * 
   * @param scaled
   *          true if the viewer will be scaled, false otherwise
   */
  public JRfbViewer(boolean scaled) {
    this.options = new RfbOptions();
    this.scaled = scaled;

    if (!scaled) {
      setupEventHandlers();
    }

  }

  /**
   * Creates the event handlers that capture mouse motion and keyboard input
   * that will e sent to the remote RFB server
   * 
   */
  private void setupEventHandlers() {
    MouseInputAdapter mouseHandler = new MouseInputAdapter() {

      private void handleEvent(MouseEvent e) {
        try {
          if (JRfbViewer.this.send_events) {
            JRfbViewer.this.rfb.writePointerEvent(e);
          }
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }

      @Override
      public void mouseClicked(java.awt.event.MouseEvent e) {
        handleEvent(e);
        JRfbViewer.this.requestFocus();
      };

      @Override
      public void mousePressed(java.awt.event.MouseEvent e) {
        handleEvent(e);
      };

      @Override
      public void mouseMoved(java.awt.event.MouseEvent e) {
        handleEvent(e);
      };

      @Override
      public void mouseReleased(java.awt.event.MouseEvent e) {
        handleEvent(e);
      };

    };

    addMouseListener(mouseHandler);

    MouseMotionAdapter mouseMotionHandler = new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        try {
          if (JRfbViewer.this.send_events && JRfbViewer.this.rfb != null) {
            JRfbViewer.this.rfb.writePointerEvent(e);
          }
        } catch (SocketException ex) {
          // Ignore errors when disconnected
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      };
    };

    addMouseMotionListener(mouseMotionHandler);

    KeyAdapter keyHandler = new KeyAdapter() {

      private void handleEvent(java.awt.event.KeyEvent e) {
        try {
          if (JRfbViewer.this.send_events && JRfbViewer.this.rfb != null) {
            JRfbViewer.this.rfb.writeKeyEvent(e);
          }
        } catch (SocketException ex) {
          // Ignore errors when disconnected
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }

      @Override
      public void keyPressed(java.awt.event.KeyEvent e) {
        handleEvent(e);
      };

      @Override
      public void keyReleased(java.awt.event.KeyEvent e) {
        handleEvent(e);
      };
    };

    addKeyListener(keyHandler);

    setFocusable(true);

  }

  /**
   * Connect to the remote host and starts showing the remote framebuffer
   * 
   */
  public synchronized void start() {
    if (this.rfbThread == null && this.port != 0 && !this.host.equals("")) {
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
      this.paused = false;
      notifyAll();
      if (this.rfb != null) {
        this.rfb.close();
      }
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

    this.rfb.writeSetPixelFormat(8, 8, false, true, 7, 7, 3, 0, 3, 6);
    // this.rfb.writeSetPixelFormat(32, 24, false, true, 255, 255, 255, 16, 8,
    // 0, false);

    this.colors = new Color[256];

    for (int i = 0; i < 256; i++) {
      this.colors[i] = new Color(this.cm.getRGB(i));
    }

    // pixels = new byte[rfb.framebufferWidth * rfb.framebufferHeight];

    // amis = new AnimatedMemoryImageSource(rfb.framebufferWidth,
    // rfb.framebufferHeight, cm, pixels);
    // rawPixelsImage = createImage(amis);

    this.paintImage = createImage(this.rfb.framebufferWidth, this.rfb.framebufferHeight);

    this.pig = this.paintImage.getGraphics();

    this.setPreferredSize(new Dimension(this.rfb.framebufferWidth, this.rfb.framebufferHeight));

    if (!this.scaled) {
      this.setMinimumSize(new Dimension(this.rfb.framebufferWidth, this.rfb.framebufferHeight));
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
      connectAndAuthenticate();

      doProtocolInitialisation();

      setupPanel();

      processNormalProtocol();

    } catch (Exception e) {
      // the connection with remote was interruped
      stop();
    }

  }

  /**
   * Connect to the RFB server and authenticate the user.
   */
  private void connectAndAuthenticate() throws Exception {

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
          // vncEncryptBytes in the UNIX libvncauth truncates password
          // after the first zero byte. We do to.
          int firstZero = pw.indexOf(0);
          if (firstZero != -1) {
            pw = pw.substring(0, firstZero);
          }

          byte[] key = { 0, 0, 0, 0, 0, 0, 0, 0 };
          System.arraycopy(pw.getBytes(), 0, key, 0, pw.length());

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

          DesCipher des2 = new DesCipher(DH.longToBytes(ekey));

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

  /**
   * Do the rest of the protocol initialisation.
   */
  private void doProtocolInitialisation() throws IOException {

    this.rfb.writeClientInit();

    this.rfb.readServerInit();

    setEncodings();
  }

  /**
   * send the current encodings from the options frame to the RFB server.
   */
  private void setEncodings() {
    try {
      if ((this.rfb != null) && this.rfb.inNormalProtocol) {
        this.rfb.writeSetEncodings(this.options.getEncodings(), this.options.getNEncodings());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getHost() {
    return this.host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return this.port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getUsername() {
    return this.username;
  }

  public boolean isStopped() {
    return this.rfbThread == null;
  }

  /**
   * The component has to send keyboard and mouse events to the remote frame
   * buffer,
   * in case the this is a scaled frame buffer this setting has no effect
   * 
   * @param send
   */
  public void setSendEvents(boolean send) {
    this.send_events = send;
  }

  /**
   * Is the component sending keyboard and mouse events to the remote frame
   * buffer
   * 
   * @return
   */
  public boolean isSendingEvents() {
    return this.send_events && !this.scaled;
  }

  @Override
  public void paintComponent(Graphics g) {
    if (this.paused) {
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());

    } else if (this.paintImage != null) {

      this.lastRepaint = new Date().getTime();

      if (this.scaled) {
        g.drawImage(this.paintImage, 0, 0, getWidth(), getHeight(), 0, 0, this.rfb.framebufferWidth,
            this.rfb.framebufferHeight, this);
      } else {
        g.drawImage(this.paintImage, 0, 0, this);
      }

    } else {

      g.setColor(Color.BLACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());

      g.setColor(Color.WHITE);

      String str = "Not connected to VNC server on " + getHost();
      Rectangle2D rect = g.getFont().getStringBounds(str, ((Graphics2D) g).getFontRenderContext());
      g.drawString(str, this.getWidth() / 2 - (int) (rect.getWidth() / 2), this.getHeight() - 20);

      int offset = (20 + (int) rect.getHeight()) / 2;
      g.setColor(Color.RED);
      int d = Math.min(this.getWidth(), this.getHeight()) / 4 / 3 * 3;
      g.fillOval(this.getWidth() / 2 - d, this.getHeight() / 2 - d - offset, d * 2, d * 2);
      g.setColor(Color.BLACK);
      g.fillOval(this.getWidth() / 2 - d * 2 / 3, this.getHeight() / 2 - d * 2 / 3 - offset, d / 3 * 4, d / 3 * 4);

    }

  }

  /**
   * Executed by the rfbThread to deal with the RFB socket.
   * 
   * @throws InterruptedException
   */
  public void processNormalProtocol() throws Exception {

    long lastRequest = 0;

    this.rfb.writeFramebufferUpdateRequest(0, 0, this.rfb.framebufferWidth,
        this.rfb.framebufferHeight, false);

    this.needToResetClip = false;

    //
    // main dispatch loop
    //

    while (true) {

      if (this.paint) {
        this.sg = getGraphics();
      } else {
        this.sg = null;
      }

      int msgType = this.rfb.readServerMessageType();

      switch (msgType) {
        case RfbProto.FramebufferUpdate:
          this.rfb.readFramebufferUpdate();

          for (int i = 0; i < this.rfb.updateNRects; i++) {
            this.rfb.readFramebufferUpdateRectHdr();

            if (this.needToResetClip
                && (this.rfb.updateRectEncoding != RfbProto.EncodingRaw)) {
              try {
                if (this.paint) {
                  this.sg.setClip(0, 0, this.rfb.framebufferWidth,
                      this.rfb.framebufferHeight);
                }
                this.pig.setClip(0, 0, this.rfb.framebufferWidth,
                    this.rfb.framebufferHeight);
              } catch (NoSuchMethodError e) {
              }
              this.needToResetClip = false;
            }

            switch (this.rfb.updateRectEncoding) {

              case RfbProto.EncodingRaw:

                drawRawRect(this.rfb.updateRectX, this.rfb.updateRectY,
                    this.rfb.updateRectW, this.rfb.updateRectH);

                break;

              case RfbProto.EncodingCopyRect:
                this.rfb.readCopyRect();
                this.pig.copyArea(this.rfb.copyRectSrcX, this.rfb.copyRectSrcY,
                    this.rfb.updateRectW, this.rfb.updateRectH,
                    this.rfb.updateRectX - this.rfb.copyRectSrcX,
                    this.rfb.updateRectY - this.rfb.copyRectSrcY);
                if (this.rfb.options.isCopyRectFast()) {
                  if (this.paint) {
                    this.sg.copyArea(this.rfb.copyRectSrcX,
                        this.rfb.copyRectSrcY,
                        this.rfb.updateRectW,
                        this.rfb.updateRectH, this.rfb.updateRectX
                            - this.rfb.copyRectSrcX,
                        this.rfb.updateRectY - this.rfb.copyRectSrcY);
                  }
                } else {
                  if (this.paint) {
                    this.sg.drawImage(this.paintImage,
                        this.rfb.framebufferWidth,
                        this.rfb.framebufferHeight,
                        0, 0, this);
                  }
                }
                break;

              case RfbProto.EncodingRRE: {
                int nSubrects = this.rfb.is.readInt();
                int bg = this.rfb.is.read();
                int pixel, x, y, w, h;
                if (this.paint) {
                  this.sg.translate(this.rfb.updateRectX,
                      this.rfb.updateRectY);
                }
                if (this.paint) {
                  this.sg.setColor(this.colors[bg]);
                }
                if (this.paint) {
                  this.sg.fillRect(0, 0, this.rfb.updateRectW,
                      this.rfb.updateRectH);
                }
                this.pig.translate(this.rfb.updateRectX, this.rfb.updateRectY);
                this.pig.setColor(this.colors[bg]);
                this.pig.fillRect(0, 0, this.rfb.updateRectW, this.rfb.updateRectH);
                for (int j = 0; j < nSubrects; j++) {
                  pixel = this.rfb.is.read();
                  x = this.rfb.is.readUnsignedShort();
                  y = this.rfb.is.readUnsignedShort();
                  w = this.rfb.is.readUnsignedShort();
                  h = this.rfb.is.readUnsignedShort();
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
                  this.sg.translate(-this.rfb.updateRectX,
                      -this.rfb.updateRectY);
                }
                this.pig.translate(-this.rfb.updateRectX, -this.rfb.updateRectY);
                break;
              }

              case RfbProto.EncodingCoRRE: {
                int nSubrects = this.rfb.is.readInt();
                int bg = this.rfb.is.read();
                int pixel, x, y, w, h;

                if (this.paint) {
                  this.sg.translate(this.rfb.updateRectX,
                      this.rfb.updateRectY);
                }
                if (this.paint) {
                  this.sg.setColor(this.colors[bg]);
                }
                if (this.paint) {
                  this.sg.fillRect(0, 0, this.rfb.updateRectW,
                      this.rfb.updateRectH);
                }
                this.pig.translate(this.rfb.updateRectX, this.rfb.updateRectY);
                this.pig.setColor(this.colors[bg]);
                this.pig.fillRect(0, 0, this.rfb.updateRectW, this.rfb.updateRectH);

                for (int j = 0; j < nSubrects; j++) {
                  pixel = this.rfb.is.read();
                  x = this.rfb.is.read();
                  y = this.rfb.is.read();
                  w = this.rfb.is.read();
                  h = this.rfb.is.read();

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
                  this.sg.translate(-this.rfb.updateRectX,
                      -this.rfb.updateRectY);
                }
                this.pig.translate(-this.rfb.updateRectX, -this.rfb.updateRectY);

                break;
              }

              case RfbProto.EncodingHextile: {
                int bg = 0, fg = 0, sx, sy, sw, sh;

                for (int ty = this.rfb.updateRectY; ty < this.rfb.updateRectY
                    + this.rfb.updateRectH; ty += 16) {
                  for (int tx = this.rfb.updateRectX; tx < this.rfb.updateRectX
                      + this.rfb.updateRectW; tx += 16) {

                    int tw = 16, th = 16;

                    if (this.rfb.updateRectX + this.rfb.updateRectW - tx < 16) {
                      tw = this.rfb.updateRectX + this.rfb.updateRectW - tx;
                    }
                    if (this.rfb.updateRectY + this.rfb.updateRectH - ty < 16) {
                      th = this.rfb.updateRectY + this.rfb.updateRectH - ty;
                    }

                    int subencoding = this.rfb.is.read();

                    if ((subencoding & RfbProto.HextileRaw) != 0) {
                      drawRawRect(tx, ty, tw, th);
                      continue;
                    }

                    if (this.needToResetClip) {
                      try {
                        if (this.paint) {
                          this.sg.setClip(0, 0,
                              this.rfb.framebufferWidth,
                              this.rfb.framebufferHeight);
                        }
                        this.pig.setClip(0, 0, this.rfb.framebufferWidth,
                            this.rfb.framebufferHeight);
                      } catch (NoSuchMethodError e) {
                      }
                      this.needToResetClip = false;
                    }

                    if ((subencoding & RfbProto.HextileBackgroundSpecified) != 0) {
                      bg = this.rfb.is.read();
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
                      fg = this.rfb.is.read();
                    }

                    if ((subencoding & RfbProto.HextileAnySubrects) == 0) {
                      continue;
                    }

                    int nSubrects = this.rfb.is.read();

                    if (this.paint) {
                      this.sg.translate(tx, ty);
                    }
                    this.pig.translate(tx, ty);

                    if ((subencoding & RfbProto.HextileSubrectsColoured) != 0) {

                      for (int j = 0; j < nSubrects; j++) {
                        fg = this.rfb.is.read();
                        int b1 = this.rfb.is.read();
                        int b2 = this.rfb.is.read();
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
                        int b1 = this.rfb.is.read();
                        int b2 = this.rfb.is.read();
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
                    + this.rfb.updateRectEncoding);
            }
          }

          if (this.scaled) {
            repaintWidget();
          }

          /* wait while in pause */
          synchronized (this) {
            while (this.paused) {
              wait();
            }
          }

          if (this.fullUpdate) {
            this.rfb.writeFramebufferUpdateRequest(0, 0, this.rfb.framebufferWidth,
                this.rfb.framebufferHeight, false);
            this.fullUpdate = false;
          } else {

            long now = new Date().getTime();

            /* make sure the maximum update request rate is 1 frame every 200 ms */
            if (now - lastRequest < 200) {
              Thread.sleep(200 - (now - lastRequest));
            }

            lastRequest = now;

            this.rfb.writeFramebufferUpdateRequest(0, 0, this.rfb.framebufferWidth,
                this.rfb.framebufferHeight, true);
          }

          break;

        case RfbProto.SetColourMapEntries:
          throw new IOException(
              "Can't handle SetColourMapEntries message");

        case RfbProto.Bell:
          System.out.print((char) 7);
          break;

        case RfbProto.ServerCutText:
          this.rfb.readServerCutText();

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
    if (this.rfb.options.isDrawEachPixelForRawRects()) {
      for (int j = y; j < (y + h); j++) {
        for (int k = x; k < (x + w); k++) {
          int pixel = this.rfb.is.read();
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
    //
    // for (int j = y; j < (y + h); j++) {
    // rfb.is.readFully(pixels, j * rfb.framebufferWidth + x, w);
    // }
    //
    // amis.newPixels(x, y, w, h);
    //
    // try {
    // if (paint)
    // sg.setClip(x, y, w, h);
    // pig.setClip(x, y, w, h);
    // needToResetClip = true;
    // } catch (NoSuchMethodError e) {
    // sg2 = sg.create();
    // sg.clipRect(x, y, w, h);
    // pig2 = pig.create();
    // pig.clipRect(x, y, w, h);
    // }
    //
    // if (paint)
    // sg.drawImage(rawPixelsImage,
    // rfb.framebufferWidth,
    // rfb.framebufferHeight,
    // 0, 0, this);
    // pig.drawImage(rawPixelsImage, 0, 0, this);
    //
    // if (sg2 != null) {
    // sg.dispose(); // reclaims resources more quickly
    // sg = sg2;
    // sg2 = null;
    // pig.dispose();
    // pig = pig2;
    // pig2 = null;
    // }
  }

  long lastRepaint = 0;

  private void repaintWidget() {

    long now = new Date().getTime();

    if (now - this.lastRepaint < 100) {
      return;
    }

    repaint();

  }

  private boolean paused;
  private boolean fullUpdate;

  public boolean getPaused() {
    return this.paused;
  }

  public synchronized void setPaused(boolean pause) {

    /* request a full update if it was in pause */
    if (!pause && this.paused) {
      this.fullUpdate = true;
    }

    this.paused = pause;
    notifyAll();
    repaint();
  }
}
