/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot;

import meteordevelopment.meteorclient.utils.notebot.instrumentdetect.InstrumentDetectFunction;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class NotebotUtils {

    public static Note getNoteFromNoteBlock(BlockState noteBlock, BlockPos blockPos, NotebotMode mode, InstrumentDetectFunction instrumentDetectFunction) {
        NoteBlockInstrument instrument = null;
        int level = noteBlock.get(NoteBlock.NOTE);
        if (mode == NotebotMode.ExactInstruments) {
            instrument = instrumentDetectFunction.detectInstrument(noteBlock, blockPos);
        }

        return new Note(instrument, level);
    }

    public enum NotebotMode {
        AnyInstrument, ExactInstruments
    }

    public enum OptionalInstrument {
        None(null),
        Harp(NoteBlockInstrument.HARP),
        Basedrum(NoteBlockInstrument.BASEDRUM),
        Snare(NoteBlockInstrument.SNARE),
        Hat(NoteBlockInstrument.HAT),
        Bass(NoteBlockInstrument.BASS),
        Flute(NoteBlockInstrument.FLUTE),
        Bell(NoteBlockInstrument.BELL),
        Guitar(NoteBlockInstrument.GUITAR),
        Chime(NoteBlockInstrument.CHIME),
        Xylophone(NoteBlockInstrument.XYLOPHONE),
        IronXylophone(NoteBlockInstrument.IRON_XYLOPHONE),
        CowBell(NoteBlockInstrument.COW_BELL),
        Didgeridoo(NoteBlockInstrument.DIDGERIDOO),
        Bit(NoteBlockInstrument.BIT),
        Banjo(NoteBlockInstrument.BANJO),
        Pling(NoteBlockInstrument.PLING)
        ;
        public static final Map<NoteBlockInstrument, OptionalInstrument> BY_MINECRAFT_INSTRUMENT = new HashMap<>();

        static {
            for (OptionalInstrument optionalInstrument : values()) {
                BY_MINECRAFT_INSTRUMENT.put(optionalInstrument.minecraftInstrument, optionalInstrument);
            }
        }

        private final NoteBlockInstrument minecraftInstrument;

        OptionalInstrument(@Nullable NoteBlockInstrument minecraftInstrument) {
            this.minecraftInstrument = minecraftInstrument;
        }

        public NoteBlockInstrument toMinecraftInstrument() {
            return minecraftInstrument;
        }

        public static OptionalInstrument fromMinecraftInstrument(NoteBlockInstrument instrument) {
            if (instrument != null) {
                return BY_MINECRAFT_INSTRUMENT.get(instrument);
            } else {
                return null;
            }
        }
    }
}
