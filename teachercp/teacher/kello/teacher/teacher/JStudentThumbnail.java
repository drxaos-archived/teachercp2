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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import kello.teacher.protocol.StudentAnnounceMessage;
import kello.teacher.rfb.viewer.JRfbViewer;

public class JStudentThumbnail extends JInternalFrame {

  private static final long serialVersionUID = -86451957557630007L;

  private JRfbViewer vncviewer3 = null;

  private Timer timer;

  private StudentHandler student;

  private JPopupMenu jPopupMenu = null;

  private JCheckBoxMenuItem jLocked = null;

  private JCheckBoxMenuItem jDemo = null;

  /**
   * This is the default constructor
   */
  public JStudentThumbnail(StudentHandler student) {
    super();
    initialize();

    this.student = student;
    this.vncviewer3.setHost(student.getHost());
    this.vncviewer3.setUsername(student.getHub().getTeacher().getProperty("vnc-client-user"));
    this.vncviewer3.setPassword(student.getHub().getTeacher().getProperty("vnc-client-password"));
    this.timer = new Timer();

  }

  /**
   * This method initializes this
   * 
   * @return void
   */
  private void initialize() {
    this.setSize(340, 240);
    this.setResizable(true);
    this.setMaximizable(true);
    this.setIconifiable(true);
    setVisible(true);
    setContentPane(getVncviewer3());
  }

  /**
   * This method initializes vncviewer3
   * 
   * @return kello.teacher.demoviewer.vncviewer
   */
  private JRfbViewer getVncviewer3() {
    if (this.vncviewer3 == null) {
      this.vncviewer3 = new JRfbViewer();
      // vncviewer3.setSize(jPanel.getSize());
      this.vncviewer3.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
          if (e.getClickCount() == 2) {
            JStudentThumbnail.this.student.startVNCviewer();
          }
        }

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
          if (e.isPopupTrigger()) {
            getJPopupMenu().show(e.getComponent(), e.getX(), e.getY());
          }
        }

        @Override
        public void mouseReleased(java.awt.event.MouseEvent e) {
          if (e.isPopupTrigger()) {
            getJPopupMenu().show(e.getComponent(), e.getX(), e.getY());
          }
        }

      });
      // vncviewer3.setSize(200,200);
    }
    return this.vncviewer3;
  }

  public void showScreen() {

    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        if (JStudentThumbnail.this.vncviewer3.isStopped()) {
          JStudentThumbnail.this.vncviewer3.start();
        }
      }
    };

    this.timer.schedule(task, 0, Integer.parseInt(this.student.getHub().getTeacher().getProperty("vnc-client-reconnect-timeout")));
  }

  public void hideScreen() {
    this.timer.cancel();
    this.vncviewer3.stop();
  }

  @Override
  public void dispose() {
    hideScreen();
    super.dispose();
  }

  public void notifyNewAnnouncement() {

    StudentAnnounceMessage amsg = this.student.getLastAnnouncement();

    String name = this.student.getHub().getTeacher().getFullname(amsg.getName());
    if (name == null || name.equals("")) {
      try {
        name = InetAddress.getByName(this.student.getHost()).getHostName();
      } catch (UnknownHostException ex) {
        name = this.student.getHost();
      }
    }

    this.setTitle(name);

    getJLocked().setSelected(amsg.isLocked());
    getJDemo().setSelected(amsg.isInDemo());

    if (amsg.isFullscreen() && amsg.isInDemo()) {
      this.vncviewer3.setPaused(true);
    } else {
      this.vncviewer3.setPaused(false);
    }

    this.vncviewer3.setPort(amsg.getVncport());
    if (this.vncviewer3.isStopped()) {
      this.vncviewer3.start();
    }

  }

  /**
   * This method initializes jPopupMenu
   * 
   * @return javax.swing.JPopupMenu
   */
  private JPopupMenu getJPopupMenu() {
    if (this.jPopupMenu == null) {
      this.jPopupMenu = new JPopupMenu();
      this.jPopupMenu.add(getJLocked());
      this.jPopupMenu.add(getJDemo());

      ArrayList cmds = this.student.getHub().getTeacher().getAllCommands();

      Iterator i = cmds.iterator();

      while (i.hasNext()) {
        final Command cmd = (Command) i.next();

        ActionListener a = new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            JStudentThumbnail.this.student.executeCommand(cmd);
          }

        };

        JMenuItem item = new JMenuItem(cmd.getDescription());
        item.addActionListener(a);
        this.jPopupMenu.add(item);

      }

    }
    return this.jPopupMenu;
  }

  /**
   * This method initializes jCheckBoxMenuItem
   * 
   * @return javax.swing.JCheckBoxMenuItem
   */
  private JCheckBoxMenuItem getJLocked() {
    if (this.jLocked == null) {
      this.jLocked = new JCheckBoxMenuItem();
      this.jLocked.setText("Locked");
      this.jLocked.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          JStudentThumbnail.this.student.sendLockScreen(JStudentThumbnail.this.jLocked.isSelected());
        }
      });
    }
    return this.jLocked;
  }

  /**
   * This method initializes jCheckBoxMenuItem
   * 
   * @return javax.swing.JCheckBoxMenuItem
   */
  private JCheckBoxMenuItem getJDemo() {
    if (this.jDemo == null) {
      this.jDemo = new JCheckBoxMenuItem();
      this.jDemo.setText("Demo");
      this.jDemo.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (JStudentThumbnail.this.jDemo.isSelected()) {
            JTeacherFrame teacher = JStudentThumbnail.this.student.getHub().getTeacher();
            JStudentThumbnail.this.student.sendDemoStart(JStudentThumbnail.this.student.getHub().getDesc(), JStudentThumbnail.this.student.getHub().getMulticastGroup(), JStudentThumbnail.this.student
                .getHub().getMulticastPort(), teacher.isFullscreenSelected());
          } else {
            JStudentThumbnail.this.student.sendDemoStop();
          }
        }
      });
    }
    return this.jDemo;
  }

  public StudentHandler getStudent() {
    return this.student;
  }

}
