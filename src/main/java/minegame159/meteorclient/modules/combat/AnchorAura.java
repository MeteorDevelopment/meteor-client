package minegame159.meteorclient.modules.combat;

//Created by squidoodly 03/08/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.DamageCalcUtils;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.stream.Collectors;

public class AnchorAura extends ToggleModule {
    public enum Mode{
        safe,
        suicide
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The distance in a single direction the anchors get placed.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("The distance in a single direction the anchors get set off.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("place-mode")
            .description("The way anchors are placed")
            .defaultValue(Mode.safe)
            .build()
    );
    
    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Places anchors in the air.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Mode> breakMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("break-mode")
            .description("The way anchors are set off.")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to anchors for you.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> spoofChange = sgGeneral.add(new BoolSetting.Builder()
            .name("spoof-change")
            .description("Spoofs item change to anchor.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage the anchor will place")
            .defaultValue(5.5)
            .build()
    );

    private final Setting<Double> maxDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("max-damage")
            .description("The maximum self-damage allowed")
            .defaultValue(3)
            .build()
    );

    private final Setting<Double> minHealth = sgPlace.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health you have to be for it to place")
            .defaultValue(15)
            .build()
    );

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
            .name("place")
            .description("Allow it to place anchors")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay ticks between placements.")
            .defaultValue(2)
            .min(0)
            .max(10)
            .build()
    );

    public AnchorAura() {super(Category.Combat, "anchor-aura", "Places and explodes respawn anchors for you,");}

    private int delayLeft = delay.get();
    private int preSlot;
    private BlockPos bestBlock;
    private BlockPos playerPos;
    private BlockPos pos;
    private boolean didBreak = false;

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        assert mc.player != null;
        assert mc.world != null;
        if (mc.world.getDimension().isRespawnAnchorWorking()) {
            Chat.info(this, "You are not in the Overworld. (highlight)Disabling(default)!");
            this.toggle();
            return;
        }
        if (getTotalHealth(mc.player) <= minHealth.get() && mode.get() != Mode.suicide) return;

        Iterator<AbstractClientPlayerEntity> validEntities = mc.world.getPlayers().stream()
                .filter(FriendManager.INSTANCE::attack)
                .filter(entityPlayer -> !entityPlayer.getDisplayName().equals(mc.player.getDisplayName()))
                .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) <= 10)
                .collect(Collectors.toList())
                .iterator();
        PlayerEntity target;
        if (validEntities.hasNext()) {
            target = validEntities.next();
        } else {
            return;
        }
        for (PlayerEntity i = null; validEntities.hasNext(); i = validEntities.next()) {
            if (i == null) continue;
            if (mc.player.distanceTo(i) < mc.player.distanceTo(target)) {
                target = i;
            }
        }

        assert mc.interactionManager != null;
        int glowSlot = -1;
        int nonGlowSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStack(i).getItem() == Items.GLOWSTONE) {
                glowSlot = i;
            } else if (mc.player.inventory.getStack(i).getItem() != Items.GLOWSTONE) {
                nonGlowSlot = i;
            }
        }

        if (glowSlot != -1 && nonGlowSlot != -1) {
            findAnchors(target);
            if (bestBlock != null) {
                Vec3d pos = new Vec3d(bestBlock.getX(), bestBlock.getY(), bestBlock.getZ());
                if ((DamageCalcUtils.bedDamage(mc.player, pos) < maxDamage.get() || breakMode.get() == Mode.suicide)
                        && DamageCalcUtils.bedDamage(target, pos) > minDamage.get()) {
                    int preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = glowSlot;
                    mc.player.setSneaking(false);
                    ((IKeyBinding) mc.options.keySneak).setPressed(false);
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(pos, Direction.UP, bestBlock, false));
                    mc.player.inventory.selectedSlot = nonGlowSlot;
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(pos, Direction.UP, bestBlock, false));
                    mc.player.inventory.selectedSlot = preSlot;
                    return;
                }
            }
        }
        if (didBreak) {
            if (delayLeft > 0) {
                delayLeft--;
                return;
            } else {
                delayLeft = delay.get();
            }
            if (place.get()) {
                findValidBlocks(target);
                if (bestBlock != null) {
                    Vec3d pos = new Vec3d(bestBlock.getX(), bestBlock.getY(), bestBlock.getZ());
                    if ((DamageCalcUtils.bedDamage(mc.player, pos) < maxDamage.get() || breakMode.get() == Mode.suicide)
                            && DamageCalcUtils.bedDamage(target, pos) > minDamage.get()) {
                        if (autoSwitch.get() && mc.player.getMainHandStack().getItem() != Items.RESPAWN_ANCHOR) {
                            int slot = InvUtils.findItemWithCount(Items.END_CRYSTAL).slot;
                            if (slot != -1 && slot < 9) {
                                if (spoofChange.get()) preSlot = mc.player.inventory.selectedSlot;
                                mc.player.inventory.selectedSlot = slot;
                            }
                        }
                        if (mc.player.getMainHandStack().getItem() != Items.RESPAWN_ANCHOR && mc.player.getOffHandStack().getItem() != Items.RESPAWN_ANCHOR)
                            return;
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(pos, Direction.UP, bestBlock, false));
                    }
                    if (spoofChange.get() && preSlot != mc.player.inventory.selectedSlot) {
                        mc.player.inventory.selectedSlot = preSlot;
                    }
                }
            }
            didBreak = false;
        }
        didBreak = true;
    });

    private void findValidBlocks(PlayerEntity target) {
        assert mc.world != null;
        assert mc.player != null;
        bestBlock = null;
        playerPos = mc.player.getBlockPos();
        for (double i = playerPos.getX() - placeRange.get(); i < playerPos.getX() + placeRange.get(); i++) {
            for (double j = playerPos.getZ() - placeRange.get(); j < playerPos.getZ() + placeRange.get(); j++) {
                for (int k = playerPos.getY() - 3; k < playerPos.getY() + 3; k++) {
                    pos = new BlockPos(i, k, j);
                    if (mc.world.canPlace(Blocks.RESPAWN_ANCHOR.getDefaultState(), pos, ShapeContext.absent())) {
                        if (airPlace.get()) {
                            if (bestBlock == null) bestBlock = pos;
                            if (DamageCalcUtils.bedDamage(target, new Vec3d(bestBlock.getX(), bestBlock.getY(), bestBlock.getZ()))
                                    < DamageCalcUtils.bedDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ()))
                                    && DamageCalcUtils.bedDamage(mc.player, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) < maxDamage.get()) {
                                bestBlock = pos;
                            }
                        } else if (!airPlace.get() && !mc.world.getBlockState(pos.down()).getMaterial().isReplaceable()) {
                            if (bestBlock == null) bestBlock = pos;
                            if (DamageCalcUtils.bedDamage(target, new Vec3d(bestBlock.getX(), bestBlock.getY(), bestBlock.getZ()))
                                    < DamageCalcUtils.bedDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ()))
                                    && DamageCalcUtils.bedDamage(mc.player, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) < maxDamage.get()) {
                                bestBlock = pos;
                            }
                        }
                    }
                }
            }
        }
    }

    private void findAnchors(PlayerEntity target) {
        assert mc.player != null;
        assert mc.world != null;
        bestBlock = null;
        playerPos = mc.player.getBlockPos();
        for (double i = playerPos.getX() - breakRange.get(); i < playerPos.getX() + breakRange.get(); i++) {
            for (double j = playerPos.getZ() - breakRange.get(); j < playerPos.getZ() + breakRange.get(); j++) {
                for (int k = playerPos.getY() - 3; k < playerPos.getY() + 3; k++) {
                    pos = new BlockPos(i, k, j);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                        if (bestBlock == null) bestBlock = pos;
                        if (DamageCalcUtils.bedDamage(target, new Vec3d(bestBlock.getX(), bestBlock.getY(), bestBlock.getZ()))
                                < DamageCalcUtils.bedDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ()))
                                && DamageCalcUtils.bedDamage(mc.player, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) < maxDamage.get()){
                            bestBlock = pos;
                        }
                    }
                }
            }
        }
    }

    private float getTotalHealth(PlayerEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    private boolean isEmpty(BlockPos pos) {
        assert mc.world != null;
        return (mc.world.getBlockState(pos).getMaterial().isReplaceable() || mc.world.isAir(pos)) && !mc.world.getEntities(null, new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D)).isEmpty();
    }
}
