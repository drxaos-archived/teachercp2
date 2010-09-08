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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

abstract public class ControlChannelNotifier extends TimerTask {

  protected MulticastSocket socket;

  protected Timer timer;

  protected InetAddress group;

  protected int port;

  protected int ttl;

  protected int interval;

  /**
   * Creates a new control channel daemon on the specified group
   * 
   * @param group
   * @param port
   * @param ttl
   * @param desc
   * 
   * @throws UnknownHostException
   * @throws IOException
   */
  public ControlChannelNotifier(String group, int port, int ttl,
       int interval) throws UnknownHostException, IOException {

    this.port = port;
    this.group = InetAddress.getByName(group);
    this.ttl = ttl;

    /* setup the multicast control channel */
    this.socket = new MulticastSocket();
    System.out.println("Port: " + this.socket.getLocalPort());
    this.socket.setTimeToLive(ttl);
    System.out.println("TTL: " + this.socket.getTimeToLive());

    /* schedule notifications on the channel */
    this.timer = new Timer();

    this.interval = interval;
  }

  /**
   * Start broadcasting messages
   * 
   */
  public void start() {
    this.timer.schedule(this, 0, this.interval);
  }

  /**
   * Send a message with the current status of the demo
   */
  @Override
  public void run() {

    byte[] b = null;

    // System.out.println("Sending message on control channel");

    b = constructMessage();

    DatagramPacket packet = new DatagramPacket(b, b.length, this.group, this.port);
    try {
      this.socket.send(packet);
    } catch (IOException e) {
      System.err.println(e);
      try {
        System.out.println("Message Size: " + b.length);
        System.out.println("SendBufferSize: "
            + this.socket.getSendBufferSize());
      } catch (SocketException se) {
        System.err.println(se);
      }
    }

  }

  abstract protected byte[] constructMessage();

}
