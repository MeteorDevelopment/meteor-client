package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;

public class AutoInvTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> priority = sgGeneral.add(new BoolSetting.Builder()
        .name("priority")
        .description("Replace whatever is in your offhand. Off only fills an empty offhand.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait after moving a totem before moving another.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private int timer;

    public AutoInvTotem() {
        super(Categories.Donut, "auto-inv-totem", "Keeps a totem in your offhand, refilling from your inventory.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (timer > 0) {
            timer--;
            return;
        }

        if (mc.player == null) return;
        if (InvUtils.testInOffHand(Items.TOTEM_OF_UNDYING)) return;
        if (!priority.get() && !mc.player.getOffhandItem().isEmpty()) return;

        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totem.found() || totem.isOffhand()) return;

        InvUtils.move().from(totem.slot()).toOffhand();
        timer = delay.get();
    }
}
