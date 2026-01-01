package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;

public class LegitTrap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("放置距离 (Hoplite建议 3.5 - 3.8)")
        .defaultValue(3.8)
        .min(0)
        .max(6)
        .build()
    );

    // 核心防回弹设置：先瞄准，等几tick，再放置
    private final Setting<Integer> aimDelay = sgGeneral.add(new IntSetting.Builder()
        .name("aim-delay")
        .description("瞄准后的等待时间(Tick)。200ms Ping 建议设为 3 或 4。")
        .defaultValue(3)
        .min(1)
        .max(10)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("放置后的冷却时间(Tick)")
        .defaultValue(5)
        .min(0)
        .max(20)
        .build()
    );

    private final Setting<Boolean> smartWeb = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-web-save")
        .description("敌人脚下已有网时绝不放第二个")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSwitchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-back")
        .description("松开按键时自动切回原物品")
        .defaultValue(true)
        .build()
    );

    private int timer = 0;        // 放置冷却
    private int aimTimer = 0;     // 瞄准稳定计时器
    private BlockPos lastTarget = null; // 上一帧尝试放置的位置
    private int originalSlot = -1; // 记录最开始手持的武器

    public LegitTrap() {
        super(Categories.Combat, "legit-trap", "Hoplite专用 - 智能连招防回弹");
    }

    @Override
    public void onActivate() {
        timer = 0;
        aimTimer = 0;
        lastTarget = null;
        if (mc.player != null) {
            // 记录当前手持物品（通常是剑），以便松开按键时切回
            originalSlot = -1;
            var mainHandStack = mc.player.getMainHandStack();
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i) == mainHandStack) {
                    originalSlot = i;
                    break;
                }
            }
            if (originalSlot == -1) originalSlot = 0;
        }
    }

    @Override
    public void onDeactivate() {
        // 松开按键时，切回武器
        if (autoSwitchBack.get() && originalSlot != -1 && mc.player != null) {
            InvUtils.swap(originalSlot, false);
        }
        originalSlot = -1;
        lastTarget = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 放置冷却中，什么都不做
        if (timer > 0) {
            timer--;
            return;
        }

        // 1. 寻找最近的敌人
        PlayerEntity target = mc.world.getPlayers().stream()
            .filter(p -> p != mc.player)
            .filter(p -> !Friends.get().isFriend(p))
            .filter(p -> !p.isDead())
            .filter(p -> mc.player.distanceTo(p) <= range.get())
            .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
            .orElse(null);

        // 如果没有目标，重置瞄准状态
        if (target == null) {
            aimTimer = 0;
            lastTarget = null;
            return;
        }

        // 2. 确定目标位置（敌人的脚和头）
        BlockPos feetPos = target.getBlockPos(); // 敌人的脚下
        BlockPos headPos = feetPos.up();         // 敌人的头顶/上半身

        // 3. 检查背包资源
        int webSlot = InvUtils.findInHotbar(Items.COBWEB).slot();
        int lavaSlot = InvUtils.findInHotbar(Items.LAVA_BUCKET).slot();

        // 4. 决策逻辑 (状态机)
        boolean needWeb = shouldPlaceWeb(feetPos);
        boolean needLava = shouldPlaceLava(headPos);

        BlockPos targetPos = null;
        int targetSlot = -1;

        // 优先级逻辑：
        // 如果脚下能放网 且 我有网 -> 决定放网
        if (needWeb && webSlot != -1) {
            targetPos = feetPos;
            targetSlot = webSlot;
        } 
        // 否则（脚下已经有网了 或 我没网了），如果头顶能放岩浆 且 我有岩浆 -> 决定放岩浆
        else if (needLava && lavaSlot != -1) {
            targetPos = headPos;
            targetSlot = lavaSlot;
        }

        // 如果既不需要放网也不需要放岩浆（或者没材料），重置瞄准
        if (targetPos == null || targetSlot == -1) {
            aimTimer = 0;
            lastTarget = null;
            return;
        }

        // 5. 执行两段式放置
        handlePlacement(targetPos, targetSlot);
    }

    private void handlePlacement(BlockPos targetPos, int slot) {
        // A. 计算怎么点击（找邻居方块 + 视线检查）
        PlaceData data = getBestPlaceData(targetPos);
        if (data == null) {
            aimTimer = 0; // 找不到合法的点击角度（被遮挡），重置
            return;
        }

        // B. 检查是否切换了目标方块
        if (!targetPos.equals(lastTarget)) {
            // 如果目标变了（比如从脚变到了头，或者敌人移动了），重新开始瞄准倒计时
            aimTimer = aimDelay.get();
            lastTarget = targetPos;
        }

        // C. 发送旋转包 (Tick 1 ~ Tick N)
        // 始终保持看向点击点，GrimAC 需要你盯着看
        Rotations.rotate(Rotations.getYaw(data.hitVec), Rotations.getPitch(data.hitVec));

        // D. 等待瞄准稳定
        if (aimTimer > 0) {
            aimTimer--;
            return; // 这一帧只旋转，不交互
        }

        // E. 瞄准完毕，执行交互
        boolean swapped = InvUtils.swap(slot, false); // 切换到物品
        if (swapped) {
            interact(new BlockHitResult(data.hitVec, data.side, data.neighbor, false));
            timer = placeDelay.get(); // 进入放置冷却
            aimTimer = 1; // 稍微重置一点瞄准时间，防止连点太快
        }
    }

    // ========== 核心判断逻辑 ==========

    private boolean shouldPlaceWeb(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        // 如果方块不是空气 且 不是流体 (说明有实体方块了)
        if (!state.isAir() && state.getFluidState().isEmpty()) {
            // 如果已经是蜘蛛网，且开启了智能省网 -> 不放
            if (state.getBlock() == Blocks.COBWEB && smartWeb.get()) return false;
            // 如果是别的方块（比如石头），也放不了 -> 不放
            if (state.getBlock() != Blocks.COBWEB) return false;
        }
        // 双重检查
        if (state.getBlock() == Blocks.COBWEB && smartWeb.get()) return false;
        return true;
    }

    private boolean shouldPlaceLava(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.getBlock() == Blocks.LAVA) return false; // 已经是岩浆
        if (!state.isAir() && state.getFluidState().isEmpty()) return false; // 是固体方块
        return true;
    }

    private void interact(BlockHitResult result) {
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // ========== 严格交互计算 (Raycast防吞) ==========

    private PlaceData getBestPlaceData(BlockPos target) {
        // 优先检查下方（地面），因为这是最自然的放置方式
        PlaceData data = checkSide(target, Direction.DOWN); 
        if (data != null) return data;

        // 检查四周
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            data = checkSide(target, dir);
            if (data != null) return data;
        }
        // 最后检查上方
        return checkSide(target, Direction.UP);
    }

    private PlaceData checkSide(BlockPos target, Direction offsetDir) {
        // 我们要点击的邻居方块位置
        BlockPos neighbor = target.offset(offsetDir);
        BlockState state = mc.world.getBlockState(neighbor);

        // 邻居必须是实体方块（不能是空气或水）
        if (state.isAir() || !state.getFluidState().isEmpty()) return null;

        // 我们要点击邻居的哪个面？(与 offsetDir 相反)
        // 例如：如果在目标下方找邻居，我们要点击邻居的"上面"
        Direction sideToClick = offsetDir.getOpposite();

        // 计算精确的点击点中心 (Hit Vector)
        Vec3d hitVec = Vec3d.ofCenter(neighbor).add(
            sideToClick.getOffsetX() * 0.5,
            sideToClick.getOffsetY() * 0.5,
            sideToClick.getOffsetZ() * 0.5
        );

        // 1. 距离检查
        if (mc.player.getEyePos().distanceTo(hitVec) > range.get()) return null;

        // 2. 视线遮挡检查 (GrimAC 核心)
        // 从眼睛发射射线到点击点，看中间有没有东西挡住
        RaycastContext context = new RaycastContext(
            mc.player.getEyePos(),
            hitVec,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        );
        BlockHitResult result = mc.world.raycast(context);
        
        // 如果射线击中了东西，且击中的不是我们要点击的那个方块 -> 说明被遮挡了
        if (result.getType() != HitResult.Type.MISS && !result.getBlockPos().equals(neighbor)) {
            return null;
        }

        return new PlaceData(neighbor, sideToClick, hitVec);
    }

    private static class PlaceData {
        public BlockPos neighbor;
        public Direction side;
        public Vec3d hitVec;

        public PlaceData(BlockPos neighbor, Direction side, Vec3d hitVec) {
            this.neighbor = neighbor;
            this.side = side;
            this.hitVec = hitVec;
        }
    }
}