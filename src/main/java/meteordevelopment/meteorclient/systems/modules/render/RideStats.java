/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.LlamaEntity;


public class RideStats extends Module {
    final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select entities to display on.")
        .filter(this::entityFilter)
        .defaultValue(EntityType.HORSE, EntityType.MULE, EntityType.DONKEY, EntityType.LLAMA, EntityType.SKELETON_HORSE)
        .build()
    );

    private final Setting<Boolean> displaySpeed = sgGeneral.add(new BoolSetting.Builder()
        .name("display-max-speed")
        .description("Display the entity's max speed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> displayJumpHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("display-max-jump-height")
        .description("Display the entity's max jump height.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> displayHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("display-max-health")
        .description("Display the entity's max health.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> displayInventorySlots = sgGeneral.add(new BoolSetting.Builder()
        .name("display-llama-slots")
        .description("Display the llama's inventory slots.")
        .defaultValue(true)
        .visible(()-> entities.get().containsKey(EntityType.LLAMA))
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the nametag.")
        .defaultValue(1.5)
        .min(0.1)
        .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("How high above entities' heads to display it.")
        .defaultValue(1)
        .sliderMax(3)
        .build()
    );

    private final Setting<SettingColor> entityNameColor = sgGeneral.add(new ColorSetting.Builder()
        .name("name-color")
        .description("What color the entity's name is.")
        .defaultValue(new SettingColor())
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("What color the background of the nametag is.")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build()
    );


    public RideStats() {
        super(Categories.Render, "ride-stats", "Displays information above rideable entity's heads");
    }

    private final Vec3 pos = new Vec3();
    private final Color GREEN = new Color(25, 252, 25);
    private final Color GREY = new Color(150, 150, 150);
    private final Color BLUE = new Color(20, 170, 170);
    private final Color GOLD = new Color(232, 185, 35);

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for(Entity entity: mc.world.getEntities()) {
            if(!entities.get().getBoolean(entity.getType())) continue;
            pos.set(entity, event.tickDelta);
            pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.75, 0);
            pos.add(0, -1 + height.get(), 0);
            if (NametagUtils.to2D(pos, scale.get())) {
                renderHorseNametag(entity);
            }
        }
    }

    private void renderHorseNametag(Entity entity) {
        AbstractHorseEntity horseEntity = (AbstractHorseEntity) entity;
        boolean llama = entity.getType() == EntityType.LLAMA;
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);
        text.beginBig();

        // Name
        String name;
        name = horseEntity.getType().getName().getString();

        // Health
        double health = horseEntity.getMaxHealth();
        String healthText = " " + String.format("%.1f", health).replace(".", ",");

        // Speed
        double speed = genericSpeedToBlockPerSecond(horseEntity.getAttributes().getBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        String speedText = " " + String.format("%.1f", speed).replace(".", ",") + " bps";

        // Jump
        double maxJump = jumpStrengthToJumpHeight(horseEntity.getJumpStrength());
        String maxJumpText = " " + String.format("%.1f", maxJump).replace(".", ",") + "m";

        //Inv Slots
        int invSlots = 0;
        if(llama) {
            invSlots = ((LlamaEntity) entity).getInventoryColumns() * 3;
        }
        String invSlotsText = " " + invSlots + " slots";

        // Widths
        double nameWidth = text.getWidth(name, true);
        double healthWidth = text.getWidth(healthText, true);
        double speedWidth = text.getWidth(speedText, true);
        double jumpWidth = text.getWidth(maxJumpText, true);
        double invSlotsWidth = text.getWidth(invSlotsText, true);
        double width = nameWidth;

        if (displayHealth.get()) width += healthWidth;
        if (displaySpeed.get()) width += speedWidth;
        if (displayJumpHeight.get()) width += jumpWidth;
        if(displayInventorySlots.get() && llama) width += invSlotsWidth;

        double widthHalf = width / 2;
        double heightDown = text.getHeight(true);

        drawBg(-widthHalf, -heightDown, width, heightDown);

        // Render texts
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(name, hX, hY, entityNameColor.get(), true);

        if(displayHealth.get()) hX = text.render(healthText, hX, hY, GREEN, true);
        if (displaySpeed.get()) hX = text.render(speedText, hX, hY, BLUE, true);
        if (displayJumpHeight.get() && !llama) text.render(maxJumpText, hX, hY, GREY, true);
        if(displayInventorySlots.get() && llama) text.render(invSlotsText, hX, hY, GOLD, true);
        text.end();
        NametagUtils.end();
    }

    private double jumpStrengthToJumpHeight(double strength) {
        // Don't Judge me, I forgot where this came from, and it works
        return -0.1817584952 * strength * strength * strength + 3.689713992 * strength * strength + 2.128599134 * strength - 0.343930367;
    }

    private double genericSpeedToBlockPerSecond(double speed) {
        return 0.132 * speed * speed + 42.119 * speed;
    }

    private void drawBg(double x, double y, double width, double height) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, backgroundColor.get());
        Renderer2D.COLOR.render(null);
    }

    private Boolean entityFilter(EntityType<?> entity) {
        return EntityType.HORSE.equals(entity) || EntityType.MULE.equals(entity) || EntityType.DONKEY.equals(entity) || EntityType.LLAMA.equals(entity) || EntityType.SKELETON_HORSE.equals(entity);
    }

}
