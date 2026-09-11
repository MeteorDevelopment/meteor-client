package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerDetection extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> notifyChat = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-chat")
        .description("Send notification in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notifySound = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-sound")
        .description("Play sound on detection.")
        .defaultValue(true)
        .build()
    );

    private final Set<UUID> detectedPlayers = new HashSet<>();

    public PlayerDetection() {
        super(Categories.Donut, "player-detection", "Detects nearby players.");
    }

    @Override
    public void onActivate() {
        detectedPlayers.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null) return;

        Set<UUID> present = new HashSet<>();

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            present.add(player.getUUID());

            if (detectedPlayers.add(player.getUUID())) {
                if (notifyChat.get()) {
                    info("Player detected: %s at %.0f blocks away", player.getName().getString(), mc.player.distanceTo(player));
                }
                if (notifySound.get()) {
                    mc.level.playSound(mc.player, mc.player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.AMBIENT, 3.0F, 1.0F);
                }
            }
        }

        // Forget players who left render distance, so they alert again if they come back.
        detectedPlayers.retainAll(present);
    }
}
