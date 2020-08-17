package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IPlayerEntity;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class Freecam extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed")
            .defaultValue(1.0)
            .min(0.0)
            .build()
    );

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
        ((IPlayerEntity) camera).setInventory(mc.player.inventory);

        dummy = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        dummy.copyPositionAndRotation(mc.player);
        dummy.setBoundingBox(dummy.getBoundingBox().expand(0.1));
        ((IPlayerEntity) dummy).setInventory(mc.player.inventory);

        mc.world.addEntity(camera.getEntityId(), camera);
        mc.world.addEntity(dummy.getEntityId(), dummy);

        mc.cameraEntity = camera;
    }

    @Override
    public void onDeactivate() {
        if (mc.world == null) {
            return;
        }
        mc.cameraEntity = mc.player;

        mc.world.removeEntity(camera.getEntityId());
        mc.world.removeEntity(dummy.getEntityId());
    }

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (event.packet instanceof ClientCommandC2SPacket || event.packet instanceof PlayerMoveC2SPacket || event.packet instanceof PlayerInputC2SPacket) event.cancel();
    });

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.player.deathTime > 0 || mc.player.getHealth() <= 0) {
            toggle();
            return;
        }

        camera.setVelocity(0, 0, 0);

        camera.yaw = mc.player.yaw;
        camera.headYaw = mc.player.headYaw;
        camera.elytraYaw = mc.player.elytraYaw;
        camera.pitch = mc.player.pitch;
        camera.elytraPitch = mc.player.elytraPitch;
        camera.setHealth(mc.player.getHealth());
        camera.setAbsorptionAmount(mc.player.getAbsorptionAmount());
        camera.getActiveStatusEffects().putAll(mc.player.getActiveStatusEffects());

        double speed = this.speed.get() / 2;
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
    });
}
