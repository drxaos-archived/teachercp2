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

package kello.teacher.rfb;

public class MulticastRfbOptions extends RfbOptions {

  private String multicastGroup;
  private int multicastPort;
  private int ttl;

  public String getMulticastGroup() {
    return this.multicastGroup;
  }

  public void setMulticastGroup(String multicastGroup) {
    this.multicastGroup = multicastGroup;
  }

  public int getMulticastPort() {
    return this.multicastPort;
  }

  public void setMulticastPort(int multicastPort) {
    this.multicastPort = multicastPort;
  }

  public int getTtl() {
    return this.ttl;
  }

  public void setTtl(int ttl) {
    this.ttl = ttl;
  }

}
