package minegame159.meteorclient.systems.modules.movement.strafe;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.movement.strafe.modes.Vanilla;
import minegame159.meteorclient.systems.modules.world.Timer;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class Strafe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<StrafeModes> strafeMode = sgGeneral.add(new EnumSetting.Builder<StrafeModes>()
            .name("mode")
            .description("The method of applying strafe.")
            .defaultValue(StrafeModes.Vanilla)
            .onModuleActivated(strafeModesSetting -> onStrafeModeChanged(strafeModesSetting.get()))
            .onChanged(this::onStrafeModeChanged)
            .build()
    );

    public final Setting<Double> vanillaStrafe = sgGeneral.add(new DoubleSetting.Builder()
            .name("Vanilla")
            .description("The speed in blocks per second.")
            .defaultValue(5.6)
            .min(0)
            .sliderMax(20)
            .visible(() -> strafeMode.get() == StrafeModes.Vanilla)
            .build()
    );

    private StrafeMode currentMode;

    public Strafe() {
        super(Categories.Movement, "Strafe", "Allows you to freely move in-air.");

        onStrafeModeChanged(strafeMode.get());
    }

    @Override
    public void onActivate() {
        currentMode.onActivate();
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        currentMode.onDeactivate();
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (event.type != MovementType.SELF || mc.player.isFallFlying() || mc.player.isClimbing() || mc.player.getVehicle() != null) return;

        currentMode.onMove(event);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player.isFallFlying() || mc.player.isClimbing() || mc.player.getVehicle() != null) return;

        currentMode.onTick();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) currentMode.onRubberband();
    }

    private void onStrafeModeChanged(StrafeModes mode) {
        switch (mode) {
            case Vanilla:   currentMode = new Vanilla(); break;
        }
    }

    @Override
    public String getInfoString() {
        return currentMode.getHudString();
    }
}