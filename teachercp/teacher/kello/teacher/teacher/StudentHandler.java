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

import java.io.IOException;
import java.net.Socket;

import kello.teacher.protocol.ControlChannelMessage;
import kello.teacher.protocol.DemoStartMessage;
import kello.teacher.protocol.DemoStopMessage;
import kello.teacher.protocol.LockScreenMessage;
import kello.teacher.protocol.RfbServerDescription;
import kello.teacher.protocol.StudentAnnounceMessage;
import kello.teacher.protocol.util.MessageStreamHandler;
import kello.teacher.rfb.viewer.JRfbFrame;
import kello.teacher.util.Utility;

public class StudentHandler extends MessageStreamHandler {

  private StudentHub hub;

  private JStudentThumbnail widget;

  private StudentAnnounceMessage lastAnnouncement;

  private String host;

  public StudentHandler(Socket s, StudentHub hub) throws IOException {
    super(s);
    this.hub = hub;

    this.host = s.getInetAddress().getCanonicalHostName();

    this.widget = new JStudentThumbnail(this);
    this.hub.getTeacher().addNewStudent(this.widget);
    this.widget.showScreen();

  }

  @Override
  protected void processMessage(ControlChannelMessage o) {

    switch (o.getMessageType()) {
      case ControlChannelMessage.STUDENT_ANNOUNCE:
        this.lastAnnouncement = (StudentAnnounceMessage) o;
        this.widget.notifyNewAnnouncement();
        break;
      case ControlChannelMessage.REQUEST_FULLREFRESH:
        this.hub.requestFullRefreshToProxy();
        break;
      default:
        System.out.println("Unknown message ID (" + o.getMessageType() + ")");
    }
  }

  public void sendDemoStart(RfbServerDescription desc, String group, int port, boolean fullscreen) {

    DemoStartMessage msg = new DemoStartMessage(desc, group, port, fullscreen);
    try {
      sendMessage(msg);
    } catch (IOException ex) {
      System.err.println("SEND DEMO to " + getHost() + " failed");
      stop();
    }
  }

  public void sendDemoStop() {

    DemoStopMessage msg = new DemoStopMessage();
    try {
      sendMessage(msg);
    } catch (IOException ex) {
      System.err.println("SEND STOP DEMO to " + getHost() + " failed");
      stop();
    }
  }

  @Override
  public void cleanup() {
    this.hub.notifyStudentDisconnect(this);
    this.widget.dispose();
  }

  @Override
  public String toString() {
    return (this.lastAnnouncement != null ? this.lastAnnouncement.getName() : super.toString());
  }

  public void sendLockScreen(boolean b) {
    LockScreenMessage msg = new LockScreenMessage(b);
    try {
      sendMessage(msg);
    } catch (IOException ex) {
      System.err.println("SEND LOCK to " + getHost() + " failed");
      stop();
    }
  }

  public JStudentThumbnail getWidget() {
    return this.widget;
  }

  public StudentHub getHub() {
    return this.hub;
  }

  public StudentAnnounceMessage getLastAnnouncement() {
    return this.lastAnnouncement;
  }

  public String getHost() {
    return this.host;
  }

  public void startVNCviewer() {

    JRfbFrame rfbFrame = new JRfbFrame(false, false);
    rfbFrame.setHost(this.host);
    rfbFrame.setPort(this.lastAnnouncement.getVncport());
    rfbFrame.setUsername(getHub().getTeacher().getProperty("vnc-client-user"));
    rfbFrame.setPassword(getHub().getTeacher().getProperty("vnc-client-password"));
    rfbFrame.setSendEvents(true);
    rfbFrame.start();
    rfbFrame.setSize(800, 600);
    rfbFrame.setTitle(getHub().getTeacher().getFullname(this.lastAnnouncement.getName()));
    rfbFrame.setVisible(true);

  }

  public void executeCommand(Command selectedCommand) {
    String scommand = selectedCommand.getCommand();
    if (getLastAnnouncement().getName() != null) {
      scommand = scommand.replaceAll("%user", getLastAnnouncement().getName());
    } else {
      scommand = scommand.replaceAll("%user", "none");
    }
    scommand = scommand.replaceAll("%host", getHost());
    Utility.startCommand(scommand);

  }

}
