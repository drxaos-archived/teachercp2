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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Utility {

  public static void startCommand(String command) {
    try {
      String[] trueCommand = { "bash", "-c", command };

      Runtime.getRuntime().exec(trueCommand);

    } catch (IOException ex) {
      ex.printStackTrace();
    }

  }

  public static String executeCommand(String command) {
    try {
      String[] trueCommand = { "bash", "-c", command };

      Process p = Runtime.getRuntime().exec(trueCommand);

      p.waitFor();

      BufferedReader in = new BufferedReader(new InputStreamReader(p
          .getInputStream()));

      String line;

      String ret = null;
      while ((line = in.readLine()) != null) {
        ret = line;
      }
      in.close();

      return ret;

    } catch (InterruptedException ex) {
      return null;
    } catch (IOException ex) {
      return null;
    }
  }

  public static void main(String[] args) {
    String s = executeCommand("w -h | grep ' :0 ' | cut -d ' ' -f 1 | head -n1");
    String s1 = executeCommand("finger -l " + s + "| head -1 | cut -f4 | cut -c 7-");
    System.out.println(s1);
  }

}
