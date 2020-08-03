package minegame159.meteorclient.modules.combat;

//Created by squidoodly 03/08/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.DamageCalcUtils;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
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

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.world.getDimension().isRespawnAnchorWorking()) {
            Chat.info(this, "You are not in the Overworld. (highlight)Disabling(default)!");
            this.toggle();
            return;
        }
        if (getTotalHealth(mc.player) <= minHealth.get() && mode.get() != Mode.suicide) return;
        if (delayLeft > 0) {
            delayLeft--;
            return;
        } else {
            delayLeft = delay.get();
        }
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
        if(place.get()) {
            List<BlockPos> validBlocks = findValidBlocks(target);

            BlockPos bestBlock = null;
            for (BlockPos blockPos : validBlocks) {
                BlockPos pos = blockPos.up();
                if (DamageCalcUtils.bedDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) > minDamage.get()) {
                    pos = blockPos.up();
                    BlockPos pos2 = bestBlock.up();
                    if (bestBlock == null) {
                        bestBlock = blockPos;
                    } else if (DamageCalcUtils.bedDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ()))
                            > DamageCalcUtils.bedDamage(target, new Vec3d(pos2.getX(), pos2.getY(), pos2.getZ()))) {
                        bestBlock = blockPos;
                    }
                }
            }
            if (bestBlock != null) {

                if(autoSwitch.get() && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL){
                    int slot = InvUtils.findItemWithCount(Items.END_CRYSTAL).slot;
                    if(slot != -1 && slot < 9){
                        if (spoofChange.get()) preSlot = mc.player.inventory.selectedSlot;
                        mc.player.inventory.selectedSlot = slot;
                    }
                }
                Utils.place(Blocks.RESPAWN_ANCHOR.getDefaultState(), bestBlock, true, false, true);
            }
            if (spoofChange.get() && preSlot != mc.player.inventory.selectedSlot) mc.player.inventory.selectedSlot = preSlot;
        }
        int glowSlot = -1;
        int nonGlowSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStack(i).getItem() == Items.GLOWSTONE) {
                glowSlot = i;
            }else if (mc.player.inventory.getStack(i).getItem() != Items.GLOWSTONE) {
                nonGlowSlot = i;
            }
        }
        if (glowSlot != -1 && nonGlowSlot != -1) {
            List<BlockPos> anchors = findAnchors(target);
            for (int i = 0; i < anchors.size() - 1; i++) {
                BlockPos anchor = anchors.get(i);
                Vec3d pos = new Vec3d(anchor.getX(), anchor.getY(), anchor.getZ());
                if ((DamageCalcUtils.bedDamage(mc.player, pos) < maxDamage.get() || breakMode.get() == Mode.suicide)
                        &&DamageCalcUtils.bedDamage(target, pos) > minDamage.get()) {
                    mc.player.inventory.selectedSlot = glowSlot;
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(pos, Direction.UP, anchor.up(), false));
                    mc.player.inventory.selectedSlot = nonGlowSlot;
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(pos, Direction.UP, anchor.up(), false));
                    return;
                }
            }
        }
    });

    private List<BlockPos> findValidBlocks(PlayerEntity target){
        Iterator<BlockPos> allBlocks = getRange(mc.player.getBlockPos(), placeRange.get()).iterator();
        List<BlockPos> validBlocks = new ArrayList<>();
        for(BlockPos i = null; allBlocks.hasNext(); i = allBlocks.next()){
            if (i == null) continue;
            if (airPlace.get() && isEmpty(i.up())) {
                validBlocks.add(i);
            }else if (mc.world.getBlockState(i).isAir() && isEmpty(i.up())) {
                validBlocks.add(i);
            }
        }
        validBlocks.sort(Comparator.comparingDouble(value ->  {
            BlockPos pos = value.up();
            return DamageCalcUtils.bedDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
        }));
        validBlocks.removeIf(blockpos -> {
            BlockPos pos = blockpos.up();
            return DamageCalcUtils.bedDamage(mc.player, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) > maxDamage.get();
        });
        Collections.reverse(validBlocks);
        return validBlocks;
    }

    private List<BlockPos> findAnchors(PlayerEntity target) {
        Iterator<BlockPos> allBlocks = getRange(mc.player.getBlockPos(), breakRange.get()).iterator();
        List<BlockPos> validBlocks = new ArrayList<>();
        for(BlockPos i = null; allBlocks.hasNext(); i = allBlocks.next()){
            if (mc.world.getBlockState(i).getBlock() == Blocks.RESPAWN_ANCHOR) {
                validBlocks.add(i);
            }
        }
        validBlocks.sort(Comparator.comparingDouble(value ->  {
            BlockPos pos = value.up();
            return DamageCalcUtils.bedDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
        }));
        validBlocks.removeIf(blockpos -> {
            BlockPos pos = blockpos.up();
            return DamageCalcUtils.bedDamage(mc.player, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) > maxDamage.get();
        });
        Collections.reverse(validBlocks);
        return validBlocks;
    }

    private List<BlockPos> getRange(BlockPos player, double range){
        List<BlockPos> allBlocks = new ArrayList<>();
        for(double i = player.getX() - range; i < player.getX() + range; i++){
            for(double j = player.getZ() - range; j < player.getZ() + range; j++){
                for(int k = player.getY() - 3; k < player.getY() + 3; k++){
                    BlockPos x = new BlockPos(i, k, j);
                    allBlocks.add(x);
                }
            }
        }
        return allBlocks;
    }

    private float getTotalHealth(PlayerEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    private boolean isEmpty(BlockPos pos) {
        return mc.world.isAir(pos) && mc.world.getEntities(null, new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D)).isEmpty();
    }
}
