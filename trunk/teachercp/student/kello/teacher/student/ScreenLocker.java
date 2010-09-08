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

import javax.swing.JWindow;

public class ScreenLocker extends JWindow {

  private static final long serialVersionUID = 5885227624378242284L;
  private boolean locked;

  public ScreenLocker() {
    super();

    this.locked = false;
  }

  public synchronized void setLocked(boolean lock) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();

    System.out.println("========= " + (!lock ? "un" : "") + "locked ======");

    if (lock) {
      setAlwaysOnTop(true);
      this.setVisible(true);

      gd.setFullScreenWindow(this);
      this.locked = true;
    } else {
      gd.setFullScreenWindow(null);
      this.setVisible(false);
      this.locked = false;
      this.dispose();
    }
  }

  public boolean isLocked() {
    return this.locked;
  }
}
