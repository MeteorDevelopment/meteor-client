package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class AntiItemDestroy extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delayTicks = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("How many ticks to block interaction.")
        .defaultValue(30)
        .min(0)
        .build()
    );

    private int blockTimer = 0;

    public AntiItemDestroy() {
        super(Categories.Combat, "anti-item-destroy", "Instant-cancel for crystals and anchors upon kill.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (blockTimer > 0) blockTimer--;

        // Backup check: If a player is 0 HP but packet was missed
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.getHealth() <= 0 && mc.player.distanceTo(player) < 10) {
                blockTimer = delayTicks.get();
            }
        }
    }

    // High Priority ensures this triggers BEFORE the game processes the death
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 3) { // Status 3 = Death
                Entity entity = packet.getEntity(mc.world);
                if (entity instanceof PlayerEntity && entity != mc.player) {
                    if (mc.player.distanceTo(entity) < 10) {
                        blockTimer = delayTicks.get();
                        // Optional: play a sound so you know it locked
                        // mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacket(PacketEvent.Send event) {
        if (blockTimer <= 0) return;

        // BLOCK PLACING (Right Click)
        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            var item = mc.player.getStackInHand(packet.getHand()).getItem();
            if (item == Items.END_CRYSTAL || item == Items.RESPAWN_ANCHOR || item == Items.GLOWSTONE) {
                event.cancel();
            }
        }

        // BLOCK BREAKING (Left Click / Attack)
        if (event.packet instanceof PlayerInteractEntityC2SPacket) {
            // We cancel ALL entity interactions while the timer is active.
            // This prevents you from "breaking" a crystal that was already there.
            event.cancel();
        }
    }
}