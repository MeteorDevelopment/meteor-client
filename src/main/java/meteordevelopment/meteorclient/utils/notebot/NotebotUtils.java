/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot;

import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.notebot.instrumentdetect.InstrumentDetectFunction;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NotebotUtils {
    public static final EnumMap<Instrument, Item> INSTRUMENT_TO_ITEM = new EnumMap<>(Instrument.class) {{
        put(Instrument.HARP, Items.DIRT);
        put(Instrument.BASEDRUM, Items.STONE);
        put(Instrument.SNARE, Items.SAND);
        put(Instrument.HAT, Items.GLASS);
        put(Instrument.BASS, Items.OAK_WOOD);
        put(Instrument.FLUTE, Items.CLAY);
        put(Instrument.BELL, Items.GOLD_BLOCK);
        put(Instrument.GUITAR, Items.WHITE_WOOL);
        put(Instrument.CHIME, Items.PACKED_ICE);
        put(Instrument.XYLOPHONE, Items.BONE_BLOCK);
        put(Instrument.IRON_XYLOPHONE, Items.IRON_BLOCK);
        put(Instrument.COW_BELL, Items.SOUL_SAND);
        put(Instrument.DIDGERIDOO, Items.PUMPKIN);
        put(Instrument.BIT, Items.EMERALD_BLOCK);
        put(Instrument.BANJO, Items.HAY_BLOCK);
        put(Instrument.PLING, Items.GLOWSTONE);
    }};

    public static Note getNoteFromNoteBlock(BlockState noteBlock, BlockPos blockPos, NotebotMode mode, InstrumentDetectFunction instrumentDetectFunction) {
        Instrument instrument = null;
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
        Pling(Instrument.PLING);
        public static final Map<Instrument, OptionalInstrument> BY_MINECRAFT_INSTRUMENT = new HashMap<>();

        static {
            for (OptionalInstrument optionalInstrument : values()) {
                BY_MINECRAFT_INSTRUMENT.put(optionalInstrument.minecraftInstrument, optionalInstrument);
            }
        }

        private final Instrument minecraftInstrument;

        OptionalInstrument(@Nullable Instrument minecraftInstrument) {
            this.minecraftInstrument = minecraftInstrument;
        }

        public Instrument toMinecraftInstrument() {
            return minecraftInstrument;
        }

        public static OptionalInstrument fromMinecraftInstrument(Instrument instrument) {
            if (instrument != null) {
                return BY_MINECRAFT_INSTRUMENT.get(instrument);
            } else {
                return null;
            }
        }
    }

    public static List<String> getRequirementString(Collection<Note> notes) {
        List<String> requirements = new ArrayList<>();
        if (!notes.isEmpty()) {
            Map<Instrument, Integer> uniqueInstrumentNoteNumbers = new HashMap<>();
            for (Note note : notes) {
                uniqueInstrumentNoteNumbers.putIfAbsent(note.getInstrument(), 0);
                uniqueInstrumentNoteNumbers.computeIfPresent(note.getInstrument(), (instrument, i) -> i + 1);
            }
            requirements.add("%d note blocks.".formatted(notes.size()));
            for (Instrument instrument : uniqueInstrumentNoteNumbers.keySet()) {
                requirements.add("%d %s blocks. (%s)".formatted(uniqueInstrumentNoteNumbers.get(instrument),
                    Names.get(INSTRUMENT_TO_ITEM.getOrDefault(instrument, Items.SUSPICIOUS_STEW)), instrument.asString()));
            }
        }
        return requirements;
    }
}
