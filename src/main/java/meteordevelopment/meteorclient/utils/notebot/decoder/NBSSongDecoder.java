/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.decoder;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import meteordevelopment.meteorclient.utils.notebot.song.Song;
import net.minecraft.block.enums.Instrument;

import java.io.*;

// https://github.com/koca2000/NoteBlockAPI/blob/master/src/main/java/com/xxmicloxx/NoteBlockAPI/utils/NBSDecoder.java

/**
 * Utils for reading Note Block Studio data
 *
 */
public class NBSSongDecoder extends SongDecoder {

    public static final int NOTE_OFFSET = 33; // Magic value (https://opennbs.org/nbs)

    /**
     * Parses a Song from a Note Block Studio project file (.nbs)
     * @see Song
     * @param songFile .nbs file
     * @return Song object representing a Note Block Studio project
     */
    @Override
    public Song parse(File songFile) {
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
    public Song parse(InputStream inputStream) {
        return parse(inputStream, null); // Source is unknown -> no file
    }

    /**
     * Parses a Song from an InputStream and a Note Block Studio project file (.nbs)
     * @see Song
     * @param inputStream of a .nbs file
     * @param songFile representing a .nbs file
     * @return Song object representing the given .nbs file
     */
    private Song parse(InputStream inputStream, File songFile) {
        Multimap<Integer, Note> notesMap = MultimapBuilder.linkedHashKeys().arrayListValues().build();

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
            readShort(dataInputStream); // Song Height
            String title = readString(dataInputStream);
            String author = readString(dataInputStream);
            readString(dataInputStream); // original author
            readString(dataInputStream); // description
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

            double tick = -1;
            while (true) {
                short jumpTicks = readShort(dataInputStream); // jumps till next tick
                //System.out.println("Jumps to next tick: " + jumpTicks);
                if (jumpTicks == 0) {
                    break;
                }
                tick += jumpTicks * (20f / speed);
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

                    Note note = new Note(fromNBSInstrument(instrument) /* instrument */, key - NOTE_OFFSET /* note */);
                    setNote((int) Math.round(tick), note, notesMap);
                }
            }

            return new Song(notesMap, title, author);
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
     * @param ticks
     * @param note
     * @param notesMap
     */
    private static void setNote(int ticks, Note note, Multimap<Integer, Note> notesMap) {
        notesMap.put(ticks, note);
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
        if (length < 0) {
            throw new EOFException("Length can't be negative! Length: " + length);
        }
        if (length > dataInputStream.available()) {
            throw new EOFException("Can't read string that is larger than a buffer! Length: " + length + " Readable Bytes Length: " + dataInputStream.available());
        }

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

    // Magic Values (https://opennbs.org/nbs)
    private static Instrument fromNBSInstrument(int instrument) {
        return switch (instrument) {
            case 0 -> Instrument.HARP;
            case 1 -> Instrument.BASS;
            case 2 -> Instrument.BASEDRUM;
            case 3 -> Instrument.SNARE;
            case 4 -> Instrument.HAT;
            case 5 -> Instrument.GUITAR;
            case 6 -> Instrument.FLUTE;
            case 7 -> Instrument.BELL;
            case 8 -> Instrument.CHIME;
            case 9 -> Instrument.XYLOPHONE;
            case 10 -> Instrument.IRON_XYLOPHONE;
            case 11 -> Instrument.COW_BELL;
            case 12 -> Instrument.DIDGERIDOO;
            case 13 -> Instrument.BIT;
            case 14 -> Instrument.BANJO;
            case 15 -> Instrument.PLING;
            default -> null;
        };
    }

}
