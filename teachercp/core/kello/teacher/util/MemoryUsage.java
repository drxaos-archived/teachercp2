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
package kello.teacher.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;

public class MemoryUsage extends JFrame {

  /**
   * 
   */
  private static final long serialVersionUID = -4557005673536220721L;
  private JButton button;
  private Timer timer;
  private static final Runtime s_runtime = Runtime.getRuntime();

  private MemoryUsage() {

    this.button = new JButton();

    ActionListener listener = new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        s_runtime.gc();
      }

    };

    this.button.addActionListener(listener);

    this.getContentPane().add(this.button);
    this.setAlwaysOnTop(true);
    this.setSize(200, 50);

    this.timer = new Timer(true);

    this.timer.schedule(new UpdateTask(), 1000, 1000);
    this.setVisible(true);
  }

  public static void monitorMemoryUsage() {
    new MemoryUsage();
  }

  private class UpdateTask extends TimerTask {

    @Override
    public void run() {
      MemoryUsage.this.button.setText("Used memory:" + (s_runtime.totalMemory() - s_runtime.freeMemory()));
    }

  }

}
