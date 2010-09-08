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

public class StudentAnnounceMessage extends ControlChannelMessage {

  /**
   * 
   */
  private static final long serialVersionUID = -7133655469489204541L;
  private String name;
  private int vncport;
  private boolean inDemo;
  private boolean locked;
  private boolean fullscreen;

  public StudentAnnounceMessage(String name, int vncport, boolean fullscreen, boolean demo, boolean locked) {
    super(ControlChannelMessage.STUDENT_ANNOUNCE);
    this.fullscreen = fullscreen;
    this.inDemo = demo;
    this.locked = locked;
    this.name = name;
    this.vncport = vncport;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getVncport() {
    return this.vncport;
  }

  public void setVncport(int vncport) {
    this.vncport = vncport;
  }

  public boolean isFullscreen() {
    return this.fullscreen;
  }

  public void setFullscreen(boolean fullscreen) {
    this.fullscreen = fullscreen;
  }

  public boolean isInDemo() {
    return this.inDemo;
  }

  public void setInDemo(boolean inDemo) {
    this.inDemo = inDemo;
  }

  public boolean isLocked() {
    return this.locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

}
