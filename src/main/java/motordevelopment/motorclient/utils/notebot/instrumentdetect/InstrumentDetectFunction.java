/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.utils.notebot.instrumentdetect;

import net.minecraft.block.BlockState;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.util.math.BlockPos;

public interface InstrumentDetectFunction {
    /**
     * Detects an instrument for noteblock
     *
     * @param noteBlock Noteblock state
     * @param blockPos Noteblock position
     * @return Detected instrument
     */
    NoteBlockInstrument detectInstrument(BlockState noteBlock, BlockPos blockPos);
}
