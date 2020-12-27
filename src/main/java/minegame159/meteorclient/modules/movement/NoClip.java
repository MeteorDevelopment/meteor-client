package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PreTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class NoClip extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("distance")
            .description("the distance per clip")
            .defaultValue(0.01D)
            .min(0.0D)
            .max(1.0D)
            .build());

    public NoClip() {
        super(Category.Movement, "NoClip", "lets you walk through blocks");
    }

    @EventHandler
    private final Listener<PreTickEvent> onTick = new Listener<>(event -> {
        if (!this.mc.player.isOnGround()) return;

        if (mc.options.keyForward.isPressed()) {
            net.minecraft.client.network.ClientPlayerEntity player = (net.minecraft.client.MinecraftClient.getInstance()).player;
            net.minecraft.util.math.Vec3d forward = net.minecraft.util.math.Vec3d.fromPolar(0.0F, player.yaw);
            player.updatePosition(player.getX() + forward.x * distance.get(), player.getY(), player.getZ() + forward.z * distance.get());
        }
        if (this.mc.options.keyBack.isPressed()) {
            net.minecraft.client.network.ClientPlayerEntity player = (net.minecraft.client.MinecraftClient.getInstance()).player;
            net.minecraft.util.math.Vec3d forward = net.minecraft.util.math.Vec3d.fromPolar(0.0F, player.yaw - 180.0F);
            player.updatePosition(player.getX() + forward.x * distance.get(), player.getY(), player.getZ() + forward.z * distance.get());
        }
        if (this.mc.options.keyLeft.isPressed()) {
            net.minecraft.client.network.ClientPlayerEntity player = (net.minecraft.client.MinecraftClient.getInstance()).player;
            net.minecraft.util.math.Vec3d forward = net.minecraft.util.math.Vec3d.fromPolar(0.0F, player.yaw - 90.0F);
            player.updatePosition(player.getX() + forward.x * distance.get(), player.getY(), player.getZ() + forward.z * distance.get());
        }
        if (this.mc.options.keyRight.isPressed()) {
            net.minecraft.client.network.ClientPlayerEntity player = (net.minecraft.client.MinecraftClient.getInstance()).player;
            net.minecraft.util.math.Vec3d forward = net.minecraft.util.math.Vec3d.fromPolar(0.0F, player.yaw - 270.0F);
            player.updatePosition(player.getX() + forward.x * distance.get(), player.getY(), player.getZ() + forward.z * distance.get());
        }
        if (mc.options.keyJump.isPressed()) {
            net.minecraft.client.network.ClientPlayerEntity player = (net.minecraft.client.MinecraftClient.getInstance()).player;
            player.updatePosition(player.getX(), player.getY() + 0.05D, player.getZ());
        }
        if (mc.options.keySneak.isPressed()) {
            net.minecraft.client.network.ClientPlayerEntity player = (net.minecraft.client.MinecraftClient.getInstance()).player;
            player.updatePosition(player.getX(), player.getY() - 0.05D, player.getZ());
        }
    });
}
