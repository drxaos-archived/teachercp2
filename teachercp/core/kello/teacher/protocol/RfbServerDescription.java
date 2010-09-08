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

package kello.teacher.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class RfbServerDescription implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1967538996794307077L;

  private int framebufferWidth;

  private int framebufferHeight;

  private int bitsPerPixel;

  private int depth;

  private boolean bigEndian;

  private boolean trueColour;

  private int redMax;

  private int greenMax;

  private int blueMax;

  private int redShift;

  private int greenShift;

  private int blueShift;

  private String name;

  private RfbServerDescription() {

  }

  public RfbServerDescription(int width, int height,
            int bitsPerPixel, int depth, boolean bigEndian,
            boolean trueColour,
            int redMax, int greenMax, int blueMax,
            int redShift, int greenShift, int blueShift, String name) {
    this.bigEndian = bigEndian;
    this.bitsPerPixel = bitsPerPixel;
    this.blueMax = blueMax;
    this.blueShift = blueShift;
    this.depth = depth;
    this.framebufferHeight = height;
    this.framebufferWidth = width;
    this.greenMax = greenMax;
    this.greenShift = greenShift;
    this.name = name;
    this.redMax = redMax;
    this.redShift = redShift;
    this.trueColour = trueColour;
  }

  public boolean isBigEndian() {
    return this.bigEndian;
  }

  public int getBitsPerPixel() {
    return this.bitsPerPixel;
  }

  public int getBlueMax() {
    return this.blueMax;
  }

  public int getBlueShift() {
    return this.blueShift;
  }

  public int getDepth() {
    return this.depth;
  }

  public int getFramebufferHeight() {
    return this.framebufferHeight;
  }

  public int getFramebufferWidth() {
    return this.framebufferWidth;
  }

  public int getGreenMax() {
    return this.greenMax;
  }

  public int getGreenShift() {
    return this.greenShift;
  }

  public String getName() {
    return this.name;
  }

  public int getRedMax() {
    return this.redMax;
  }

  public int getRedShift() {
    return this.redShift;
  }

  public boolean isTrueColour() {
    return this.trueColour;
  }

  public void setBigEndian(boolean bigEndian) {
    this.bigEndian = bigEndian;
  }

  public void setBitsPerPixel(int bitsPerPixel) {
    this.bitsPerPixel = bitsPerPixel;
  }

  public void setBlueMax(int blueMax) {
    this.blueMax = blueMax;
  }

  public void setBlueShift(int blueShift) {
    this.blueShift = blueShift;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  public void setFramebufferHeight(int framebufferHeight) {
    this.framebufferHeight = framebufferHeight;
  }

  public void setFramebufferWidth(int framebufferWidth) {
    this.framebufferWidth = framebufferWidth;
  }

  public void setGreenMax(int greenMax) {
    this.greenMax = greenMax;
  }

  public void setGreenShift(int greenShift) {
    this.greenShift = greenShift;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setRedMax(int redMax) {
    this.redMax = redMax;
  }

  public void setRedShift(int redShift) {
    this.redShift = redShift;
  }

  public void setTrueColour(boolean trueColour) {
    this.trueColour = trueColour;
  }

  public void writeBytes(OutputStream out) throws IOException {
    DataOutputStream d = new DataOutputStream(out);

    d.writeShort(getFramebufferWidth());
    d.writeShort(getFramebufferHeight());
    d.writeByte(getBitsPerPixel());
    d.writeByte(getDepth());
    d.writeByte(isBigEndian() ? 1 : 0);
    d.writeByte(isTrueColour() ? 1 : 0);
    d.writeShort(getRedMax());
    d.writeShort(getGreenMax());
    d.writeShort(getBlueMax());
    d.writeByte(getRedShift());
    d.writeByte(getGreenShift());
    d.writeByte(getBlueShift());

    d.writeInt(this.name.length());
    d.writeBytes(this.name);
  }

  public static RfbServerDescription readFromBytes(InputStream in) throws IOException {
    RfbServerDescription desc = new RfbServerDescription();

    DataInputStream d = new DataInputStream(in);

    desc.setFramebufferWidth(d.readShort());
    desc.setFramebufferHeight(d.readShort());
    desc.setBitsPerPixel(d.readByte());
    desc.setDepth(d.readByte());
    desc.setBigEndian(d.readByte() == 0 ? false : true);
    desc.setTrueColour(d.readByte() == 0 ? false : true);
    desc.setRedMax(d.readShort());
    desc.setGreenMax(d.readShort());
    desc.setBlueMax(d.readShort());
    desc.setRedShift(d.readByte());
    desc.setGreenShift(d.readByte());
    desc.setBlueShift(d.readByte());

    int len = d.readInt();
    byte[] b = new byte[len];

    int r = d.read(b, 0, len);

    if (r != len) {
      throw new IOException("Malformed packet");
    }

    desc.setName(new String(b));

    return desc;
  }

  @Override
  public String toString() {

    String str;

    str = "Size: " + getFramebufferWidth() + "x" + getFramebufferHeight() + "\n";
    str = str + "BitsPerPixel:" + getBitsPerPixel() + " Depth: " + getDepth() + "\n";
    str = str + "BigEndian: " + isBigEndian() + " TrueColor: " + isTrueColour() + "\n";
    str = str + "Max(RGB): " + getRedMax() + "/" + getGreenMax() + "/" + getBlueMax() + "\n";
    str = str + "Shift(RBG):" + getRedShift() + "/" + getGreenShift() + "/" + getBlueShift() + "\n";
    str = str + "Name: " + getName();

    return str;
  }

}
