package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;

public class Step extends ToggleModule {
    private Setting<Double> height = addSetting(new DoubleSetting.Builder()
            .name("height")
            .description("Step height.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private float prevStepHeight;

    public Step() {
        super(Category.Movement, "step", "Allows you to step up full blocks.");
    }

    @Override
    public void onActivate() {
        prevStepHeight = mc.player.stepHeight;
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        mc.player.stepHeight = height.get().floatValue();
    });

    @Override
    public void onDeactivate() {
        mc.player.stepHeight = prevStepHeight;
    }
}
