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

package kello.teacher.protocol.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

import kello.teacher.protocol.ControlChannelMessage;

public abstract class MessageStreamHandler implements Runnable {

  private Thread thread;

  private Socket socket;

  private boolean stopped;

  private ObjectOutputStream objOutputStream;

  public MessageStreamHandler(Socket s) throws IOException {
    this.thread = new Thread(this);
    this.socket = s;
    this.stopped = false;
    this.objOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
  }

  public void start() {
    this.thread.start();
  }

  public void stop() {
    this.stopped = true;
  }

  @Override
  public void run() {

    try {
      ObjectInputStream oi = new ObjectInputStream(this.socket.getInputStream());

      try {
        while (!this.stopped) {

          Object o = oi.readObject();

          if (!(o instanceof ControlChannelMessage)) {
            throw new ClassNotFoundException("Object is not a message");
          }
          processMessage((ControlChannelMessage) o);

        }
      } catch (ClassNotFoundException ex) {
        System.err.println("Unknown message:" + ex.toString());
      }

    } catch (EOFException ex) {
      System.out.println("Disconnected");
    } catch (SocketException ex) {
      System.out.println("Disconnected");
    } catch (IOException ex) {
      ex.printStackTrace();
      System.out.println("Error reading from the control socket");
    }

    cleanup();

  }

  protected void cleanup() {
    this.stopped = true;
  }

  abstract protected void processMessage(ControlChannelMessage msg);

  protected void sendMessage(ControlChannelMessage msg) throws IOException {

    this.objOutputStream.writeObject(msg);
    this.objOutputStream.flush();
    this.socket.getOutputStream().flush();
  }

  public boolean isStopped() {
    return this.stopped;
  }

}
