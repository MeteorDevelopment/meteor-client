/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Map;

/**
 * Prints the data of the block or the entity you are looking at.
 *
 * <p>A block shows its state and the data of its block entity, an entity shows the data the server
 * sent for it. The data is written to the chat and copied to the clipboard, because it is often
 * longer than the chat is comfortable with.
 */
public class Inspect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> key = sgGeneral.add(new KeybindSetting.Builder()
        .name("key")
        .description("Prints the data of the block or entity you are looking at.")
        .action(this::inspect)
        .build()
    );

    private final Setting<Boolean> copy = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-to-clipboard")
        .description("Copies the data to the clipboard as well.")
        .defaultValue(true)
        .build()
    );

    public Inspect() {
        super(Categories.Misc, "inspect", "Prints the data of the block or entity you are looking at.");
    }

    private void inspect() {
        if (mc.hitResult == null || mc.level == null) return;

        if (mc.hitResult instanceof EntityHitResult result) {
            Entity entity = result.getEntity();
            print("Entity " + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), entityData(entity));
        }
        else if (mc.hitResult instanceof BlockHitResult result) {
            BlockPos pos = result.getBlockPos();
            print("Block " + pos.toShortString(), blockData(pos));
        }
    }

    private void print(String title, CompoundTag data) {
        Component message = Component.literal(title + ": ").append(NbtUtils.toPrettyComponent(data));
        ((IChatHud) mc.gui.hud.getChat()).meteor$add(message, 0);

        if (copy.get()) {
            mc.keyboardHandler.setClipboard(data.toString());
        }
    }

    /** The state of the block and the data of its block entity, if it has one. */
    private CompoundTag blockData(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        CompoundTag data = NbtUtils.writeBlockState(state);

        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        if (blockEntity != null) {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mc.level.registryAccess());
            blockEntity.saveWithFullMetadata(output);

            CompoundTag blockEntityData = output.buildResult();
            for (Map.Entry<String, Tag> entry : blockEntityData.entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
        }

        return data;
    }

    /** The data the server sent for the entity. */
    private CompoundTag entityData(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mc.level.registryAccess());
        entity.saveWithoutId(output);

        CompoundTag data = output.buildResult();
        data.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());

        return data;
    }
}
