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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

import kello.teacher.protocol.TeacherAnnounceMessage;
import kello.teacher.protocol.util.ControlChannelNotifier;
import kello.teacher.util.ExceptionDialogBox;

public class TeacherAnnouncementDaemon extends ControlChannelNotifier {

  private String host;
  private int port;

  public TeacherAnnouncementDaemon(String group, int groupport, int ttl,
       int port, int interval) throws UnknownHostException, IOException {

    super(group, groupport, ttl, interval);

    // FIXME: this is an hack
    /* discover one of the local IP addresses */
    this.host = null;
    try {
      Enumeration e = NetworkInterface.getNetworkInterfaces();

      while (e.hasMoreElements()) {
        NetworkInterface netface = (NetworkInterface) e.nextElement();

        Enumeration e2 = netface.getInetAddresses();

        while (e2.hasMoreElements()) {
          InetAddress ip = (InetAddress) e2.nextElement();
          if (!ip.isLoopbackAddress() && !(ip instanceof Inet6Address)) {
            this.host = ip.getHostAddress();
            break;
          }
        }

        if (this.host != null) {
          break;
        }
      }
    } catch (Exception ex) {
      ExceptionDialogBox.displayExceptionDialog(null, ex, "Error while setting up the teacher announcment daemon");
    }

    this.port = port;

  }

  @Override
  protected byte[] constructMessage() {
    try {

      ByteArrayOutputStream b = new ByteArrayOutputStream();
      ObjectOutputStream o = new ObjectOutputStream(b);

      TeacherAnnounceMessage msg = new TeacherAnnounceMessage(this.host, this.port);

      o.writeObject(msg);
      o.flush();
      b.flush();

      return b.toByteArray();

    } catch (IOException ex) {
      ExceptionDialogBox.displayExceptionDialog(null, ex, "Error while sending an announce message");
      return null;
    }

  }

}
