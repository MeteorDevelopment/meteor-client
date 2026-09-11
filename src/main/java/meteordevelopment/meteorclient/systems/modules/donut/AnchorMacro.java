package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AnchorMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between each place, charge or explode.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> requireRightClick = sgGeneral.add(new BoolSetting.Builder()
        .name("require-right-click")
        .description("Only run while holding right click, so anchors never blow up by accident.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-place")
        .description("Place an anchor on the block you look at.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoCharge = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-charge")
        .description("Charge an empty anchor with glowstone.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoExplode = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-explode")
        .description("Detonate a charged anchor.")
        .defaultValue(true)
        .build()
    );

    private int timer;

    public AnchorMacro() {
        super(Categories.Donut, "anchor-macro", "Places, charges and detonates respawn anchors at your crosshair.");
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

        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (requireRightClick.get() && !mc.options.keyUse.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockState state = mc.level.getBlockState(hit.getBlockPos());

        if (state.getBlock() instanceof RespawnAnchorBlock) {
            if (state.getValue(RespawnAnchorBlock.CHARGE) == 0) {
                if (autoCharge.get()) useOn(InvUtils.findInHotbar(Items.GLOWSTONE), hit);
            } else if (autoExplode.get()) {
                // Interacting while holding glowstone adds charge instead of detonating.
                FindItemResult notGlowstone = InvUtils.findInHotbar(Items.TOTEM_OF_UNDYING);
                if (!notGlowstone.found()) notGlowstone = InvUtils.findInHotbar(stack -> stack.getItem() != Items.GLOWSTONE);
                useOn(notGlowstone, hit);
            }
        } else if (autoPlace.get()) {
            useOn(InvUtils.findInHotbar(Items.RESPAWN_ANCHOR), hit);
        }
    }

    private void useOn(FindItemResult item, BlockHitResult hit) {
        if (!item.found()) return;

        InvUtils.swap(item.slot(), true);
        BlockUtils.interact(hit, InteractionHand.MAIN_HAND, true);
        InvUtils.swapBack();

        timer = delay.get();
    }
}
