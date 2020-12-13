/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.GetTooltipEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.ByteCountDataOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.io.IOException;

public class ItemByteSize extends ToggleModule {
    public enum Mode {
        Standard, True
    }

    private final SettingGroup sgUseKbIfBigEnough = settings.createGroup("Use KB if big enough");

    private final Setting<Boolean> useKbIfBigEnoughEnabled = sgUseKbIfBigEnough.add(new BoolSetting.Builder()
            .name("use-kb-if-big-enough-enabled")
            .description("Uses kilobytes instead of bytes if the item is larger than 1 kb.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Mode> mode = sgUseKbIfBigEnough.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Standard 1 kb = 1000 b, True 1 kb = 1024 b.")
            .defaultValue(Mode.True)
            .build()
    );

    public ItemByteSize() {
        super(Category.Misc, "item-byte-size", "Displays item's size in bytes in tooltip.");
    }

    @EventHandler
    private final Listener<GetTooltipEvent> onGetTooltip = new Listener<>(event -> {
        try {
            event.itemStack.toTag(new CompoundTag()).write(ByteCountDataOutput.INSTANCE);
            int byteCount = ByteCountDataOutput.INSTANCE.getCount();
            ByteCountDataOutput.INSTANCE.reset();

            event.list.add(new LiteralText(Formatting.GRAY + ModuleManager.INSTANCE.get(ItemByteSize.class).bytesToString(byteCount)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    });

    private int getKbSize() {
        return mode.get() == Mode.True ? 1024 : 1000;
    }

    public String bytesToString(int count) {
        if (useKbIfBigEnoughEnabled.get() && count >= getKbSize()) return String.format("%.2f kb", count / (float) getKbSize());
        return String.format("%d bytes", count);
    }
}
