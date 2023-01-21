/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot.notedetect;

import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import net.minecraft.client.MinecraftClient;

public enum NoteDetectMode {
    BlockState(((noteBlock, blockPos) -> noteBlock.get(NoteBlock.INSTRUMENT))),
    BelowBlock(((noteBlock, blockPos) -> Instrument.fromBelowState(MinecraftClient.getInstance().world.getBlockState(blockPos.down()))))
    ;

    private final NoteDetectFunction noteDetectFunction;

    NoteDetectMode(NoteDetectFunction noteDetectFunction) {
        this.noteDetectFunction = noteDetectFunction;
    }

    public NoteDetectFunction getNoteDetectFunction() {
        return noteDetectFunction;
    }
}
