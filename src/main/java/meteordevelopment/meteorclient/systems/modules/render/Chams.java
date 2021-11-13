/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.EntityShaders;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

public class Chams extends Module {
    private final SettingGroup sgThroughWalls = settings.createGroup("Through Walls");
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");
    private final SettingGroup sgHand = settings.createGroup("Hand");

    // Through walls

    public final Setting<Object2BooleanMap<EntityType<?>>> entities = sgThroughWalls.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select entities to show through walls.")
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    public final Setting<Shader> shader = sgThroughWalls.add(new EnumSetting.Builder<Shader>()
        .name("shader")
        .description("Renders a shader instead of the entities.")
        .defaultValue(Shader.Liquid1)
        .onModuleActivated(shaderSetting -> {
            if (shaderSetting.get() != Shader.None) EntityShaders.initOverlay(shaderSetting.get().shaderName);
        })
        .onChanged(value -> {
            if (value != Shader.None) EntityShaders.initOverlay(value.shaderName);
        })
        .build()
    );

    public final Setting<Boolean> ignoreSelfDepth = sgThroughWalls.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores yourself drawing the player.")
        .defaultValue(true)
        .build()
    );

    //Players

    public final Setting<Boolean> players = sgPlayers.add(new BoolSetting.Builder()
        .name("players")
        .description("Enables model tweaks for players.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> ignoreSelf = sgPlayers.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores yourself when tweaking player models.")
        .defaultValue(true)
        .visible(players::get)
        .build()
    );

    public final Setting<Double> playersScale = sgPlayers.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Players scale.")
        .defaultValue(1.0)
        .min(0.0)
        .visible(players::get)
        .build()
    );

    public final Setting<Boolean> playersTexture = sgPlayers.add(new BoolSetting.Builder()
        .name("texture")
        .description("Enables player model textures.")
        .defaultValue(false)
        .visible(players::get)
        .build()
    );

    public final Setting<SettingColor> playersColor = sgPlayers.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of player models.")
        .defaultValue(new SettingColor(0, 255, 255, 100))
        .visible(players::get)
        .build()
    );

    //Crystals

    public final Setting<Boolean> crystals = sgCrystals.add(new BoolSetting.Builder()
        .name("crystals")
        .description("Enables model tweaks for end crystals.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> crystalsScale = sgCrystals.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Crystal scale.")
        .defaultValue(0.6)
        .min(0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Double> crystalsBounce = sgCrystals.add(new DoubleSetting.Builder()
        .name("bounce")
        .description("How high crystals bounce.")
        .defaultValue(0.3)
        .min(0.0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Double> crystalsRotationSpeed = sgCrystals.add(new DoubleSetting.Builder()
        .name("rotation-speed")
        .description("Multiplies the roation speed of the crystal.")
        .defaultValue(3)
        .min(0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Boolean> crystalsTexture = sgCrystals.add(new BoolSetting.Builder()
        .name("texture")
        .description("Whether to render crystal model textures.")
        .defaultValue(false)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Boolean> renderCore = sgCrystals.add(new BoolSetting.Builder()
        .name("render-core")
        .description("Enables rendering of the core of the crystal.")
        .defaultValue(false)
        .visible(crystals::get)
        .build()
    );

    public final Setting<SettingColor> crystalsCoreColor = sgCrystals.add(new ColorSetting.Builder()
        .name("core-color")
        .description("The color of end crystal models.")
        .defaultValue(new SettingColor(0, 255, 255, 100))
        .visible(renderCore::get)
        .build()
    );

    public final Setting<Boolean> renderFrame1 = sgCrystals.add(new BoolSetting.Builder()
        .name("render-inner-frame")
        .description("Enables rendering of the frame of the crystal.")
        .defaultValue(true)
        .visible(crystals::get)
        .build()
    );

    public final Setting<SettingColor> crystalsFrame1Color = sgCrystals.add(new ColorSetting.Builder()
        .name("inner-frame-color")
        .description("The color of end crystal models.")
        .defaultValue(new SettingColor(0, 255, 255, 100))
        .visible(renderFrame1::get)
        .build()
    );

    public final Setting<Boolean> renderFrame2 = sgCrystals.add(new BoolSetting.Builder()
        .name("render-outer-frame")
        .description("Enables rendering of the frame of the crystal.")
        .defaultValue(true)
        .visible(crystals::get)
        .build()
    );

    public final Setting<SettingColor> crystalsFrame2Color = sgCrystals.add(new ColorSetting.Builder()
        .name("outer-frame-color")
        .description("The color of end crystal models.")
        .defaultValue(new SettingColor(0, 255, 255, 100))
        .visible(renderFrame2::get)
        .build()
    );

    // Hand

    public final Setting<Boolean> hand = sgHand.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Enables tweaks of hand rendering.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> handTexture = sgHand.add(new BoolSetting.Builder()
        .name("texture")
        .description("Whether to render hand textures.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> handColor = sgHand.add(new ColorSetting.Builder()
        .name("hand-color")
        .description("The color of your hand.")
        .defaultValue(new SettingColor(0, 255, 255, 100))
        .build()
    );

    public static final Identifier BLANK = new Identifier("meteor-client", "textures/blank.png");

    public Chams() {
        super(Categories.Render, "chams", "Tweaks rendering of entities.");
    }

    public boolean shouldRender(Entity entity) {
        return isActive() && !isShader() && entities.get().getBoolean(entity.getType()) && (entity != mc.player || ignoreSelfDepth.get());
    }

    public boolean isShader() {
        return isActive() && shader.get() != Shader.None;
    }

    public enum Shader {
        Liquid1("liquid"),
        None(null);

        public final String shaderName;

        Shader(String shaderName) {
            this.shaderName = shaderName;
        }
    }
}
