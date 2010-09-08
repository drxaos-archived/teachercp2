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

import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JOptionPane;

public class ExceptionDialogBox extends Object {
  static public void displayExceptionDialog(Component parent, Exception ex, String message) {
    String title = "Internal Error";
    String text = message + "\n\n" + "The details of the error the follows:\n\n" +
            ex.toString();

    String[] buttons = { "OK", "Save report" };

    int ret = JOptionPane.showOptionDialog(parent, text, title, JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, buttons, buttons[0]);

    if (ret == JOptionPane.NO_OPTION) {
      try {
        File report = new File("report.log");
        FileWriter out = new FileWriter(report);
        PrintWriter print = new PrintWriter(out);
        print.print(text);
        ex.printStackTrace(print);
        print.close();
      } catch (IOException ex1) {
        System.out.println(":'-( I'm really buggy! I can't do a dump of my exceptions");
        ex1.printStackTrace();
      }
    }

  }
}
