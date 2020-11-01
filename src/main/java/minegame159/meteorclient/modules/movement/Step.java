package minegame159.meteorclient.modules.movement;

import baritone.api.BaritoneAPI;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class Step extends ToggleModule {
    public enum ActiveWhen {
        Always,
        Sneaking,
        NotSneaking
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("Step height.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<ActiveWhen> activeWhen = sgGeneral.add(new EnumSetting.Builder<ActiveWhen>()
            .name("active-when")
            .description("Step active when.")
            .defaultValue(ActiveWhen.Always)
            .build()
    );

    private float prevStepHeight;
    private boolean prevBaritoneAssumeStep;

    public Step() {
        super(Category.Movement, "step", "Allows you to step up full blocks.");
    }

    @Override
    public void onActivate() {
        prevStepHeight = mc.player.stepHeight;

        prevBaritoneAssumeStep = BaritoneAPI.getSettings().assumeStep.value;
        BaritoneAPI.getSettings().assumeStep.value = true;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        boolean work = (activeWhen.get() == ActiveWhen.Always) || (activeWhen.get() == ActiveWhen.Sneaking && mc.player.isSneaking()) || (activeWhen.get() == ActiveWhen.NotSneaking && !mc.player.isSneaking());

        if (work) mc.player.stepHeight = height.get().floatValue();
        else mc.player.stepHeight = prevStepHeight;
    });

    @Override
    public void onDeactivate() {
        mc.player.stepHeight = prevStepHeight;

        BaritoneAPI.getSettings().assumeStep.value = prevBaritoneAssumeStep;
    }
}
