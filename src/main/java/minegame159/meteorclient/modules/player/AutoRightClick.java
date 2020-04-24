package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;

public class AutoRightClick extends ToggleModule {
    private Setting<Integer> delay = addSetting(new IntSetting.Builder()
            .name("delay")
            .description("Delay between clicks in ticks.")
            .defaultValue(2)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private Setting<Boolean> onlyWhenHoldingUse = addSetting(new BoolSetting.Builder()
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

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.player.getHealth() <= 0) return;

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
