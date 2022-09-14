/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.notebot.nbs.Layer;
import meteordevelopment.meteorclient.utils.notebot.nbs.Note;
import meteordevelopment.meteorclient.utils.notebot.nbs.Song;

import java.io.*;
import java.util.HashMap;

// https://github.com/koca2000/NoteBlockAPI/blob/master/src/main/java/com/xxmicloxx/NoteBlockAPI/utils/NBSDecoder.java

/**
 * Utils for reading Note Block Studio data
 *
 */
public class NBSDecoder {

    /**
     * Parses a Song from a Note Block Studio project file (.nbs)
     * @see Song
     * @param songFile .nbs file
     * @return Song object representing a Note Block Studio project
     */
    public static Song parse(File songFile) {
        try {
            return parse(new FileInputStream(songFile), songFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Parses a Song from an InputStream
     * @see Song
     * @param inputStream of a Note Block Studio project file (.nbs)
     * @return Song object from the InputStream
     */
    public static Song parse(InputStream inputStream) {
        return parse(inputStream, null); // Source is unknown -> no file
    }

    /**
     * Parses a Song from an InputStream and a Note Block Studio project file (.nbs)
     * @see Song
     * @param inputStream of a .nbs file
     * @param songFile representing a .nbs file
     * @return Song object representing the given .nbs file
     */
    private static Song parse(InputStream inputStream, File songFile) {
        HashMap<Integer, Layer> layerHashMap = new HashMap<Integer, Layer>();
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            short length = readShort(dataInputStream);
            int nbsversion = 0;
            if (length == 0) {
                nbsversion = dataInputStream.readByte();
                dataInputStream.readByte(); // first custom instrument
                if (nbsversion >= 3) {
                    length = readShort(dataInputStream);
                }
            }
            short songHeight = readShort(dataInputStream);
            String title = readString(dataInputStream);
            String author = readString(dataInputStream);
            readString(dataInputStream); // original author
            String description = readString(dataInputStream);
            float speed = readShort(dataInputStream) / 100f;
            dataInputStream.readBoolean(); // auto-save
            dataInputStream.readByte(); // auto-save duration
            dataInputStream.readByte(); // x/4ths, time signature
            readInt(dataInputStream); // minutes spent on project
            readInt(dataInputStream); // left clicks (why?)
            readInt(dataInputStream); // right clicks (why?)
            readInt(dataInputStream); // blocks added
            readInt(dataInputStream); // blocks removed
            readString(dataInputStream); // .mid/.schematic file name
            if (nbsversion >= 4) {
                dataInputStream.readByte(); // loop on/off
                dataInputStream.readByte(); // max loop count
                readShort(dataInputStream); // loop start tick
            }
            short tick = -1;
            while (true) {
                short jumpTicks = readShort(dataInputStream); // jumps till next tick
                //System.out.println("Jumps to next tick: " + jumpTicks);
                if (jumpTicks == 0) {
                    break;
                }
                tick += jumpTicks;
                //System.out.println("Tick: " + tick);
                short layer = -1;
                while (true) {
                    short jumpLayers = readShort(dataInputStream); // jumps till next layer
                    if (jumpLayers == 0) {
                        break;
                    }
                    layer += jumpLayers;
                    //System.out.println("Layer: " + layer);
                    byte instrument = dataInputStream.readByte();

                    byte key = dataInputStream.readByte();
                    if (nbsversion >= 4) {
                        dataInputStream.readByte(); // note block velocity
                        dataInputStream.readUnsignedByte(); // note panning, 0 is right in nbs format
                        readShort(dataInputStream); // note block pitch
                    }

                    setNote(layer, tick,
                        new Note(instrument /* instrument */, key/* note */),
                        layerHashMap);
                }
            }

            if (nbsversion > 0 && nbsversion < 3) {
                length = tick;
            }

            for (int i = 0; i < songHeight; i++) {
                Layer layer = layerHashMap.get(i);

                String name = readString(dataInputStream);
                if (nbsversion >= 4){
                    dataInputStream.readByte(); // layer lock
                }

                byte volume = dataInputStream.readByte();
                if (nbsversion >= 2){
                    dataInputStream.readUnsignedByte(); // layer stereo, 0 is right in nbs format
                }

                if (layer != null) {
                    layer.setName(name);
                    layer.setVolume(volume);
                }
            }
            //count of custom instruments
            byte customAmnt = dataInputStream.readByte();

            for (int index = 0; index < customAmnt; index++) {
                readString(dataInputStream); // name
                readString(dataInputStream); // sound file name
                dataInputStream.readByte();//pitch
                dataInputStream.readByte();//key
            }

            return new Song(speed, layerHashMap, songHeight, length, title,
                author, description, songFile);
        } catch (EOFException e) {
            String file = "";
            if (songFile != null) {
                file = songFile.getName();
            }
            MeteorClient.LOG.error("Song is corrupted: " + file, e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sets a note at a tick in a song
     * @param layerIndex
     * @param ticks
     * @param note
     * @param layerHashMap
     */
    private static void setNote(int layerIndex, int ticks, Note note, HashMap<Integer, Layer> layerHashMap) {
        Layer layer = layerHashMap.get(layerIndex);
        if (layer == null) {
            layer = new Layer();
            layerHashMap.put(layerIndex, layer);
        }
        layer.setNote(ticks, note);
    }

    private static short readShort(DataInputStream dataInputStream) throws IOException {
        int byte1 = dataInputStream.readUnsignedByte();
        int byte2 = dataInputStream.readUnsignedByte();
        return (short) (byte1 + (byte2 << 8));
    }

    private static int readInt(DataInputStream dataInputStream) throws IOException {
        int byte1 = dataInputStream.readUnsignedByte();
        int byte2 = dataInputStream.readUnsignedByte();
        int byte3 = dataInputStream.readUnsignedByte();
        int byte4 = dataInputStream.readUnsignedByte();
        return (byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24));
    }

    private static String readString(DataInputStream dataInputStream) throws IOException {
        int length = readInt(dataInputStream);
        StringBuilder builder = new StringBuilder(length);
        for (; length > 0; --length) {
            char c = (char) dataInputStream.readByte();
            if (c == (char) 0x0D) {
                c = ' ';
            }
            builder.append(c);
        }
        return builder.toString();
    }

}
