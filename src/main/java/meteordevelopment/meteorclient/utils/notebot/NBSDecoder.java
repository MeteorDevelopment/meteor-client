/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.notebot.nbs.Layer;
import meteordevelopment.meteorclient.utils.notebot.nbs.Note;
import meteordevelopment.meteorclient.utils.notebot.nbs.Song;

import java.io.*;
import java.util.HashMap;

//https://github.com/xxmicloxx/NoteBlockAPI/blob/master/src/main/java/com/xxmicloxx/NoteBlockAPI/NBSDecoder.java

public class NBSDecoder {
    public static Song parse(File decodeFile) {
        try {
            return parse(new FileInputStream(decodeFile), decodeFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Song parse(InputStream inputStream) {
        return parse(inputStream, null); // Source is unknown -> no file
    }

    private static Song parse(InputStream inputStream, File decodeFile) {
        try {
            DataInputStream dis = new DataInputStream(inputStream);
            short length = readShort(dis);
            if (length != 0) return parseClassic(dis, decodeFile, length);
            else return parseOpenNBS(dis, decodeFile);
        } catch (IOException e) {
            MeteorClient.LOG.error("", e);
        }

        return null;
    }

    private static Song parseClassic(DataInputStream dis, File decodeFile, short length) throws IOException {
        HashMap<Integer, Layer> layerHashMap = new HashMap<Integer, Layer>();
        short songHeight = readShort(dis);
        String title = readString(dis);
        String author = readString(dis);
        readString(dis);
        String description = readString(dis);
        float speed = readShort(dis) / 100f;
        dis.readBoolean(); // auto-save
        dis.readByte(); // auto-save duration
        dis.readByte(); // x/4ths, time signature
        readInt(dis); // minutes spent on project
        readInt(dis); // left clicks (why?)
        readInt(dis); // right clicks (why?)
        readInt(dis); // blocks added
        readInt(dis); // blocks removed
        readString(dis); // .mid/.schematic file name
        short tick = -1;
        while (true) {
            short jumpTicks = readShort(dis); // jumps till next tick
            //System.out.println("Jumps to next tick: " + jumpTicks);
            if (jumpTicks == 0) {
                break;
            }
            tick += jumpTicks;
            //System.out.println("Tick: " + tick);
            short layer = -1;
            while (true) {
                short jumpLayers = readShort(dis); // jumps till next layer
                if (jumpLayers == 0) {
                    break;
                }
                layer += jumpLayers;
                //System.out.println("Layer: " + layer);
                setNote(layer, tick, dis.readByte() /* instrument */, dis.readByte() /* note */, layerHashMap);
            }
        }
        for (int i = 0; i < songHeight; i++) {
            Layer l = layerHashMap.get(i);
            if (l != null) {
                l.setName(readString(dis));
                l.setVolume(dis.readByte());
            }
        }
        return new Song(speed, layerHashMap, songHeight, length, title, author, description, decodeFile);
    }

    private static Song parseOpenNBS(DataInputStream dis, File decodeFile) throws IOException {
        HashMap<Integer, Layer> layerHashMap = new HashMap<Integer, Layer>();
        byte version = dis.readByte();
        if (version != 5) return null;
        dis.readByte(); //vanillaInstrumentCount
        short length = readShort(dis);
        short songHeight = readShort(dis);
        String title = readString(dis);
        String author = readString(dis);
        readString(dis); //originalAuthor
        String description = readString(dis);
        float speed = readShort(dis) / 100f;
        dis.readByte(); //Auto-saving
        dis.readByte(); //Auto-saving duration
        dis.readByte(); // x/4ths, time signature
        readInt(dis); // minutes spent on project
        readInt(dis); // left clicks (why?)
        readInt(dis); // right clicks (why?)
        readInt(dis); // blocks added
        readInt(dis); // blocks removed
        readString(dis); // .mid/.schematic file name
        dis.readByte(); //Loop on/off
        dis.readByte(); //loop count
        readShort(dis); //Loop start tick
        short tick = -1;
        while (true) {
            short jumpTicks = readShort(dis); // jumps till next tick
            //System.out.println("Jumps to next tick: " + jumpTicks);
            if (jumpTicks == 0) {
                break;
            }
            tick += jumpTicks;
            //System.out.println("Tick: " + tick);
            short layer = -1;
            while (true) {
                short jumpLayers = readShort(dis); // jumps till next layer
                if (jumpLayers == 0) {
                    break;
                }
                layer += jumpLayers;
                //System.out.println("Layer: " + layer);
                setNote(layer, tick, dis.readByte() /* instrument */, dis.readByte() /* note */, layerHashMap);
                dis.readByte(); //velocity
                dis.readByte(); //panning
                readShort(dis); //pitch
            }
        }
        for (int i = 0; i < songHeight; i++) {
            Layer l = layerHashMap.get(i);
            if (l != null) {
                l.setName(readString(dis));
                dis.readByte(); //lock
                l.setVolume(dis.readByte());
                dis.readByte(); //stereo
            }
        }
        return new Song(speed, layerHashMap, songHeight, length, title, author, description, decodeFile);
    }

    private static void setNote(int layer, int ticks, byte instrument, byte key, HashMap<Integer, Layer> layerHashMap) {
        Layer l = layerHashMap.get(layer);
        if (l == null) {
            l = new Layer();
            layerHashMap.put(layer, l);
        }
        l.setNote(ticks, new Note(instrument, key));
    }

    private static short readShort(DataInputStream dis) throws IOException {
        int byte1 = dis.readUnsignedByte();
        int byte2 = dis.readUnsignedByte();
        return (short) (byte1 + (byte2 << 8));
    }

    private static int readInt(DataInputStream dis) throws IOException {
        int byte1 = dis.readUnsignedByte();
        int byte2 = dis.readUnsignedByte();
        int byte3 = dis.readUnsignedByte();
        int byte4 = dis.readUnsignedByte();
        return (byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24));
    }

    private static String readString(DataInputStream dis) throws IOException {
        int length = readInt(dis);
        StringBuilder sb = new StringBuilder(length);
        for (; length > 0; --length) {
            char c = (char) dis.readByte();
            if (c == (char) 0x0D) {
                c = ' ';
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
