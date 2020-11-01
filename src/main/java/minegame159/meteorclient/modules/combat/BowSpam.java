package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.item.Items;

public class BowSpam extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> charge = sgGeneral.add(new IntSetting.Builder()
            .name("charge")
            .description("How much to charge the bow in ticks. (0-20)")
            .defaultValue(5)
            .min(5)
            .max(20)
            .sliderMin(5)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> onlyWhenHoldingRightClick = sgGeneral.add(new BoolSetting.Builder()
            .name("only-when-holding-right-click")
            .description("Works only when holding right click.")
            .defaultValue(false)
            .build()
    );

    private boolean wasBow = false;
    private boolean wasHoldingRightClick = false;

    public BowSpam() {
        super(Category.Combat, "bow-spam", "Spams arrows.");
    }

    @Override
    public void onActivate() {
        wasBow = false;
        wasHoldingRightClick = false;
    }

    @Override
    public void onDeactivate() {
        setPressed(false);
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (!onlyWhenHoldingRightClick.get() || mc.options.keyUse.isPressed()) {
            boolean isBow = mc.player.getMainHandStack().getItem() == Items.BOW;
            if (!isBow && wasBow) setPressed(false);

            wasBow = isBow;
            if (!isBow) return;

            if (mc.player.getItemUseTime() >= charge.get()) {
                mc.player.stopUsingItem();
                mc.interactionManager.stopUsingItem(mc.player);
            } else {
                setPressed(true);
            }

            wasHoldingRightClick = mc.options.keyUse.isPressed();
        } else {
            if (wasHoldingRightClick) {
                setPressed(false);
                wasHoldingRightClick = false;
            }
        }
    });

    private void setPressed(boolean pressed) {
        ((IKeyBinding) mc.options.keyUse).setPressed(pressed);
    }
}
