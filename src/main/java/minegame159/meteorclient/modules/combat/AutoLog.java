package minegame159.meteorclient.modules.combat;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.events.TookDamageEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.IntSettingBuilder;
import net.minecraft.client.network.packet.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;

public class AutoLog extends Module {
    private Setting<Integer> health = addSetting(new IntSettingBuilder()
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

    @SubscribeEvent
    private void onTookDamage(TookDamageEvent e) {
        if (!shouldLog && e.entity.getUuid().equals(mc.player.getUuid()) && e.entity.getHealth() <= health.value()) {
            shouldLog = true;
            lastLog = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (shouldLog && System.currentTimeMillis() - lastLog <= 1000) {
            shouldLog = false;
            MeteorClient.saveConfig();
            ModuleManager.deactivateAll();
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("AutoLog")));
        }
    }
}
