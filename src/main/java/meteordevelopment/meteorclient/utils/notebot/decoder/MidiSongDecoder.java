/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.decoder;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import meteordevelopment.meteorclient.utils.notebot.song.Song;
import net.minecraft.block.enums.Instrument;
import org.apache.commons.io.FilenameUtils;

import javax.sound.midi.*;
import java.io.File;

public class MidiSongDecoder extends SongDecoder {
    @Override
    public Song parse(File file) throws Exception {
        Multimap<Integer, Note> notes = MultimapBuilder.linkedHashKeys().arrayListValues().build();
        String title = FilenameUtils.getBaseName(file.getName());
        String author = "Unknown";

        Sequence seq = MidiSystem.getSequence(file);

        int res = seq.getResolution();
        for (Track track : seq.getTracks()) {
            long time;
            long bpm = 120;
            boolean skipNote = false;
            Instrument instrument = Instrument.HARP;
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();

                int ticksPerSecond = (int) (res * bpm / 60);
                time = (long) (1000d / ticksPerSecond * event.getTick());

                if (message instanceof ShortMessage msg) {
                    if (msg.getCommand() == 0xB0) {
                        // TODO: Convert ids to instruments
                    } else if (msg.getCommand() == 0x90 || msg.getCommand() == 0x80) {
                        int key = msg.getData1();
                        int note = key % 12;

                        if (!skipNote) {
                            notes.put((int) (time / 50d), new Note(instrument, note + 6));
                            skipNote = true;
                        } else {
                            skipNote = false;
                        }
                    }
                } else if (message instanceof MetaMessage msg) {
                    byte[] data = msg.getData();
                    if (msg.getType() != 0x03) {
                        if (msg.getType() == 0x51) {
                            int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                            bpm = 60000000 / tempo;
                        }
                    }
                }
            }
        }

        return new Song(notes, title, author);
    }
}
