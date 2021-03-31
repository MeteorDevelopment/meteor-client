/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.world;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.render.SkyProperties;
import net.minecraft.util.math.Vec3d;

/**
 * @author Walaryne
 */
public class Ambience extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDynamic = settings.createGroup("Dynamic");
    private final SettingGroup sgStatic = settings.createGroup("Static");

    // GENERAL

    public final Setting<Boolean> enderMode = sgGeneral.add(new BoolSetting.Builder()
            .name("ender-mode")
            .description("Makes the sky like the vast void of the End.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> enderCustomSkyColor = sgGeneral.add(new BoolSetting.Builder()
            .name("ender-custom-color")
            .description("Allows a custom sky color for Ender Mode.")
            .defaultValue(false)
            .build()
    );

    // DYNAMIC

    public final Setting<Boolean> changeSkyColor = sgDynamic.add(new BoolSetting.Builder()
            .name("change-sky-color")
            .description("Should the sky color be changed.")
            .defaultValue(false)
            .build()
    );

    public final Setting<SettingColor> skyColor = sgDynamic.add(new ColorSetting.Builder()
            .name("sky-color")
            .description("The color to change the sky to.")
            .defaultValue(new SettingColor(102, 0, 0, 255))
            .build()
    );

    public final Setting<SettingColor> endSkyColor = sgDynamic.add(new ColorSetting.Builder()
            .name("end-sky-color")
            .description("The color to change the End sky to.")
            .defaultValue(new SettingColor(102, 0, 0, 255))
            .build()
    );

    public final Setting<Boolean> changeCloudColor = sgDynamic.add(new BoolSetting.Builder()
            .name("change-cloud-color")
            .description("Should the color of the clouds be changed.")
            .defaultValue(false)
            .build()
    );

    public final Setting<SettingColor> cloudColor = sgDynamic.add(new ColorSetting.Builder()
            .name("clouds-color")
            .description("The color to change the clouds to.")
            .defaultValue(new SettingColor(102, 0, 0, 255))
            .build()
    );

    public final Setting<Boolean> changeLightningColor = sgDynamic.add(new BoolSetting.Builder()
            .name("change-lightning-color")
            .description("Should the color of lightning be changed.")
            .defaultValue(false)
            .build()
    );

    public final Setting<SettingColor> lightningColor = sgDynamic.add(new ColorSetting.Builder()
            .name("lightning-color")
            .description("The color to change lightning to.")
            .defaultValue(new SettingColor(102, 0, 0, 255))
            .build()
    );

    // STATIC

    public final Setting<Boolean> changeWaterColor = sgStatic.add(new BoolSetting.Builder()
            .name("change-water-color")
            .description("Should the color of water be changed.")
            .defaultValue(false)
            .build()
    );

    public final Setting<SettingColor> waterColor = sgStatic.add(new ColorSetting.Builder()
            .name("water-color")
            .description("The color to change water to.")
            .defaultValue(new SettingColor(102, 0, 0, 255))
            .build()
    );

    public final Setting<Boolean> changeLavaColor = sgStatic.add(new BoolSetting.Builder()
            .name("change-lava-color")
            .description("Should the color of lava be changed.")
            .defaultValue(false)
            .build()
    );

    public final Setting<SettingColor> lavaColor = sgStatic.add(new ColorSetting.Builder()
            .name("lava-color")
            .description("The color to change lava to.")
            .defaultValue(new SettingColor(102, 0, 0, 255))
            .build()
    );

    public final Setting<Boolean> changeFoliageColor = sgStatic.add(new BoolSetting.Builder()
            .name("change-foliage-color")
            .description("Should the color of the foliage be changed.")
            .defaultValue(false)
            .build()
    );

    public final Setting<SettingColor> foliageColor = sgStatic.add(new ColorSetting.Builder()
            .name("foliage-color")
            .description("The color to change the foliage to.")
            .defaultValue(new SettingColor(102, 0, 0, 255))
            .build()
    );

    public final Setting<Boolean> changeGrassColor = sgStatic.add(new BoolSetting.Builder()
            .name("change-grass-color")
            .description("Should the color of grass be changed.")
            .defaultValue(false)
            .build()
    );

    public final Setting<SettingColor> grassColor = sgStatic.add(new ColorSetting.Builder()
            .name("grass-color")
            .description("The color to change grass to.")
            .defaultValue(new SettingColor(102, 0, 0, 255))
            .build()
    );

    public Ambience() {
        super(Categories.World, "ambience", "Change the color of various pieces of the environment.");
    }

    @Override
    public void onActivate() {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();

        WButton reloadWorld = list.add(theme.button("Reload World")).expandX().widget();
        reloadWorld.action = () -> {
            if (mc.worldRenderer != null) mc.worldRenderer.reload();
        };

        return list;
    }

    public static class Custom extends SkyProperties {
        public Custom() {
            super(Float.NaN, true, SkyProperties.SkyType.END, true, false);
        }

        @Override
        public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
            return color.multiply(0.15000000596046448D);
        }

        @Override
        public boolean useThickFog(int camX, int camY) {
            return false;
        }

        @Override
        public float[] getFogColorOverride(float skyAngle, float tickDelta) {
            return null;
        }
    }
}
