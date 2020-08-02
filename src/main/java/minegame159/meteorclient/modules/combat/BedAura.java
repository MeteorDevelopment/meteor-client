package minegame159.meteorclient.modules.combat;

//Created by squidoodly 03/06/2020
//Updated by squidoodly 19/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.DamageCalcUtils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.stream.Collectors;

public class BedAura extends ToggleModule {
    public enum Mode{
        safe,
        suicide
    }

    public BedAura(){
        super(Category.Combat, "bed-aura", "Automatically places and blows up beds in the nether");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The distance in a single direction the beds get placed.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("The distance in a single direction the beds get broken.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("place-mode")
            .description("The way beds are placed")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Mode> clickMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("break-mode")
            .description("The way the beds are broken")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to bed automatically")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches back to the previous slot after auto switching.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between placements.")
            .defaultValue(2)
            .min(0)
            .max(10)
            .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Places beds in the air if they do more damage.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage the beds will place")
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
            .description("Allow it to place beds")
            .defaultValue(true)
            .build()
    );

    private int delayLeft = delay.get();

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (delayLeft > 0) {
            delayLeft --;
            return;
        } else {
            delayLeft = delay.get();
        }
        if( mc.player.dimension == DimensionType.OVERWORLD) {
            Chat.warning(this, "You are in the overworld. Disabling!");
            this.toggle();
            return;
        }
        if ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= minHealth.get() && mode.get() != Mode.suicide) return;
        if (place.get() && (!(mc.player.getMainHandStack().getItem() instanceof BedItem) && !(mc.player.getOffHandStack().getItem() instanceof BedItem))) return;
        if (place.get()) {
            Iterator<AbstractClientPlayerEntity> validEntities = mc.world.getPlayers().stream()
                    .filter(FriendManager.INSTANCE::attack)
                    .filter(entityPlayer -> !entityPlayer.getDisplayName().equals(mc.player.getDisplayName()))
                    .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) <= 10)
                    .collect(Collectors.toList()).iterator();

            AbstractClientPlayerEntity target;
            if (validEntities.hasNext()) {
                target = validEntities.next();
            } else {
                return;
            }
            for (AbstractClientPlayerEntity i = null; validEntities.hasNext(); i = validEntities.next()) {
                if (i == null) continue;
                if (mc.player.distanceTo(i) < mc.player.distanceTo(target)) {
                    target = i;
                }
            }
            List<BlockPos> validBlocks = findValidBlocks(target);
            BlockPos bestBlock = validBlocks.get(0);
            int preSlot = -1;
            if (DamageCalcUtils.bedDamage(target, new Vec3d(bestBlock.up())) > minDamage.get()) {
                if (autoSwitch.get()) {
                    for (int i = 0; i < 9; i++) {
                        if (mc.player.inventory.getInvStack(i).getItem() instanceof BedItem) {
                            preSlot = mc.player.inventory.selectedSlot;
                            mc.player.inventory.selectedSlot = i;
                        }
                    }
                }
                double north = -1;
                double east = -1;
                double south = -1;
                double west = -1;
                if(mc.world.isAir(bestBlock.add(1, 1, 0))){
                    east = DamageCalcUtils.bedDamage(target, new Vec3d(bestBlock.add(1, 1, 0)));
                }
                if(mc.world.isAir(bestBlock.add(-1, 1, 0))){
                    west = DamageCalcUtils.bedDamage(target, new Vec3d(bestBlock.add(-1, 1, 0)));
                }
                if(mc.world.isAir(bestBlock.add(0, 1, 1))){
                    south = DamageCalcUtils.bedDamage(target, new Vec3d(bestBlock.add(0, 1, 1)));
                }
                if(mc.world.isAir(bestBlock.add(0, 1, -1))){
                    north = DamageCalcUtils.bedDamage(target, new Vec3d(bestBlock.add(0, 1, -1)));
                }
                Hand hand = Hand.MAIN_HAND;
                if (!(mc.player.getMainHandStack().getItem() instanceof BedItem) && mc.player.getOffHandStack().getItem() instanceof BedItem) {
                    hand = Hand.OFF_HAND;
                }
                if((east > north) && (east > south) && (east > west)){
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(-90f, mc.player.pitch, mc.player.onGround));
                }else if((east < north) && (north > south) && (north > west)){
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(179f, mc.player.pitch, mc.player.onGround));
                }else if((south > north) && (east < south) && (south > west)){
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(1f, mc.player.pitch, mc.player.onGround));
                }else if((west > north) && (west > south) && (east < west)){
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(90f, mc.player.pitch, mc.player.onGround));
                }
                mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(new Vec3d(bestBlock), Direction.UP, bestBlock, false));
                mc.player.swingHand(Hand.MAIN_HAND);
                if (preSlot != -1 && mc.player.inventory.selectedSlot != preSlot && switchBack.get()){
                    mc.player.inventory.selectedSlot = preSlot;
                }
            }
        }
        for(BlockEntity entity : mc.world.blockEntities){
            if(entity instanceof BedBlockEntity && Math.sqrt(entity.getSquaredDistance(mc.player.x, mc.player.y, mc.player.z)) <= breakRange.get()){
                if(DamageCalcUtils.bedDamage(mc.player, new Vec3d(entity.getPos())) < maxDamage.get()
                    || (mc.player.getHealth() + mc.player.getAbsorptionAmount() - DamageCalcUtils.bedDamage(mc.player, new Vec3d(entity.getPos()))) < minHealth.get() || clickMode.get().equals(Mode.suicide)){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, entity.getPos(), false));
                }

            }
        }
    });

    private List<BlockPos> findValidBlocks(PlayerEntity target){
        Iterator<BlockPos> allBlocks = getRange(mc.player.getBlockPos(), placeRange.get()).iterator();
        List<BlockPos> validBlocks = new ArrayList<>();
        for(BlockPos i = null; allBlocks.hasNext(); i = allBlocks.next()){
            if(i == null) continue;
            if((mc.world.isAir(i.up())
                    && mc.world.getEntities(null, new Box(i.up().getX(), i.up().getY(), i.up().getZ(), i.up().getX() + 1.0D, i.up().getY() + 1.0D, i.up().getZ() + 1.0D)).isEmpty())
                    && (mc.world.isAir(i.add(1, 1, 0)) || mc.world.isAir(i.add(-1, 1, 0))
                    || mc.world.isAir(i.add(0, 1, 1)) || mc.world.isAir(i.add(0, 1, -1)))) {
                if (airPlace.get()) {
                    validBlocks.add(i);
                } else if (!airPlace.get() && !mc.world.isAir(i)) {
                    validBlocks.add(i);
                }
            }
        }
        validBlocks.sort(Comparator.comparingDouble(value ->  DamageCalcUtils.crystalDamage(target, new Vec3d(value.up()))));
        validBlocks.removeIf(blockpos -> DamageCalcUtils.crystalDamage(mc.player, new Vec3d(blockpos.up())) > maxDamage.get());
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
}
