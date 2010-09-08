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

package kello.teacher.teacher;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import kello.teacher.protocol.RfbServerDescription;
import kello.teacher.rfb.proxy.VncProxy;
import kello.teacher.util.ExceptionDialogBox;

public class StudentHub implements Runnable {

  private Thread thread;

  private ServerSocket socket;

  private HashSet students;

  private JTeacherFrame teacher;

  private HashMap preferredPositions;

  private VncProxy proxy;

  private String multicastGroup;
  private int multicastPort;

  public StudentHub(int port, JTeacherFrame teacher) throws IOException {
    this.teacher = teacher;
    this.students = new HashSet();

    loadPositions();

    setupListening(port);

  }

  void requestFullRefreshToProxy() {

    getProxy().notifyNewClient();
  }

  public RfbServerDescription getDesc() {
    return getProxy().getDesc();
  }

  private VncProxy getProxy() {
    if (this.proxy != null) {
      return this.proxy;
    }

    this.multicastGroup = this.teacher.getProperty("screen-multicast-group");
    this.multicastPort = Integer.parseInt(this.teacher.getProperty("screen-multicast-port"));

    try {
      this.proxy = new VncProxy(this.teacher.getProperty("vncserver"),
          Integer.parseInt(this.teacher.getProperty("vncport")),
          this.teacher.getProperty("vncuser"), this.teacher.getProperty("vncpassword"), this.multicastGroup, this.multicastPort,
          Integer.parseInt(this.teacher.getProperty("screen-multicast-ttl")));
      this.proxy.start();

      this.teacher.setTitle("Teacher Control Panel (Demo mode)");

      return this.proxy;

    } catch (UnknownHostException ex) {
      ExceptionDialogBox.displayExceptionDialog(this.teacher, ex, "Fatal error while starting the proxy: the  multicast group name is invalid");
      System.exit(1);
    } catch (Exception ex) {
      ExceptionDialogBox.displayExceptionDialog(this.teacher, ex, "Fatal error while starting the proxy");
      System.exit(1);
    }

    return null;
  }

  private void loadPositions() {

    this.preferredPositions = new HashMap();

    if (!new File("positions.properties").exists()) {
      savePositions();
    }

    try {
      Properties storedPositions = new Properties();
      storedPositions.load(new FileInputStream("positions.properties"));
      Iterator i = storedPositions.entrySet().iterator();

      while (i.hasNext()) {
        Map.Entry entry = (Map.Entry) i.next();
        StringTokenizer st = new StringTokenizer((String) entry.getValue(), ",");
        Rectangle rect = new Rectangle(Integer.parseInt(st.nextToken()),
                            Integer.parseInt(st.nextToken()),
                            Integer.parseInt(st.nextToken()),
                            Integer.parseInt(st.nextToken()));
        this.preferredPositions.put(entry.getKey(), rect);
      }

    } catch (Exception ex) {
      ExceptionDialogBox.displayExceptionDialog(this.teacher, ex, "Error loading window position from positions.properties (the error is not fatal)");
    }
  }

  private void setupListening(int port) {

    try {
      this.socket = new ServerSocket(port);
    } catch (BindException ex) {
      ExceptionDialogBox.displayExceptionDialog(this.teacher, ex, "Failed to bind port: The application is probably already started");
      System.exit(1);
    } catch (IOException ex) {
      ExceptionDialogBox.displayExceptionDialog(this.teacher, ex, "Error while binding port");
      System.exit(1);
    }

    this.thread = new Thread(this);
  }

