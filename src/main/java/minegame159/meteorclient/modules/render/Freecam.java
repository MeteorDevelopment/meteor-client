package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.DoubleSettingBuilder;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.packet.ClientCommandC2SPacket;
import net.minecraft.server.network.packet.PlayerInputC2SPacket;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class Freecam extends Module {
    private Setting<Double> speed = addSetting(new DoubleSettingBuilder()
            .name("speed")
            .description("Speed")
            .defaultValue(1.0)
            .min(0.0)
            .build()
    );

    private Entity preCameraEntity;
    private OtherClientPlayerEntity camera;
    private OtherClientPlayerEntity dummy;

    public Freecam() {
        super(Category.Render, "freecam", "You know what it does.");
    }

    @Override
    public void onActivate() {
        camera = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        camera.copyPositionAndRotation(mc.player);
        camera.horizontalCollision = false;
        camera.verticalCollision = false;

        dummy = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        dummy.copyPositionAndRotation(mc.player);
        dummy.setBoundingBox(dummy.getBoundingBox().expand(0.1));

        mc.world.addEntity(camera.getEntityId(), camera);
        mc.world.addEntity(dummy.getEntityId(), dummy);

        preCameraEntity = mc.cameraEntity;
        mc.cameraEntity = camera;
    }

    @Override
    public void onDeactivate() {
        mc.cameraEntity = preCameraEntity;

        mc.world.removeEntity(camera.getEntityId());
        mc.world.removeEntity(dummy.getEntityId());
    }

    @SubscribeEvent
    private void onPacketSend(SendPacketEvent e) {
        if (e.packet instanceof ClientCommandC2SPacket || e.packet instanceof PlayerMoveC2SPacket || e.packet instanceof PlayerInputC2SPacket) e.setCancelled(true);
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        camera.setVelocity(0, 0, 0);

        camera.yaw = mc.player.yaw;
        camera.headYaw = mc.player.headYaw;
        camera.bodyYaw = mc.player.bodyYaw;
        camera.elytraYaw = mc.player.elytraYaw;
        camera.pitch = mc.player.pitch;
        camera.elytraPitch = mc.player.elytraPitch;

        double speed = this.speed.value() / 2;
        Vec3d vel = camera.getVelocity();
        Vec3d forward = new Vec3d(0, 0, speed).rotateY(-(float) Math.toRadians(camera.headYaw));
        Vec3d strafe = forward.rotateY((float) Math.toRadians(90));

        if (mc.options.keyForward.isPressed()) vel = vel.add(forward.x, 0, forward.z);
        if (mc.options.keyBack.isPressed()) vel = vel.subtract(forward.x, 0, forward.z);
        if (mc.options.keyLeft.isPressed()) vel = vel.add(strafe.x, 0, strafe.z);
        if (mc.options.keyRight.isPressed()) vel = vel.subtract(strafe.x, 0, strafe.z);
        if (mc.options.keyJump.isPressed()) vel = vel.add(0, speed, 0);
        if (mc.options.keySneak.isPressed()) vel = vel.subtract(0, speed, 0);

        camera.setPos(camera.getX() + vel.x, camera.getY() + vel.y, camera.getZ() + vel.z);
    }
}
