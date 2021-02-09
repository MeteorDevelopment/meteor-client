/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc;

import java.io.DataOutput;
import java.io.IOException;

public class ByteCountDataOutput implements DataOutput {
    public static final ByteCountDataOutput INSTANCE = new ByteCountDataOutput();

    private int count;

    public int getCount() {
        return count;
    }

    public void reset() {
        count = 0;
    }

    @Override
    public void write(int b) throws IOException {
        count++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        count += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        count += len;
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        count++;
    }

    @Override
    public void writeByte(int v) throws IOException {
        count++;
    }

    @Override
    public void writeShort(int v) throws IOException {
        count += 2;
    }

    @Override
    public void writeChar(int v) throws IOException {
        count += 2;
    }

    @Override
    public void writeInt(int v) throws IOException {
        count += 4;
    }

    @Override
    public void writeLong(long v) throws IOException {
        count += 8;
    }

    @Override
    public void writeFloat(float v) throws IOException {
        count += 4;
    }

    @Override
    public void writeDouble(double v) throws IOException {
        count += 8;
    }

    @Override
    public void writeBytes(String s) throws IOException {
        count += s.length();
    }

    @Override
    public void writeChars(String s) throws IOException {
        count += s.length() * 2;
    }

    @Override
    public void writeUTF(String s) throws IOException {
        count += 2 + getUTFLength(s);
    }

    long getUTFLength(String s) {
        long utflen = 0;
        for (int cpos = 0; cpos < s.length(); cpos++) {
            char c = s.charAt(cpos);
            if (c >= 0x0001 && c <= 0x007F) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }
        return utflen;
    }
}
