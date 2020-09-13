package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class Step extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("Step height.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Boolean> notWhenSneaking = sgGeneral.add(new BoolSetting.Builder()
            .name("not-when-sneaking")
            .description("Option for Step to not work when sneaking.")
            .defaultValue(true)
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
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        boolean work = true;
        if (notWhenSneaking.get() && mc.player.isSneaking()) work = false;

        if (work) mc.player.stepHeight = height.get().floatValue();
        else mc.player.stepHeight = prevStepHeight;
    });

    @Override
    public void onDeactivate() {
        mc.player.stepHeight = prevStepHeight;
    }
}
