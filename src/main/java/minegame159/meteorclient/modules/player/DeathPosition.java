package minegame159.meteorclient.modules.player;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DeathPosition extends Module {
    private boolean isDead;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public DeathPosition() {
        super(Category.Player, "death-position", "Sends to your chat where you died.");
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (mc.player.getHealth() <= 0 && !isDead) {
            Utils.sendMessage("#yellowDied at #blue%.1f#yellow, #blue%.1f#yellow, #blue%.1f#yellow on #blue%s.", mc.player.getX(), mc.player.getY(), mc.player.getZ(), dateFormat.format(new Date()));
            isDead = true;
        }
        else if (mc.player.getHealth() > 0 && isDead) {
            isDead = false;
        }
    }
}
