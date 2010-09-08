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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.UnknownHostException;

import kello.teacher.protocol.ControlChannelMessage;
import kello.teacher.protocol.TeacherAnnounceMessage;
import kello.teacher.protocol.util.ControlChannelListener;

public class StudentControlChannelListener extends ControlChannelListener {

  private Student student;

  public StudentControlChannelListener(String group, int port, Student student)
      throws UnknownHostException, IOException {
    super(group, port);

    this.student = student;
  }

  @Override
  protected void processMessage(DataInputStream d) {

    try {

      ObjectInputStream oi = new ObjectInputStream(d);

      try {

        Object o = oi.readObject();

        if (!(o instanceof ControlChannelMessage)) {
          throw new ClassNotFoundException("Object is not a message");
        }

        ControlChannelMessage msg = (ControlChannelMessage) o;

        switch (msg.getMessageType()) {
          case ControlChannelMessage.TEACHER_ANNOUNCE:

            TeacherAnnounceMessage tmsg = (TeacherAnnounceMessage) msg;

            this.student.notifyTeacher(tmsg.getHost(), tmsg.getPort());

            break;
          default:
            System.out.println("Unknown message");
        }

      } catch (ClassNotFoundException ex) {
        System.err.println("Unknown message:" + ex.toString());
      }

    } catch (IOException ex) {
      ex.printStackTrace();
    }

  }

}
