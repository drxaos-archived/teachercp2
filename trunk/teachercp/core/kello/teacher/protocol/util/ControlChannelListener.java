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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class ControlChannelListener implements Runnable {

  protected MulticastSocket socket;

  protected InetAddress group;

  protected int port;

  protected Thread thread;

  public ControlChannelListener(String group, int port) throws UnknownHostException, IOException {

    this.port = port;
    this.group = InetAddress.getByName(group);

    /* setup the multicast control channel */
    this.socket = new MulticastSocket(port);
    System.out.println("Port: " + this.socket.getLocalPort());

    // socket.connect(this.group,port);
    this.socket.joinGroup(this.group);

    this.thread = new Thread(this);

  }

  private byte[] getDatagram() throws IOException {

    int size = this.socket.getReceiveBufferSize();
    byte[] buffer = new byte[size];

    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    this.socket.receive(packet);

    return packet.getData();

  }

  public void start() {
    this.thread.start();
  }

  @Override
  public void run() {

    while (true) {
      try {
        byte[] packet = getDatagram();

        ByteArrayInputStream b = new ByteArrayInputStream(packet);
        DataInputStream d = new DataInputStream(b);

        processMessage(d);

      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

  }

  protected void processMessage(DataInputStream data) {

  }

}
