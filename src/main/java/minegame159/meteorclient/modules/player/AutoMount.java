package minegame159.meteorclient.modules.player;

//Created by squidooly 16/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

public class AutoMount extends ToggleModule {
    public AutoMount(){super(Category.Player, "auto-mount", "Mounts entities for you.");}

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> donkeys  = sgGeneral.add(new BoolSetting.Builder().name("donkey").description("DoNkE").defaultValue(false).build());
    private final Setting<Boolean> llamas  = sgGeneral.add(new BoolSetting.Builder().name("llama").description("Llama").defaultValue(false).build());
    private final Setting<Boolean> boats  = sgGeneral.add(new BoolSetting.Builder().name("boat").description("Boat").defaultValue(false).build());
    private final Setting<Boolean> minecarts  = sgGeneral.add(new BoolSetting.Builder().name("minecart").description("Minecart").defaultValue(false).build());
    private final Setting<Boolean> horses  = sgGeneral.add(new BoolSetting.Builder().name("horse").description("Horse").defaultValue(false).build());
    private final Setting<Boolean> pigs  = sgGeneral.add(new BoolSetting.Builder().name("pig").description("Pig").defaultValue(false).build());
    private final Setting<Boolean> mules  = sgGeneral.add(new BoolSetting.Builder().name("mule").description("Mule").defaultValue(false).build());
    private final Setting<Boolean> skeletons  = sgGeneral.add(new BoolSetting.Builder().name("skeleton-horse").description("Skeleton Horse").defaultValue(false).build());

    private final Setting<Boolean> checkSaddle = sgGeneral.add(new BoolSetting.Builder()
            .name("check-saddle")
            .description("Check if the entity has a saddle before mounting")
            .defaultValue(false)
            .build()
    );


    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if(mc.player.hasVehicle())return;
        for(Entity entity : mc.world.getEntities()){
            if(mc.player.distanceTo(entity) > 4) continue;
            if (donkeys.get() && entity instanceof DonkeyEntity && (!checkSaddle.get() || ((DonkeyEntity) entity).isSaddled())) {
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, mc.player.isSneaking()));
            } else if (llamas.get() && entity instanceof LlamaEntity) {
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, mc.player.isSneaking()));
            } else if (boats.get() && entity instanceof BoatEntity) {
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, mc.player.isSneaking()));
            } else if (minecarts.get() && entity instanceof MinecartEntity) {
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, mc.player.isSneaking()));
            } else if (horses.get() && entity instanceof HorseEntity && (!checkSaddle.get() || ((HorseEntity) entity).isSaddled())) {
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, mc.player.isSneaking()));
            } else if (pigs.get() && entity instanceof PigEntity && ((PigEntity) entity).isSaddled()) {
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, mc.player.isSneaking()));
            } else if (mules.get() && entity instanceof MuleEntity && (!checkSaddle.get() || ((MuleEntity) entity).isSaddled())) {
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, mc.player.isSneaking()));
            } else if (skeletons.get() && entity instanceof SkeletonHorseEntity && (!checkSaddle.get() || ((SkeletonHorseEntity) entity).isSaddled())) {
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, mc.player.isSneaking()));
            }
        }
    });
}
