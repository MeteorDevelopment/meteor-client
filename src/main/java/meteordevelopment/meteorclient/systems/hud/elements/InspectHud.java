/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Shows the data of the block or the entity you are looking at.
 *
 * <p>A block shows its state and the data of its block entity, an entity shows the data the server
 * sent for it.
 */
public class InspectHud extends HudElement {
    public static final HudElementInfo<InspectHud> INFO = new HudElementInfo<>(Hud.GROUP, "inspect", "Displays the data of the block or entity you are looking at.", InspectHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    public final Setting<Boolean> showBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("show-blocks")
        .description("Shows the data of the block you are looking at.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> showEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("show-entities")
        .description("Shows the data of the entity you are looking at.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> maxLines = sgGeneral.add(new IntSetting.Builder()
        .name("max-lines")
        .description("Maximum amount of lines that are shown.")
        .defaultValue(10)
        .min(1)
        .max(40)
        .build()
    );

    public final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Text shadow.")
        .defaultValue(true)
        .build()
    );

    public final Setting<SettingColor> titleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("title-color")
        .description("Color of the title.")
        .defaultValue(new SettingColor(145, 61, 226))
        .build()
    );

    public final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the data.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    // Scale

    public final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .onChanged(_ -> calculateSize())
        .build()
    );

    public final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .onChanged(_ -> calculateSize())
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Background

    public final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    private String title = "";
    private final List<String> lines = new ArrayList<>();

    private InspectHud() {
        super(INFO);

        calculateSize();
    }

    private double getScale() {
        return customScale.get() ? scale.get() : 1;
    }

    private void calculateSize() {
        List<String> all = allLines();

        double width = 0;
        for (String line : all) {
            width = Math.max(width, HudRenderer.INSTANCE.textWidth(line, shadow.get(), getScale()));
        }

        setSize(width, all.size() * HudRenderer.INSTANCE.textHeight(shadow.get(), getScale()));
    }

    private List<String> allLines() {
        List<String> all = new ArrayList<>();
        if (!title.isEmpty()) all.add(title);
        all.addAll(lines);
        return all;
    }

    @Override
    public void tick(HudRenderer renderer) {
        update();
    }

    @Override
    public void render(HudRenderer renderer) {
        if (background.get()) renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());

        double lineY = y;
        for (String line : allLines()) {
            Color color = line.equals(title) ? titleColor.get() : textColor.get();
            renderer.text(line, x, lineY, color, shadow.get(), getScale());
            lineY += renderer.textHeight(shadow.get(), getScale());
        }
    }

    /** Reads the data of whatever the player is looking at. */
    private void update() {
        List<String> newLines = new ArrayList<>();
        String newTitle = "";

        if (mc.hitResult != null && mc.level != null) {
            if (mc.hitResult instanceof EntityHitResult result && showEntities.get()) {
                Entity entity = result.getEntity();
                newTitle = "Entity " + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                newLines.addAll(entries(entityData(entity)));
            }
            else if (mc.hitResult instanceof BlockHitResult result && showBlocks.get()) {
                BlockPos pos = result.getBlockPos();
                newTitle = "Block " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                newLines.addAll(entries(blockData(pos)));
            }
        }

        if (newLines.size() > maxLines.get()) {
            newLines = newLines.subList(0, maxLines.get());
        }

        if (newTitle.equals(title) && newLines.equals(lines)) return;

        title = newTitle;
        lines.clear();
        lines.addAll(newLines);
        calculateSize();
    }

    private static List<String> entries(CompoundTag data) {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, Tag> entry : data.entrySet()) {
            entries.add(entry.getKey() + ": " + entry.getValue());
        }
        return entries;
    }

    /** The state of the block and the data of its block entity, if it has one. */
    private CompoundTag blockData(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        CompoundTag data = net.minecraft.nbt.NbtUtils.writeBlockState(state);

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
