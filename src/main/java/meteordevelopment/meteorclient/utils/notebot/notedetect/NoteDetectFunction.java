/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.notedetect;

import net.minecraft.block.BlockState;
import net.minecraft.block.enums.Instrument;
import net.minecraft.util.math.BlockPos;

public interface NoteDetectFunction {
    /**
     * Detects an instrument for noteblock
     *
     * @param noteBlock Noteblock state
     * @param blockPos Noteblock position
     * @return Detected instrument
     */
    Instrument detectInstrument(BlockState noteBlock, BlockPos blockPos);
}
