/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot;

import meteordevelopment.meteorclient.utils.notebot.nbs.Note;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import org.jetbrains.annotations.Nullable;

public class NotebotUtils {
    public static final int NOTE_OFFSET = 33; // Magic value (https://opennbs.org/nbs)

    // Magic Values (https://opennbs.org/nbs)
    public static Instrument fromNBSInstrument(int instrument) {
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

    public static int toNBSInstrument(Instrument instrument) {
        return switch (instrument) {
            case HARP -> 0;
            case BASS -> 1;
            case BASEDRUM -> 2;
            case SNARE -> 3;
            case HAT -> 4;
            case GUITAR -> 5;
            case FLUTE -> 6;
            case BELL -> 7;
            case CHIME -> 8;
            case XYLOPHONE -> 9;
            case IRON_XYLOPHONE -> 10;
            case COW_BELL -> 11;
            case DIDGERIDOO -> 12;
            case BIT -> 13;
            case BANJO -> 14;
            case PLING -> 15;
        };
    }

    public static Note getNoteFromNoteBlock(BlockState noteBlock, NotebotMode mode) {
        int instrument = -1;
        int level = noteBlock.get(NoteBlock.NOTE) + NOTE_OFFSET;
        if (mode == NotebotMode.ExactInstruments) {
            Instrument blockInstrument = noteBlock.get(NoteBlock.INSTRUMENT);
            instrument = NotebotUtils.toNBSInstrument(blockInstrument);
        }

        return new Note(instrument, level);
    }

    public enum NotebotMode {
        AnyInstrument, ExactInstruments
    }

    public enum NullableInstrument {
        None(null),
        Harp(Instrument.HARP),
        Basedrum(Instrument.BASEDRUM),
        Snare(Instrument.SNARE),
        Hat(Instrument.HAT),
        Bass(Instrument.BASS),
        Flute(Instrument.FLUTE),
        Bell(Instrument.BELL),
        Guitar(Instrument.GUITAR),
        Chime(Instrument.CHIME),
        Xylophone(Instrument.XYLOPHONE),
        IronXylophone(Instrument.IRON_XYLOPHONE),
        CowBell(Instrument.COW_BELL),
        Didgeridoo(Instrument.DIDGERIDOO),
        Bit(Instrument.BIT),
        Banjo(Instrument.BANJO),
        Pling(Instrument.PLING)
        ;

        private final Instrument minecraftInstrument;

        NullableInstrument(@Nullable Instrument minecraftInstrument) {
            this.minecraftInstrument = minecraftInstrument;
        }

        public Instrument toMinecraftInstrument() {
            return minecraftInstrument;
        }

        public static NullableInstrument fromMinecraftInstrument(Instrument instrument) {
            return instrument == null ? null : NullableInstrument.values()[instrument.ordinal()+1]; // Offset by 1 to exclude 'None'
        }
    }
}
