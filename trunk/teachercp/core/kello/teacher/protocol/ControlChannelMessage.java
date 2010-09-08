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

import java.io.Serializable;

public class ControlChannelMessage implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -5376742979677880313L;
  public static final int DEMO_MESSAGE = 0;
  public static final int NODEMO_MESSAGE = 1;
  public static final int REQUEST_FULLREFRESH = 2;
  public static final int STUDENT_ANNOUNCE = 3;
  public static final int TEACHER_ANNOUNCE = 4;
  public static final int DEMO_START = 5;
  public static final int DEMO_STOP = 6;
  public static final int LOCK_SCREEN = 7;

  private int messageType;

  public ControlChannelMessage(int messageType) {
    this.messageType = messageType;
  }

  public int getMessageType() {
    return this.messageType;
  }

}
