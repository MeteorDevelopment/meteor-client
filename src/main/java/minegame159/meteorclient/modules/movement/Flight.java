package minegame159.meteorclient.modules.movement;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.FloatSetting;

public class Flight extends Module {
    public enum Mode {
        Vanilla
    }

    private static EnumSetting<Mode> mode = new EnumSetting<>("mode", "Mode.", Mode.Vanilla);
    private static FloatSetting speed = new FloatSetting("speed", "Speed.", 0.1f, 0f, null);

    public Flight() {
        super(Category.Movement, "flight", "FLYYYY! You will take fall damage so enable no fall.", speed);
    }

    @Override
    public void onActivate() {
        if (mode.value == Mode.Vanilla) {
            mc.player.abilities.flying = true;
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = true;
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.value == Mode.Vanilla) {
            mc.player.abilities.flying = false;
            mc.player.abilities.setFlySpeed(0.05f);
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = false;
        }
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        switch (mode.value) {
            case Vanilla:
                mc.player.abilities.setFlySpeed(speed.value);
                mc.player.abilities.flying = true;
                if (mc.player.abilities.creativeMode) return;
                mc.player.abilities.allowFlying = true;
                break;
        }
    }
}
