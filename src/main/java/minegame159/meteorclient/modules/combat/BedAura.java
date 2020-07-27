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

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if( mc.player.dimension == DimensionType.OVERWORLD) {
            Chat.warning(this, "You are in the overworld. Disabling!");
            this.toggle();
            return;
        }
        if ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= minHealth.get() && mode.get() != Mode.suicide) return;
        if (place.get() && (!(mc.player.getMainHandStack().getItem() instanceof BedItem) && !(mc.player.getOffHandStack().getItem() instanceof BedItem))) return;
        if (place.get()) {
            ListIterator<BlockPos> validBlocks = Objects.requireNonNull(findValidBlocks()).listIterator();
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
            BlockPos bestBlock = mc.player.getBlockPos();
            for (BlockPos i = null; validBlocks.hasNext(); i = validBlocks.next()) {
                if (i == null) continue;
                Vec3d convert = new Vec3d(i.up());
                double selfDamage = DamageCalcUtils.bedDamage(mc.player, convert);
                if (mc.player.getHealth() + mc.player.getAbsorptionAmount() - selfDamage < minHealth.get()
                        && mode.get() != Mode.suicide) continue;
                double damage = DamageCalcUtils.bedDamage(target, convert);
                convert = new Vec3d(bestBlock.up());
                if (damage > DamageCalcUtils.bedDamage(target, convert)
                        && (selfDamage < maxDamage.get() || mode.get() == Mode.suicide) && damage > minDamage.get()) {
                    bestBlock = i;
                }
            }
            if (!bestBlock.equals(mc.player.getBlockPos())) {
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

    private List<BlockPos> findValidBlocks(){
        Iterator<BlockPos> allBlocks = getRange(mc.player.getBlockPos(), placeRange.get()).iterator();
        List<BlockPos> validBlocks = new ArrayList<>();
        for(BlockPos i = null; allBlocks.hasNext(); i = allBlocks.next()){
            if(i == null) continue;
            if(!mc.world.isAir(i) && (mc.world.isAir(i.up())
                    && mc.world.getEntities(null, new Box(i.up().getX(), i.up().getY(), i.up().getZ(), i.up().getX() + 1.0D, i.up().getY() + 1.0D, i.up().getZ() + 1.0D)).isEmpty())
                    && (mc.world.isAir(i.add(1, 1, 0)) || mc.world.isAir(i.add(-1, 1, 0))
                    || mc.world.isAir(i.add(0, 1, 1)) || mc.world.isAir(i.add(0, 1, -1)))) {
                validBlocks.add(i);
            }
        }
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
