package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.events.TookDamageEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;

public class AutoLog extends Module {
    private Setting<Integer> health = addSetting(new IntSetting.Builder()
            .name("health")
            .description("Disconnects when health is lower or equal to this value.")
            .defaultValue(6)
            .min(0)
            .max(20)
            .build()
    );

    private long lastLog = System.currentTimeMillis();
    private boolean shouldLog = false;

    public AutoLog() {
        super(Category.Combat, "auto-log", "Automatically disconnects when low on health.");
    }

    @EventHandler
    private Listener<TookDamageEvent> onTookDamage = new Listener<>(event -> {
        if (!shouldLog && event.entity.getUuid().equals(mc.player.getUuid()) && event.entity.getHealth() <= health.get()) {
            shouldLog = true;
            lastLog = System.currentTimeMillis();
        }
    });

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (shouldLog && System.currentTimeMillis() - lastLog <= 1000) {
            shouldLog = false;
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("AutoLog")));
        }
    });
}
