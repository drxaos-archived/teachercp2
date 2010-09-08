/*
 * Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
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

public class RfbOptions {

  //
  // The actual data which other classes look at:
  //

  private int[] encodings = new int[10];

  private int nEncodings;

  private boolean reverseMouseButtons2And3;

  private boolean drawEachPixelForRawRects;

  private boolean copyRectFast;

  private boolean shareDesktop;

  private boolean useCopyRect;

  private int defaultEncoding;

  public RfbOptions() {
    super();

    // Set up defaults

    this.defaultEncoding = RfbProto.EncodingHextile;
    this.useCopyRect = true;
    this.reverseMouseButtons2And3 = false;
    this.drawEachPixelForRawRects = true;
    this.copyRectFast = true;
    this.shareDesktop = true;

    setDefaultEncodings();
  }

  private void setDefaultEncodings() {
    this.nEncodings = 0;
    if (this.useCopyRect) {
      this.encodings[this.nEncodings++] = RfbProto.EncodingCopyRect;
    }

    int preferredEncoding = RfbProto.EncodingRaw;

    preferredEncoding = this.defaultEncoding;

    if (preferredEncoding == RfbProto.EncodingRaw) {
      this.drawEachPixelForRawRects = false;
    }

    this.encodings[this.nEncodings++] = preferredEncoding;
    if (preferredEncoding != RfbProto.EncodingRRE) {
      this.encodings[this.nEncodings++] = RfbProto.EncodingRRE;
    }
    if (preferredEncoding != RfbProto.EncodingCoRRE) {
      this.encodings[this.nEncodings++] = RfbProto.EncodingCoRRE;
    }
    if (preferredEncoding != RfbProto.EncodingHextile) {
      this.encodings[this.nEncodings++] = RfbProto.EncodingHextile;
    }
  }

  public void addEncoding(int value) {
    this.encodings[this.nEncodings++] = value;
  }

  public int[] getEncodings() {
    return this.encodings.clone();
  }

  public boolean isShareDesktop() {
    return this.shareDesktop;
  }

  public void setShareDesktop(boolean shareDesktop) {
    this.shareDesktop = shareDesktop;
  }

  public boolean isCopyRectFast() {
    return this.copyRectFast;
  }

  public void setCopyRectFast(boolean copyRectFast) {
    this.copyRectFast = copyRectFast;
  }

  public boolean isDrawEachPixelForRawRects() {
    return this.drawEachPixelForRawRects;
  }

  public void setDrawEachPixelForRawRects(boolean drawEachPixelForRawRects) {
    this.drawEachPixelForRawRects = drawEachPixelForRawRects;
  }

  public boolean isReverseMouseButtons2And3() {
    return this.reverseMouseButtons2And3;
  }

  public void setReverseMouseButtons2And3(boolean reverseMouseButtons2And3) {
    this.reverseMouseButtons2And3 = reverseMouseButtons2And3;
  }

  public boolean isUseCopyRect() {
    return this.useCopyRect;
  }

  public void setUseCopyRect(boolean useCopyRect) {
    this.useCopyRect = useCopyRect;
  }

  public int getNEncodings() {
    return this.nEncodings;
  }

}
