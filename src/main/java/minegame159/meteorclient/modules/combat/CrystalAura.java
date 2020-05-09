package minegame159.meteorclient.modules.combat;

//Updated by squidoodly 31/04/2020

import com.google.common.collect.Streams;
import com.sun.org.apache.xpath.internal.operations.Mod;
import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.DamageCalcUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EnderCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Collectors;

public class CrystalAura extends ToggleModule {
    public enum Mode{
        safe,
        suicide
    }

    public Setting<Integer> range = addSetting(new IntSetting.Builder()
            .name("range")
            .description("Attack range")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    public Setting<Mode> mode = addSetting(new EnumSetting.Builder<Mode>()
            .name("place-mode")
            .description("The way crystals are placed")
            .defaultValue(Mode.safe)
            .build()
    );

    public Setting<Double> minDamage = addSetting(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage the crystal will place")
            .defaultValue(5.5)
            .build()
    );

    public Setting<Double> maxDamage = addSetting(new DoubleSetting.Builder()
            .name("max-damage")
            .description("The maximum self-damage allowed")
            .defaultValue(3)
            .build()
    );
    public Setting<Boolean> breakMode = addSetting(new BoolSetting.Builder()
            .name("anti-suicide")
            .description("The way the crystals are broken")
            .defaultValue(true)
            .build()
    );

    public Setting<Double> minHealth = addSetting(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health you have to be for it to place")
            .defaultValue(15)
            .build()
    );

    public Setting<Boolean> ignoreWalls = addSetting(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Attack through walls")
            .defaultValue(true)
            .build()
    );

    public Setting<Boolean> place = addSetting(new BoolSetting.Builder()
            .name("place")
            .description("Allow it to place cystals")
            .defaultValue(true)
            .build()
    );

    public CrystalAura() {
        super(Category.Combat, "crystal-aura", "You know what it does");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= minHealth.get() && mode.get() != Mode.suicide) return;
        if(place.get()) {
            ListIterator<BlockPos> validBlocks = Objects.requireNonNull(findValidBlocks()).listIterator();
            Iterator<AbstractClientPlayerEntity> validEntities = mc.world.getPlayers().stream().filter(entityPlayer -> !FriendManager.INSTANCE.isTrusted(entityPlayer)).collect(Collectors.toList()).iterator();
            AbstractClientPlayerEntity target = validEntities.next();
            for (AbstractClientPlayerEntity i = null; validEntities.hasNext(); i = validEntities.next()) {
                if (i == null) continue;
                if (mc.player.distanceTo(i) < mc.player.distanceTo(target)) {
                    target = i;
                }
            }
            BlockPos bestBlock = null;
            for (BlockPos i = null; validBlocks.hasNext(); i = validBlocks.next()) {
                if (i == null) continue;
                Vec3d convert = new Vec3d(i.getX(), i.getY(), i.getZ()).add(0, 1, 0);
                if(mc.player.getHealth() + mc.player.getAbsorptionAmount() - DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.blastProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.crystalDamage(mc.player, convert))))
                        < minHealth.get() && mode.get() != Mode.suicide) continue;
                double damage = DamageCalcUtils.resistanceReduction(target, DamageCalcUtils.blastProtReduction(target, DamageCalcUtils.armourCalc(target, DamageCalcUtils.crystalDamage(target, convert))))
                        - DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.blastProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.crystalDamage(mc.player, convert))));
                if(bestBlock != null) {
                    convert = new Vec3d(bestBlock.getX(), bestBlock.getY(), bestBlock.getZ()).add(0, 1, 0);
                    if (damage > DamageCalcUtils.resistanceReduction(target, DamageCalcUtils.blastProtReduction(target, DamageCalcUtils.armourCalc(target, DamageCalcUtils.crystalDamage(target, convert))))
                            - DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.blastProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.crystalDamage(mc.player, convert))))) {
                        bestBlock = i;
                        break;
                    }
                }else{
                    bestBlock = i;
                }
            }
            PlayerInteractBlockC2SPacket placePacket = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, bestBlock, false));
            mc.player.networkHandler.sendPacket(placePacket);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof EnderCrystalEntity)
                .filter(entity -> entity.distanceTo(mc.player) <= range.get())
                .filter(entity -> ignoreWalls.get() || mc.player.canSee(entity))
                .filter(entity -> !breakMode.get() || mc.player.getHealth() + mc.player.getAbsorptionAmount()
                        - DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.blastProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.crystalDamage(mc.player, entity.getPos()))))
                        < minHealth.get())
                .min(Comparator.comparingDouble(o -> o.distanceTo(mc.player)))
                .ifPresent(entity -> {
                    mc.interactionManager.attackEntity(mc.player, entity);
                    mc.player.swingHand(Hand.MAIN_HAND);
                });
    }, EventPriority.HIGH);

    private List<BlockPos> findValidBlocks(){
        Iterator<BlockPos> allBlocks = getRange(mc.player.getBlockPos(), range.get()).iterator();
        List<BlockPos> validBlocks = new ArrayList<>();
        for(BlockPos i = null; allBlocks.hasNext(); i = allBlocks.next()){
            if(i == null) continue;
            if((mc.world.getBlockState(i).getBlock() == Blocks.BEDROCK
                    || mc.world.getBlockState(i).getBlock() == Blocks.OBSIDIAN)
                    && mc.world.getBlockState(i.add(0, 1, 0)).getBlock() == Blocks.AIR
                    && mc.world.getBlockState(i.add(0, 2, 0)).getBlock() == Blocks.AIR){
                validBlocks.add(i);
            }
        }
        return validBlocks;
    }

    private List<BlockPos> getRange(BlockPos player, int range){
        List<BlockPos> allBlocks = new ArrayList<>();
        for(int i = player.getX() - Math.floorDiv(range, 2); i < player.getX() + Math.floorDiv(range, 2); i++){
            for(int j = player.getZ() - Math.floorDiv(range, 2); j < player.getZ() + Math.floorDiv(range, 2); j++){
                for(int k = -2; k < 2; k++){
                    BlockPos x = new BlockPos(i, k, j);
                    allBlocks.add(x);
                }
            }
        }
        return allBlocks;
    }
}
