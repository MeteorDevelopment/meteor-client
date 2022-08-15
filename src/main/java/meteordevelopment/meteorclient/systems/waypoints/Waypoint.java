/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.waypoints;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class Waypoint implements ISerializable<Waypoint> {
    public final Settings settings = new Settings();

    private final SettingGroup sgVisual = settings.createGroup("Visual");
    private final SettingGroup sgPosition = settings.createGroup("Position");

    public Setting<String> nameSetting = sgVisual.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the waypoint.")
        .defaultValue("Home")
        .build()
    );

    public Setting<String> iconSetting = sgVisual.add(new ProvidedStringSetting.Builder()
        .name("icon")
        .description("The icon of the waypoint.")
        .defaultValue("Square")
        .supplier(() -> Waypoints.BUILTIN_ICONS)
        .onChanged(v -> validateIcon())
        .build()
    );

    public Setting<SettingColor> colorSetting = sgVisual.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the waypoint.")
        .defaultValue(MeteorClient.ADDON.color.toSetting())
        .build()
    );

    public Setting<Boolean> visibleSetting = sgVisual.add(new BoolSetting.Builder()
        .name("visible")
        .description("Whether to show the waypoint.")
        .defaultValue(true)
        .build()
    );

    public Setting<Integer> maxVisibleSetting = sgVisual.add(new IntSetting.Builder()
        .name("max-visible-distance")
        .description("How far away to render the waypoint.")
        .defaultValue(5000)
        .build()
    );

    public Setting<Double> scaleSetting = sgVisual.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the waypoint.")
        .defaultValue(1)
        .build()
    );

    public Setting<BlockPos> posSetting = sgPosition.add(new BlockPosSetting.Builder()
        .name("location")
        .description("The location of the waypoint.")
        .defaultValue(BlockPos.ORIGIN)
        .build()
    );

    public Setting<Dimension> dimensionSetting = sgPosition.add(new EnumSetting.Builder<Dimension>()
        .name("dimension")
        .description("Which dimension the waypoint is in.")
        .defaultValue(Dimension.Overworld)
        .build()
    );

    public Setting<Boolean> oppositeSetting = sgPosition.add(new BoolSetting.Builder()
        .name("opposite-dimension")
        .description("Whether to show the waypoint in the opposite dimension.")
        .defaultValue(true)
        .visible(() -> dimensionSetting.get() != Dimension.End)
        .build()
    );

    private Waypoint() {}
    public Waypoint(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    public void renderIcon(double x, double y, double a, double size) {
        AbstractTexture texture = Waypoints.get().icons.get(iconSetting.get());
        if (texture == null) return;

        int preA = colorSetting.get().a;
        colorSetting.get().a *= a;

        GL.bindTexture(texture.getGlId());
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, size, size, colorSetting.get());
        Renderer2D.TEXTURE.render(null);

        colorSetting.get().a = preA;
    }

    public BlockPos getPos() {
        Dimension dim = dimensionSetting.get();
        BlockPos pos = posSetting.get();

        Dimension currentDim = PlayerUtils.getDimension();
        if (dim == currentDim || dim.equals(Dimension.End)) return posSetting.get();

        return switch (dim) {
            case Overworld -> new BlockPos(pos.getX() / 8, pos.getY(), pos.getZ() / 8);
            case Nether -> new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8);
            default -> null;
        };
    }

    private void validateIcon() {
        Map<String, AbstractTexture> icons = Waypoints.get().icons;

        AbstractTexture texture = icons.get(iconSetting.get());
        if (texture == null && !icons.isEmpty()) {
            iconSetting.set(icons.keySet().iterator().next());
        }
    }

    public static class Builder {
        protected String nameB = "", iconB = "";
        private BlockPos posB = BlockPos.ORIGIN;
        protected Dimension dimB = Dimension.Overworld;

        public Builder name(String name) {
            this.nameB = name;
            return this;
        }

        public Builder icon(String icon) {
            this.iconB = icon;
            return this;
        }

        public Builder pos(BlockPos pos) {
            this.posB = pos;
            return this;
        }

        public Builder dimension(Dimension dimension) {
            this.dimB = dimension;
            return this;
        }

        public Waypoint build() {
            Waypoint waypoint = new Waypoint();

            if (!nameB.equals(waypoint.nameSetting.getDefaultValue())) waypoint.nameSetting.set(nameB);
            if (!iconB.equals(waypoint.iconSetting.getDefaultValue())) waypoint.iconSetting.set(iconB);
            if (!posB.equals(waypoint.posSetting.getDefaultValue())) waypoint.posSetting.set(posB);
            if (!dimB.equals(waypoint.dimensionSetting.getDefaultValue())) waypoint.dimensionSetting.set(dimB);

            return waypoint;
        }
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public Waypoint fromTag(NbtCompound tag) {
        if (tag.contains("settings")) {
            settings.fromTag(tag.getCompound("settings"));
        }

        return this;
    }
}
