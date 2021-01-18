package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AirPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> toggle = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle")
            .description("Toggles off after placing a block once.")
            .defaultValue(true)
            .build()
    );

    public AirPlace() {
        super(Category.Player, "air-place", "Places a block where your crosshair is pointing at.");
    }

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        assert mc.world != null;
        assert mc.player != null;
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockPos crossHairPos;

            crossHairPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            if (!mc.world.getBlockState(crossHairPos).isAir() || mc.player.getMainHandStack().getItem() == Items.AIR) return;
            PlayerUtils.placeBlock(crossHairPos, Hand.MAIN_HAND);
            if (toggle.get()) this.toggle();
        }
    });
}
