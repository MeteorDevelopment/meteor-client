/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import org.jetbrains.annotations.Nullable;

public class ESPBlockDataScreen extends WindowScreen {
    private final ESPBlockData blockData;
    private final Setting<?> setting;
    private final @Nullable Runnable firstChangeConsumer;

    public ESPBlockDataScreen(GuiTheme theme, ESPBlockData blockData, Block block, BlockDataSetting<ESPBlockData> setting) {
        this(theme, blockData, setting, () -> setting.get().put(block, blockData));
    }

    public ESPBlockDataScreen(GuiTheme theme, ESPBlockData blockData, GenericSetting<ESPBlockData> setting) {
        this(theme, blockData, setting, null);
    }

    private ESPBlockDataScreen(GuiTheme theme, ESPBlockData blockData, Setting<?> setting, @Nullable Runnable firstChangeConsumer) {
        super(theme, "Configure Block");

        this.blockData = blockData;
        this.setting = setting;
        this.firstChangeConsumer = firstChangeConsumer;
    }

    @Override
    public void initWidgets() {
        Settings settings = new Settings();
        SettingGroup sgGeneral = settings.getDefaultGroup();
        SettingGroup sgTracer = settings.createGroup("Tracer");

        sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shape is rendered.")
            .defaultValue(ShapeMode.Lines)
            .onModuleActivated(shapeModeSetting -> shapeModeSetting.set(blockData.shapeMode))
            .onChanged(shapeMode -> {
                if (blockData.shapeMode != shapeMode) {
                    blockData.shapeMode = shapeMode;
                    onChanged();
                }
            })
            .build()
        );

        sgTracer.add(new BoolSetting.Builder()
            .name("tracer")
            .description("If tracer line is allowed to this block.")
            .defaultValue(true)
            .onModuleActivated(booleanSetting -> booleanSetting.set(blockData.tracer))
            .onChanged(aBoolean -> {
                if (blockData.tracer != aBoolean) {
                    blockData.tracer = aBoolean;
                    onChanged();
                }
            })
            .build()
        );

        settings.onActivated();
        add(theme.settings(settings)).expandX();
    }

    private void onChanged() {
        if (!blockData.isChanged() && firstChangeConsumer != null) {
            firstChangeConsumer.run();
        }

        setting.onChanged();
        blockData.changed();
    }
}
