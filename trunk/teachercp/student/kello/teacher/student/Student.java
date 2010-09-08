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

package kello.teacher.student;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.WindowConstants;

import kello.teacher.protocol.RfbServerDescription;
import kello.teacher.rfb.MulticastRfbOptions;
import kello.teacher.rfb.viewer.JRfbMulticastViewer;

public class Student {

  private TeacherHandler teacher;

  private StudentControlChannelListener control;

  private Window frame;
  private JRfbMulticastViewer viewer;

  private ScreenLocker locker;

  private int vncport;

  private Properties properties;

  private boolean isFullscreen = false;

  public Student() {

    Properties defaultProperties = new Properties();

    defaultProperties.setProperty("vncport", "5900");
    defaultProperties.setProperty("teacher-multicast-group", "230.0.0.2");
    defaultProperties.setProperty("teacher-multicast-port", "3000");
    defaultProperties.setProperty("announce-interval", "10000");
    defaultProperties.setProperty("height", "600");
    defaultProperties.setProperty("width", "800");
    defaultProperties.setProperty("who", "w -h | grep ' :0 ' | cut -d ' ' -f 1 | head -n1");

    this.properties = new Properties(defaultProperties);

    try {
      this.properties.load(new FileInputStream("student.properties"));
    } catch (IOException ex) {
      System.out.println(ex);
    }

    this.vncport = Integer.parseInt(this.properties.getProperty("vncport"));
    try {
      this.control = new StudentControlChannelListener(this.properties.getProperty("teacher-multicast-group"), Integer.parseInt(this.properties.getProperty("teacher-multicast-port")), this);
    } catch (Exception e) {
      e.printStackTrace();
    }

    this.locker = new ScreenLocker();
  }

  public void start() {
    this.control.start();
  }

  public synchronized void notifyTeacher(String host, int port) {
    // System.out.println("Received teacher announcement ("+host + ":" + port
    // +")");

    if (this.teacher == null || this.teacher.isStopped()) {
      try {
        System.out.println("Connecting to teacher (" + host + ":" + port + ")");
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port));
        this.teacher = new TeacherHandler(s, this);
        this.teacher.start();
      } catch (IOException ex) {
        ex.printStackTrace();
      } catch (Exception ex) {
        // Malformed teacher announce messages
        ex.printStackTrace();
      }
    }
  }

  private void buildFullscreenFrame(MulticastRfbOptions opt, String group, int port, RfbServerDescription desc) throws UnknownHostException, IOException {
    this.frame = new JWindow();
    this.viewer = new JRfbMulticastViewer(false, desc, opt);
    this.frame.setAlwaysOnTop(true);
    this.frame.setSize(Integer.parseInt(this.properties.getProperty("width")), Integer.parseInt(this.properties.getProperty("height")));
    ((JWindow) this.frame).getContentPane().add(this.viewer);
    this.frame.setVisible(true);
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();
    gd.setFullScreenWindow(this.frame);
  }

  private void buildNonFullscreenFrame(MulticastRfbOptions opt, String group, int port, RfbServerDescription desc) throws UnknownHostException, IOException {
    JFrame frame = new JFrame();
    this.frame = frame;

    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    this.viewer = new JRfbMulticastViewer(false, desc, opt);

    frame.setAlwaysOnTop(true);
    frame.setSize(Integer.parseInt(this.properties.getProperty("width")), Integer.parseInt(this.properties.getProperty("height")));

    final JScrollPane scroll = new JScrollPane();

    scroll.setViewportView(this.viewer);

    AdjustmentListener listener = new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent event) {
        scroll.getViewport().repaint();
      }
    };

    scroll.getHorizontalScrollBar().addAdjustmentListener(listener);
    scroll.getVerticalScrollBar().addAdjustmentListener(listener);

    frame.getContentPane().add(scroll);

    frame.setVisible(true);
  }

  public synchronized void startDemo(String group, int port, RfbServerDescription desc, boolean fullscreen) {
    if (this.viewer != null) {
      stopDemo();
    }

    this.isFullscreen = fullscreen;

    MulticastRfbOptions opt = new MulticastRfbOptions();
    opt.setMulticastGroup(group);
    opt.setMulticastPort(port);

    try {

      if (fullscreen) {
        buildFullscreenFrame(opt, group, port, desc);
      } else {
        buildNonFullscreenFrame(opt, group, port, desc);
      }

      this.viewer.start();

      this.teacher.sendRefresh();

    } catch (UnknownHostException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public synchronized void stopDemo() {
    if (this.viewer != null) {
      this.viewer.stop();
      this.viewer = null;
      this.frame.setVisible(false);
      this.frame.dispose();
      this.frame = null;
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    Student s = new Student();
    s.start();
  }

  public TeacherHandler getTeacher() {
    return this.teacher;
  }

  public void lockScreen(boolean lock) {
    this.locker.setLocked(lock);

  }

  public int getVncport() {
    return this.vncport;
  }

  public void setVncport(int vncport) {
    this.vncport = vncport;
  }

  public String getProperty(String key) {
    return this.properties.getProperty(key);
  }

  public Object setProperty(String key, String value) {
    return this.properties.setProperty(key, value);
  }

  public boolean isLocked() {
    return this.locker.isLocked();
  }

  public boolean isFullscreen() {
    return this.isFullscreen;
  }

  public boolean isInDemo() {
    return (this.viewer != null ? true : false);
  }

}
