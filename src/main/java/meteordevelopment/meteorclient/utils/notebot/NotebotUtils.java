/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot;

import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.notebot.instrumentdetect.InstrumentDetectFunction;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotebotUtils {
    private static Map<NoteBlockInstrument, Item> instrumentItems;

    /**
     * The item of the first registered block that produces the given instrument when placed under a note block.
     * Built from the block registry instead of a hand-written map so new/renamed instruments keep working.
     */
    public static Item getInstrumentItem(NoteBlockInstrument instrument) {
        Map<NoteBlockInstrument, Item> items = instrumentItems;
        if (items == null) instrumentItems = items = buildInstrumentItems();
        return items.getOrDefault(instrument, Items.AIR);
    }

    private static Map<NoteBlockInstrument, Item> buildInstrumentItems() {
        Map<NoteBlockInstrument, Item> items = new EnumMap<>(NoteBlockInstrument.class);

        for (Block block : BuiltInRegistries.BLOCK) {
            Item item = block.asItem();
            if (item == Items.AIR) continue;
            items.putIfAbsent(block.defaultBlockState().instrument(), item);
        }

        return items;
    }

    public static Note getNoteFromNoteBlock(BlockState noteBlock, BlockPos blockPos, NotebotMode mode, InstrumentDetectFunction instrumentDetectFunction) {
        NoteBlockInstrument instrument = null;
        int level = noteBlock.getValue(NoteBlock.NOTE);
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
        Pling(NoteBlockInstrument.PLING);
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

    public static List<String> getRequirementString(Collection<Note> notes) {
        List<String> requirements = new ArrayList<>();
        if (notes.isEmpty()) return requirements;

        Map<NoteBlockInstrument, Integer> instrumentNoteNumbers = new EnumMap<>(NoteBlockInstrument.class);
        for (Note note : notes) {
            NoteBlockInstrument instrument = note.getInstrument();
            // Instrument is null when the notebot mode does not care about instruments
            if (instrument != null) instrumentNoteNumbers.merge(instrument, 1, Integer::sum);
        }

        requirements.add("%d note blocks.".formatted(notes.size()));
        for (Map.Entry<NoteBlockInstrument, Integer> entry : instrumentNoteNumbers.entrySet()) {
            Item item = getInstrumentItem(entry.getKey());
            if (item == Items.AIR) {
                requirements.add("%d (%s)".formatted(entry.getValue(), entry.getKey().getSerializedName()));
            } else {
                requirements.add("%d %s blocks. (%s)".formatted(entry.getValue(), Names.get(item), entry.getKey().getSerializedName()));
            }
        }

        return requirements;
    }
}
