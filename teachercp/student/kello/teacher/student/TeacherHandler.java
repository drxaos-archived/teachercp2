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

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import kello.teacher.protocol.ControlChannelMessage;
import kello.teacher.protocol.DemoStartMessage;
import kello.teacher.protocol.FullscreenResfreshMessage;
import kello.teacher.protocol.LockScreenMessage;
import kello.teacher.protocol.StudentAnnounceMessage;
import kello.teacher.protocol.util.MessageStreamHandler;
import kello.teacher.util.Utility;

public class TeacherHandler extends MessageStreamHandler {

  private Student student;

  private Timer timer;

  public TeacherHandler(Socket s, Student student) throws IOException {
    super(s);
    this.student = student;

  }

  @Override
  public void start() {
    super.start();

    /* schedule an announcement of the client name every 10 sec */
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        if (!TeacherHandler.this.isStopped()) {
          sendAnnouncement();
        }
      }
    };

    this.timer = new Timer();
    this.timer.schedule(task, 0, Integer.parseInt(this.student.getProperty("announce-interval")));

  }

  private void sendAnnouncement() {
    String username = Utility.executeCommand(this.student.getProperty("who"));
    StudentAnnounceMessage msg = new StudentAnnounceMessage(username, this.student.getVncport(), this.student.isFullscreen(), this.student.isInDemo(), this.student.isLocked());
    try {
      sendMessage(msg);
    } catch (IOException ex) {
      System.out.println("Unable to notify presence to Teacher");
      stop();
    }
  }

  public void sendRefresh() {
    FullscreenResfreshMessage msg = new FullscreenResfreshMessage();
    try {
      sendMessage(msg);
    } catch (IOException ex) {
      System.out.println("Unable to request a screen refresh");
      stop();
    }

  }

  @Override
  public void stop() {
    this.timer.cancel();
    super.stop();
  }

  @Override
  public void cleanup() {
    super.cleanup();
    this.timer.cancel();
    this.student.stopDemo();
    this.student.lockScreen(false);
  }

  @Override
  protected void processMessage(ControlChannelMessage msg) {
    switch (msg.getMessageType()) {
      case ControlChannelMessage.DEMO_START:
        System.out.println("Demo started");
        DemoStartMessage dmsg = (DemoStartMessage) msg;
        this.student.startDemo(dmsg.getGroup(), dmsg.getGroupPort(), dmsg.getDesc(), dmsg.isFullscreen());
        sendAnnouncement();
        break;
      case ControlChannelMessage.DEMO_STOP:
        System.out.println("Demo stopped");
        this.student.stopDemo();
        sendAnnouncement();
        break;
      case ControlChannelMessage.LOCK_SCREEN:
        LockScreenMessage lmsg = (LockScreenMessage) msg;
        this.student.lockScreen(lmsg.isLocked());
        sendAnnouncement();
        break;
      default:
        System.out.println("Unknown message ID (" + msg.getMessageType() + ")");
    }
  }
}
