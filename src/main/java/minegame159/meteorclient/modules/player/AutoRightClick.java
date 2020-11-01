package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;

public class AutoRightClick extends ToggleModule {
    public enum Mode{
        Hold,
        Press
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("How it right clicks.")
            .defaultValue(Mode.Press)
            .build()
    );
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between clicks in ticks.")
            .defaultValue(2)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private final Setting<Boolean> onlyWhenHoldingUse = sgGeneral.add(new BoolSetting.Builder()
            .name("only-when-holding-use")
            .description("Only when holding right click.")
            .defaultValue(false)
            .build()
    );

    private int timer;

    public AutoRightClick() {
        super(Category.Player, "auto-right-click", "Automatically right clicks.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Hold && mc.options.keyUse.isPressed()) {
            ((IKeyBinding)mc.options.keyUse).setPressed(false);
        }
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mc.player.getHealth() <= 0) return;
        if (mode.get() == Mode.Hold && !mc.options.keyUse.isPressed()) {
            ((IKeyBinding)mc.options.keyUse).setPressed(true);
            return;
        } else if (mode.get() == Mode.Hold) return;

        timer++;

        if (timer > delay.get()) {
            if (onlyWhenHoldingUse.get()) {
                if (mc.options.keyAttack.isPressed()) Utils.rightClick();
            } else {
                Utils.rightClick();
            }

            timer = 0;
        }
    });
}
