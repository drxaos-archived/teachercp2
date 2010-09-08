/*
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

package kello.teacher.rfb.viewer;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class JRfbFrame extends JFrame {

  /**
   * 
   */
  private static final long serialVersionUID = -4117572899104159937L;
  private JPanel jContentPane = null;
  private JScrollPane jScrollPane = null;
  private JRfbViewer jRfbViewer = null;
  private boolean scaled;
  private boolean fullscreen;

  /**
   * This method initializes jScrollPane
   * 
   * @return javax.swing.JScrollPane
   */
  private JScrollPane getJScrollPane() {
    if (this.jScrollPane == null) {
      this.jScrollPane = new JScrollPane();
      this.jScrollPane.setViewportView(getJRfbViewer());
    }
    return this.jScrollPane;
  }

  /**
   * This method initializes jRfbViewer
   * 
   * @return kello.teacher.rfb.viewer.JRfbViewer
   */
  private JRfbViewer getJRfbViewer() {
    if (this.jRfbViewer == null) {
      this.jRfbViewer = new JRfbViewer(this.scaled);

    }
    return this.jRfbViewer;
  }

  /**
   * 
   * Use this to test the frame
   * 
   * @param args
   */
  public static void main(String[] args) {
    JRfbFrame frame = new JRfbFrame(false, true);
    frame.setVisible(true);
    frame.setHost("127.0.0.1");
    frame.setPort(5901);
    frame.setPassword("guest");
    frame.setPassword("lorenzo_40");
    frame.setSendEvents(true);
    frame.start();
  }

  /**
   * This is the default constructor
   * 
   * @param scaled
   *          the framebuffer should be scaled to the frame size
   * @param fullscreen
   *          the frame should be fullscreen
   */
  public JRfbFrame(boolean scaled, boolean fullscreen) {
    super();
    this.scaled = scaled;
    this.fullscreen = fullscreen;
    initialize();
  }

  /**
   * This method initializes this
   * 
   * @return void
   */
  private void initialize() {
    this.setSize(300, 200);
    this.setContentPane(getJContentPane());
    this.setTitle("Remote Screen");
    this.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        if (JRfbFrame.this.jRfbViewer != null) {
          JRfbFrame.this.jRfbViewer.stop();
        }
        dispose();
      }
    });

    if (this.fullscreen) {
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

      GraphicsDevice gd = ge.getDefaultScreenDevice();
      this.setUndecorated(true);
      gd.setFullScreenWindow(this);
    }
  }

  /**
   * This method initializes jContentPane
   * 
   * @return javax.swing.JPanel
   */
  private JPanel getJContentPane() {
    if (this.jContentPane == null) {
      this.jContentPane = new JPanel();
      this.jContentPane.setLayout(new BorderLayout());
      if (!this.scaled) {
        this.jContentPane.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
      } else {
        this.jContentPane.add(getJRfbViewer(), java.awt.BorderLayout.CENTER);
      }
    }
    return this.jContentPane;
  }

  public String getHost() {
    return this.jRfbViewer.getHost();
  }

  public int getPort() {
    return this.jRfbViewer.getPort();
  }

  public boolean isSendingEvents() {
    return this.jRfbViewer.isSendingEvents();
  }

  public void setHost(String host) {
    setTitle("Remote Screen @ " + host);
    this.jRfbViewer.setHost(host);
  }

  public void setUsername(String username) {
    this.jRfbViewer.setUsername(username);
  }

  public void setPassword(String password) {
    this.jRfbViewer.setPassword(password);
  }

  public void setPort(int port) {
    this.jRfbViewer.setPort(port);
  }

  public void setSendEvents(boolean send) {
    this.jRfbViewer.setSendEvents(send);
  }

  public void start() {
    this.jRfbViewer.start();
  }

  public void stop() {
    this.jRfbViewer.stop();
  }

}
