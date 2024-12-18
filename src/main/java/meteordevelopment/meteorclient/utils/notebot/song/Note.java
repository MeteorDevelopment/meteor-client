/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.song;

import net.minecraft.block.enums.NoteBlockInstrument;

import java.util.Objects;

public class Note {

    private NoteBlockInstrument instrument;
    private int noteLevel;

    public Note(NoteBlockInstrument instrument, int noteLevel) {
        this.instrument = instrument;
        this.noteLevel = noteLevel;
    }

    public NoteBlockInstrument getInstrument() {
        return this.instrument;
    }

    public void setInstrument(NoteBlockInstrument instrument) {
        this.instrument = instrument;
    }

    public int getNoteLevel() {
        return noteLevel;
    }

    public void setNoteLevel(int noteLevel) {
        this.noteLevel = noteLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return instrument == note.instrument && noteLevel == note.noteLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrument, noteLevel);
    }

    @Override
    public String toString() {
        return "Note{" +
            "instrument=" + getInstrument() +
            ", noteLevel=" + getNoteLevel() +
            '}';
    }
}
