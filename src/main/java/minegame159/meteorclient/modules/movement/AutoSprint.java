package minegame159.meteorclient.modules.movement;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.utils.KeyBindings;

public class AutoSprint extends Module {
    public enum Mode {
        Always,
        Legit
    }

    private static EnumSetting<Mode> mode = new EnumSetting<>("mode", "Mode.", Mode.Always);

    public AutoSprint() {
        super(Category.Movement, "auto-sprint", "Automatically sprints.", mode);
    }

    @Override
    public void onDeactivate() {
        setSprinting(false);
    }

    private void setSprinting(boolean sprinting) {
        if (mode.value == Mode.Always) mc.player.setSprinting(sprinting);
        else KeyBindings.sprint.setPressed(sprinting);
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (mc.player.forwardSpeed > 0 && !mc.player.horizontalCollision && !mc.player.isSneaking()) setSprinting(true);
        else setSprinting(false);
    }
}
