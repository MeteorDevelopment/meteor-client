package minegame159.meteorclient.modules.combat;

//Updated by squidoodly 31/04/2020
//Updated by squidoodly 19/06/2020
//Updated by squidoodly 24/07/2020
//Updated by squidoodly 26-28/07/2020

import com.google.common.collect.Streams;
import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.DamageCalcUtils;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Collectors;

public class CrystalAura extends ToggleModule {
    public enum Mode{
        safe,
        suicide
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The distance in a single direction the crystals get placed.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("The distance in a single direction the crystals get broken.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("place-mode")
            .description("The way crystals are placed")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Mode> breakMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("break-mode")
            .description("The way crystals are broken.")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to crystals for you.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> spoofChange = sgGeneral.add(new BoolSetting.Builder()
            .name("spoof-change")
            .description("Spoofs item change to crystal.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage the crystal will place")
            .defaultValue(5.5)
            .build()
    );

    private final Setting<Double> maxDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("max-damage")
            .description("The maximum self-damage allowed")
            .defaultValue(3)
            .build()
    );

    private final Setting<Boolean> strict = sgPlace.add(new BoolSetting.Builder()
            .name("strict")
            .description("Helps compatibility with some servers.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minHealth = sgPlace.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health you have to be for it to place")
            .defaultValue(15)
            .build()
    );

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Attack through walls")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
            .name("place")
            .description("Allow it to place cystals")
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

    private final Setting<Boolean> antiWeakness = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-weakness")
            .description("Switches to tools when you have weakness")
            .defaultValue(true)
            .build()
    );

    public CrystalAura() {
        super(Category.Combat, "crystal-aura", "Places and breaks end crystals automatically");
    }

    private int preSlot;
    private int delayLeft = delay.get();

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (getTotalHealth(mc.player) <= minHealth.get() && mode.get() != Mode.suicide) return;
        if (delayLeft > 0) {
            delayLeft--;
            return;
        } else {
            delayLeft = delay.get();
        }
        if(place.get()) {

            Iterator<AbstractClientPlayerEntity> validEntities = mc.world.getPlayers().stream()
                    .filter(FriendManager.INSTANCE::attack)
                    .filter(entityPlayer -> !entityPlayer.getDisplayName().equals(mc.player.getDisplayName()))
                    .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) <= 10)
                    .collect(Collectors.toList())
                    .iterator();

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

            BlockPos bestBlock = null;
            for (BlockPos blockPos : validBlocks) {
                BlockPos pos = blockPos.up();
                if (DamageCalcUtils.crystalDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) > minDamage.get()) {
                    pos = blockPos.up();
                    if (bestBlock == null) {
                        bestBlock = blockPos;
                        continue;
                    }
                    BlockPos pos2 = bestBlock.up();
                    if (DamageCalcUtils.crystalDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ()))
                            > DamageCalcUtils.crystalDamage(target, new Vec3d(pos2.getX(), pos2.getY(), pos2.getZ()))) {
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

                Hand hand = Hand.MAIN_HAND;
                if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) hand = Hand.OFF_HAND;
                else if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) return;
                    placeBlock(bestBlock, hand);
            }
            if (spoofChange.get() && preSlot != mc.player.inventory.selectedSlot) mc.player.inventory.selectedSlot = preSlot;
        }
        Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof EndCrystalEntity)
                .filter(entity -> entity.distanceTo(mc.player) <= breakRange.get())
                .filter(Entity::isAlive)
                .filter(entity -> ignoreWalls.get() || mc.player.canSee(entity))
                .filter(entity -> !(breakMode.get() == Mode.safe)
                        || (getTotalHealth(mc.player) - DamageCalcUtils.crystalDamage(mc.player, entity.getPos()) > minHealth.get()
                        && DamageCalcUtils.crystalDamage(mc.player, entity.getPos()) < maxDamage.get()))
                .min(Comparator.comparingDouble(o -> o.distanceTo(mc.player)))
                .ifPresent(entity -> {
                    int preSlot = mc.player.inventory.selectedSlot;
                    if(mc.player.getActiveStatusEffects().containsKey(StatusEffects.WEAKNESS) && antiWeakness.get()){
                        for(int i = 0; i < 9; i++){
                            if(mc.player.inventory.getStack(i).getItem() instanceof SwordItem || mc.player.inventory.getStack(i).getItem() instanceof AxeItem){
                                mc.player.inventory.selectedSlot = i;
                            }
                        }
                    }

                    BlockPos pos = entity.getBlockPos();
                    Vec3d vec1 = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                    PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(Utils.getNeededYaw(vec1), Utils.getNeededPitch(vec1), mc.player.isOnGround());
                    mc.player.networkHandler.sendPacket(packet);

                    mc.interactionManager.attackEntity(mc.player, entity);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    mc.player.inventory.selectedSlot = preSlot;
                });
    }, EventPriority.HIGH);

    private void placeBlock(BlockPos block, Hand hand){
        Vec3d vec1 = new Vec3d(block.getX(), block.getY(), block.getZ()).add(0.5, 0.5, 0.5);
        PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(Utils.getNeededYaw(vec1), Utils.getNeededPitch(vec1), mc.player.isOnGround());
        mc.player.networkHandler.sendPacket(packet);

        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(new Vec3d(block.getX(), block.getY(), block.getZ()), Direction.UP, block , false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private List<BlockPos> findValidBlocks(AbstractClientPlayerEntity target){
        Iterator<BlockPos> allBlocks = getRange(mc.player.getBlockPos(), placeRange.get()).iterator();
        List<BlockPos> validBlocks = new ArrayList<>();
        for(BlockPos i = null; allBlocks.hasNext(); i = allBlocks.next()){
            if(i == null) continue;
            if((mc.world.getBlockState(i).getBlock() == Blocks.BEDROCK
                    || mc.world.getBlockState(i).getBlock() == Blocks.OBSIDIAN)
                    && isEmpty(i.up())){
                if (!strict.get()) {
                    validBlocks.add(i);
                } else if (strict.get() && isEmpty(i.up(2))) {
                    validBlocks.add(i);
                }
            }
        }
        validBlocks.sort(Comparator.comparingDouble(value ->  {
            BlockPos pos = value.up();
            return DamageCalcUtils.crystalDamage(target, new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
        }));
        validBlocks.removeIf(blockpos -> {
            BlockPos pos = blockpos.up();
            return DamageCalcUtils.crystalDamage(mc.player, new Vec3d(pos.getX(), pos.getY(), pos.getZ())) > maxDamage.get();
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
