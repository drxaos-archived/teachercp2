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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import kello.teacher.util.ExceptionDialogBox;
import kello.teacher.util.Utility;

public class JTeacherFrame extends JFrame {
  private static final long serialVersionUID = -659098825070160220L;

  private JToolBar buttonPanel = null;
  private JButton startDemoButton = null;
  private JButton stopDemoButton = null;
  private TeacherAnnouncementDaemon announcer;

  private StudentHub hub;

  private JCheckBox chkFullscreen = null;
  private JLabel lblFullscreen = null;
  private JToggleButton lockScreenButton = null;
  private JDesktopPane jDesktopPane = null;

  private HashMap fullnames;

  private Properties properties;
  private JButton saveConfigButton = null;
  private JButton ReloadLayoutButton = null;

  public JTeacherFrame() {
    super("Teacher control panel");
    initialize();
  }

  /**
   * This method initializes this
   * 
   */
  private void initialize() {

    this.setTitle("Teacher Control Panel");

    this.fullnames = new HashMap();
    this.commands = new ArrayList();

    Properties defaultProperties = new Properties();

    defaultProperties.setProperty("vncserver", "127.0.0.1");
    defaultProperties.setProperty("vncport", "5900");
    defaultProperties.setProperty("vncpassword", "");
    defaultProperties.setProperty("screen-multicast-group", "230.0.0.1");
    defaultProperties.setProperty("screen-multicast-port", "3333");
    defaultProperties.setProperty("screen-multicast-ttl", "16");
    defaultProperties.setProperty("teacher-multicast-group", "230.0.0.2");
    defaultProperties.setProperty("teacher-multicast-port", "3000");
    defaultProperties.setProperty("teacher-multicast-ttl", "16");
    defaultProperties.setProperty("teacher-multicast-interval", "2000");
    defaultProperties.setProperty("teacher-port", "3333");
    defaultProperties.setProperty("height", "600");
    defaultProperties.setProperty("width", "800");
    defaultProperties.setProperty("vnc-client-reconnect-timeout", "10000");
    defaultProperties.setProperty("vnc-client-password", "");
    defaultProperties.setProperty("finger", "finger -l '%user'| head -1 | cut -f4 | cut -c 7-");

    this.properties = new Properties(defaultProperties);

    if (!new File("teacher.properties").exists() || !new File("commands.properties").exists()) {
      JOptionPane.showMessageDialog(this, "The configuration file was not found, please configure the application", "Install", JOptionPane.INFORMATION_MESSAGE);
      System.exit(1);
    }

    try {
      this.properties.load(new FileInputStream("teacher.properties"));

    } catch (Exception ex) {
      ExceptionDialogBox.displayExceptionDialog(this, ex, "Error while loading the configurations from teacher.properties (this is not fatal)");
    }

    this.add(getJPanel(), java.awt.BorderLayout.NORTH);
    this.add(getJDesktopPane(), java.awt.BorderLayout.CENTER);

    this.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);
      }
    });

    this.setSize(Integer.parseInt(this.properties.getProperty("width")),
            Integer.parseInt(this.properties.getProperty("height")));

    try {
      this.hub = new StudentHub(Integer.parseInt(this.properties.getProperty("teacher-port")), this);
      this.hub.start();
    } catch (Exception ex) {
      ExceptionDialogBox.displayExceptionDialog(this, ex, "Fatal error while starting the teacher server");
    }

    try {
      this.announcer = new TeacherAnnouncementDaemon(this.properties.getProperty("teacher-multicast-group"),
                            Integer.parseInt(this.properties.getProperty("teacher-multicast-port")),
                            Integer.parseInt(this.properties.getProperty("teacher-multicast-ttl")),
                            Integer.parseInt(this.properties.getProperty("teacher-port")),
                            Integer.parseInt(this.properties.getProperty("teacher-multicast-interval")));
      this.announcer.start();

    } catch (Exception ex) {
      ExceptionDialogBox.displayExceptionDialog(this, ex, "Fatal error while connecting to control group");
    }

  }

  public void saveConfiguration() {

    /* store the configurations */
    try {

      this.properties.store(new FileOutputStream("teacher.properties"), "Teacher configuration");

    } catch (IOException ex) {
      ExceptionDialogBox.displayExceptionDialog(this, ex, "Error saving the configurations in teacher.properties");
    }

    /* store the commands */
    try {

      Properties storedCommands = new Properties();

      Iterator i = this.commands.iterator();

      while (i.hasNext()) {
        Command cmd = (Command) i.next();

        String cmdString = cmd.getDescription() + "#" + cmd.getCommand();

        storedCommands.setProperty(cmd.getName(), cmdString);
      }

      storedCommands.store(new FileOutputStream("commands.properties"), "Teacher commands");

    } catch (Exception ex) {
      ExceptionDialogBox.displayExceptionDialog(this, ex, "Error saving the commands in commands.properties");
    }

  }

  private JComboBox commandList = null;
  private JButton executeCommandButton = null;
  private ArrayList commands;
  private JButton configureButton = null;

  private void setupCommandList() {

    try {
      Properties commandfile = new Properties();
      commandfile.load(new FileInputStream("commands.properties"));
      Iterator i = commandfile.entrySet().iterator();

      while (i.hasNext()) {
        Map.Entry entry = (Map.Entry) i.next();
        StringTokenizer st = new StringTokenizer((String) entry.getValue(), "#");
        this.commands.add(new Command((String) entry.getKey(), st.nextToken(), st.nextToken()));
        System.out.println("added" + this.commands.get(this.commands.size() - 1));
      }

    } catch (Exception ex) {
      ExceptionDialogBox.displayExceptionDialog(this, ex, "Error while loading the command list from commands.properties");
    }

  }

  /**
   * This method initializes jPanel
   * 
   * @return javax.swing.JPanel
   */
  private JToolBar getJPanel() {
    if (this.buttonPanel == null) {
      this.lblFullscreen = new JLabel();
      this.lblFullscreen.setText("Fullscreen");
      this.buttonPanel = new JToolBar();
      // this.buttonPanel.setFloatable(false);
      // this.buttonPanel.setMargin(new Insets(5, 5, 5, 5));
      // this.buttonPanel.setRollover(true);
      this.buttonPanel.setName("buttonPanel");
      this.buttonPanel.add(getJButton(), null);
      this.buttonPanel.add(getJButton2(), null);
      this.buttonPanel.add(getJCheckBox(), null);
      this.buttonPanel.add(this.lblFullscreen, null);
      this.buttonPanel.addSeparator();
      this.buttonPanel.add(getLockScreenButton(), null);
      this.buttonPanel.addSeparator();
      this.buttonPanel.add(getJButton3(), null);
      this.buttonPanel.add(getJButton4(), null);
      this.buttonPanel.addSeparator();
      this.buttonPanel.add(getJButton5(), null);
      this.buttonPanel.add(getCommandList(), null);
      this.buttonPanel.addSeparator();
    }
    return this.buttonPanel;
  }

  /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
  private JButton getJButton() {
    if (this.startDemoButton == null) {
      this.startDemoButton = new JButton();
      this.startDemoButton.setText("Start demo");
      this.startDemoButton.setActionCommand("Start demo on all");
      this.startDemoButton.setName("startDemoButton");
      this.startDemoButton.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          JTeacherFrame.this.hub.sendStartDemoToAll(JTeacherFrame.this.chkFullscreen.isSelected());
        }
      });
    }
    return this.startDemoButton;
  }

  /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
  private JButton getJButton2() {
    if (this.stopDemoButton == null) {
      this.stopDemoButton = new JButton();
      this.stopDemoButton.setName("stopDemoButton");
      this.stopDemoButton.setActionCommand("Stop demo on all");
      this.stopDemoButton.setText("Stop demo");
      this.stopDemoButton.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          JTeacherFrame.this.hub.sendStopDemoToAll();
        }
      });
    }
    return this.stopDemoButton;
  }

  /**
   * This method initializes jCheckBox
   * 
   * @return javax.swing.JCheckBox
   */
  private JCheckBox getJCheckBox() {
    if (this.chkFullscreen == null) {
      this.chkFullscreen = new JCheckBox();
      this.chkFullscreen.setName("chkFullscreen");
      this.chkFullscreen.setSelected(true);
    }
    return this.chkFullscreen;
  }

  /**
   * This method initializes jToggleButton
   * 
   * @return javax.swing.JToggleButton
   */
  private JToggleButton getLockScreenButton() {
    if (this.lockScreenButton == null) {
      this.lockScreenButton = new JToggleButton();
      this.lockScreenButton.setText("Lock");
      this.lockScreenButton.setName("lockScreenButton");
      this.lockScreenButton.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          JTeacherFrame.this.hub.sendLockScreenToAll(JTeacherFrame.this.lockScreenButton.isSelected());
        }
      });
    }
    return this.lockScreenButton;
  }

  /**
   * This method initializes jDesktopPane
   * 
   * @return javax.swing.JDesktopPane
   */
  private JDesktopPane getJDesktopPane() {
    if (this.jDesktopPane == null) {
      this.jDesktopPane = new JDesktopPane();
    }
    return this.jDesktopPane;
  }

  /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
  private JButton getJButton3() {
    if (this.saveConfigButton == null) {
      this.saveConfigButton = new JButton();
      this.saveConfigButton.setText("Save layout");
      this.saveConfigButton.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          JTeacherFrame.this.hub.savePositions();
          setProperty("height", "" + getHeight());
          setProperty("width", "" + getWidth());
        }
      });
    }
    return this.saveConfigButton;
  }

  /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
  private JButton getJButton4() {
    if (this.ReloadLayoutButton == null) {
      this.ReloadLayoutButton = new JButton();
      this.ReloadLayoutButton.setText("Reload layout");
      this.ReloadLayoutButton.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          JTeacherFrame.this.hub.relayoutStudents();
        }
      });
    }
    return this.ReloadLayoutButton;
  }

  /**
   * This method initializes jComboBox
   * 
   * @return javax.swing.JComboBox
   */
  private JComboBox getCommandList() {
    if (this.commandList == null) {
      this.commandList = new JComboBox();

      setupCommandList();
      this.commandList.setModel(new DefaultComboBoxModel(this.commands.toArray()));
    }
    return this.commandList;
  }

  /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
  private JButton getJButton5() {
    if (this.executeCommandButton == null) {
      this.executeCommandButton = new JButton();
      this.executeCommandButton.setText("Do");
      this.executeCommandButton.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          JTeacherFrame.this.hub.executeOnAll(((Command) JTeacherFrame.this.commandList.getSelectedItem()));
        }
      });
    }
    return this.executeCommandButton;
  }

  public void addNewStudent(JStudentThumbnail s) {
    s.setBounds(this.hub.getPreferredPosition(s.getStudent().getHost()));
    this.jDesktopPane.add(s);
  }

  public boolean isFullscreenSelected() {
    return getJCheckBox().isSelected();
  }

  public String getProperty(String key, String defaultValue) {
    return this.properties.getProperty(key, defaultValue);
  }

  public String getProperty(String key) {
    return this.properties.getProperty(key);
  }

  public Object setProperty(String key, String value) {
    return this.properties.setProperty(key, value);
  }

  public String getFullname(String login) {
    if (login == null) {
      return null;
    }
    if (this.fullnames.containsKey(login)) {
      return (String) this.fullnames.get(login);
    } else {
      String fullname = Utility.executeCommand(this.properties.getProperty("finger").replaceAll("%user", login));
      this.fullnames.put(login, fullname);
      return fullname;
    }
  }

  public Command getSelectedCommand() {
    return (Command) this.commandList.getSelectedItem();
  }

  public ArrayList getAllCommands() {
    return this.commands;
  }

}
