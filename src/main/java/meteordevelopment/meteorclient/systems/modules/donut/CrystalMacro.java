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
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class CrystalMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Ticks between crystal placements.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("break-delay")
        .description("Ticks between crystal breaks.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> requireRightClick = sgGeneral.add(new BoolSetting.Builder()
        .name("require-right-click")
        .description("Only run while holding right click.")
        .defaultValue(true)
        .build()
    );

    private int placeTimer;
    private int breakTimer;

    public CrystalMacro() {
        super(Categories.Donut, "crystal-macro", "Places and breaks end crystals at your crosshair.");
    }

    @Override
    public void onActivate() {
        placeTimer = 0;
        breakTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (placeTimer > 0) placeTimer--;
        if (breakTimer > 0) breakTimer--;

        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (requireRightClick.get() && !mc.options.keyUse.isDown()) return;

        HitResult target = mc.hitResult;

        if (target instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof EndCrystal crystal) {
            if (breakTimer > 0) return;

            mc.gameMode.attack(mc.player, crystal);
            mc.player.swing(InteractionHand.MAIN_HAND);
            breakTimer = breakDelay.get();
            return;
        }

        if (target instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK && placeTimer <= 0) {
            Block base = mc.level.getBlockState(blockHit.getBlockPos()).getBlock();
            if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) return;
            if (!mc.level.getBlockState(blockHit.getBlockPos().above()).isAir()) return;

            FindItemResult crystalItem = InvUtils.findInHotbar(Items.END_CRYSTAL);
            if (!crystalItem.found()) return;

            InvUtils.swap(crystalItem.slot(), true);
            BlockUtils.interact(blockHit, InteractionHand.MAIN_HAND, true);
            InvUtils.swapBack();

            placeTimer = placeDelay.get();
        }
    }
}
