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

package kello.teacher.protocol;

public class TeacherAnnounceMessage extends ControlChannelMessage {

  /**
   * 
   */
  private static final long serialVersionUID = 6732310916185572559L;
  private String host;
  private int port;

  public TeacherAnnounceMessage(String host, int port2) {
    super(ControlChannelMessage.TEACHER_ANNOUNCE);
    this.host = host;
    this.port = port2;
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

}
