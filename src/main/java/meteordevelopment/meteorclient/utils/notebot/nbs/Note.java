/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.nbs;

import meteordevelopment.meteorclient.utils.notebot.NotebotUtils;
import net.minecraft.block.enums.Instrument;

import java.util.Objects;

public class Note {

    private int instrument; // -1 when can be any instrument
    private int key;

    public Note(int instrument, int key) {
        this.instrument = instrument;
        this.key = key;
    }

    public int getRawInstrument() {
        return instrument;
    }

    public void setRawInstrument(int rawInstrument) {
        this.instrument = rawInstrument;
    }

    public Instrument getInstrument() {
        return NotebotUtils.fromNBSInstrument(instrument);
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = NotebotUtils.toNBSInstrument(instrument);
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getNoteLevel() {
        return this.key - NotebotUtils.NOTE_OFFSET;
    }

    public void setNoteLevel(int level) {
        this.key = level + NotebotUtils.NOTE_OFFSET;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return instrument == note.instrument && key == note.key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrument, key);
    }

    @Override
    public String toString() {
        return "Note{" +
            "instrument=" + getInstrument() +
            ", noteLevel=" + getNoteLevel() +
            '}';
    }
}