  public synchronized Rectangle getPreferredPosition(String host) {
    Rectangle preferred = (Rectangle) this.preferredPositions.get(host);
    if (preferred != null) {
      return preferred;
    } else {

      /* find an empty place to put the student widget */
      for (int y = 0; y < (int) (this.teacher.getHeight() / 200.0); y++) {
        for (int x = 0; x < (int) (this.teacher.getWidth() / 320.0); x++) {

          Rectangle rect = new Rectangle(x * 320 + 1, y * 200 + 1, 320 - 2, 200 - 2);
          boolean good = true;

          Iterator i = this.students.iterator();
          while (i.hasNext()) {
            StudentHandler s = (StudentHandler) i.next();
            if (s.getWidget().getBounds().intersects(rect)) {
              good = false;
              break;
            }
          }

          if (good) {
            return rect;
          }
        }

      }

      /* if no rect is good put random */
      return new Rectangle(
          ((int) (Math.random() * (this.teacher.getWidth() / 320.0))) * 320,
          ((int) (Math.random() * (this.teacher.getHeight() / 200.0))) * 200,
          320, 200);
    }
  }

  public String getMulticastGroup() {
    getProxy();
    return this.multicastGroup;
  }

  public int getMulticastPort() {
    getProxy();
    return this.multicastPort;
  }

  public synchronized void savePositions() {

    Iterator i = this.students.iterator();

    while (i.hasNext()) {
      StudentHandler s = (StudentHandler) i.next();
      this.preferredPositions.put(s.getHost(), s.getWidget().getBounds());
    }

    Properties storedPositions = new Properties();

    Iterator j = this.preferredPositions.entrySet().iterator();

    while (j.hasNext()) {
      Map.Entry entry = (Map.Entry) j.next();

      Rectangle rect = (Rectangle) entry.getValue();

      String pos = rect.x + "," + rect.y + "," + rect.width + "," + rect.height;

      storedPositions.setProperty((String) entry.getKey(), pos);
    }

    try {
      storedPositions.store(new FileOutputStream("positions.properties"), "Preferred student positions");
    } catch (IOException ex) {
      ExceptionDialogBox.displayExceptionDialog(this.teacher, ex, "Error while storing positions of the student windows");
    }

  }

  public void start() {
    this.thread.start();
  }

  @Override
  public void run() {
    while (true) {
      try {
        Socket client = this.socket.accept();
        StudentHandler student = new StudentHandler(client, this);
        student.start();
        synchronized (this) {
          this.students.add(student);
        }
      } catch (IOException ex) {
        ExceptionDialogBox.displayExceptionDialog(this.teacher, ex, "Error on the teacher server main loop");
      }

    }
  }

  synchronized void notifyStudentDisconnect(StudentHandler student) {
    this.students.remove(student);
  }

  synchronized void sendStartDemoToAll(boolean fullscreen) {

    Iterator i = this.students.iterator();

    while (i.hasNext()) {
      StudentHandler s = (StudentHandler) i.next();
      s.sendDemoStart(getProxy().getDesc(), this.multicastGroup, this.multicastPort, fullscreen);
    }

  }

  synchronized void sendStopDemoToAll() {

    Iterator i = this.students.iterator();

    while (i.hasNext()) {
      StudentHandler s = (StudentHandler) i.next();
      s.sendDemoStop();
    }

    this.teacher.setTitle("Teacher Control Panel");
    if (this.proxy != null) {
      this.proxy.stop();
    }
    this.proxy = null;

  }

  synchronized public void sendLockScreenToAll(boolean b) {

    Iterator i = this.students.iterator();

    while (i.hasNext()) {
      StudentHandler s = (StudentHandler) i.next();
      s.sendLockScreen(b);
    }

  }

  public JTeacherFrame getTeacher() {
    return this.teacher;
  }

  public synchronized HashSet getStudents() {
    return (HashSet) this.students.clone();
  }

  public synchronized void relayoutStudents() {

    Iterator i = this.students.iterator();

    while (i.hasNext()) {
      StudentHandler s = (StudentHandler) i.next();
      s.getWidget().setBounds(getPreferredPosition(s.getHost()));
    }

  }

  public synchronized void executeOnAll(Command command) {

    Iterator i = this.students.iterator();

    while (i.hasNext()) {
      StudentHandler s = (StudentHandler) i.next();
      s.executeCommand(command);
    }

  }

}
