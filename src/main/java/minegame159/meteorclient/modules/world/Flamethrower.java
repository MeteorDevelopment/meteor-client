package minegame159.meteorclient.modules.world;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.Rotations;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;

public class Flamethrower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFnS = settings.createGroup("Flint and Steel");

    public enum Mode {
        FlintAndSteel, LavaBucket
    }

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The maximum distance the animal has to be to be roasted.")
        .min(0.0)
        .defaultValue(5.0)
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode used to roast the animals.")
        .defaultValue(Mode.FlintAndSteel)
        .build()
    );

    private final Setting<Integer> tickInterval = sgGeneral.add(new IntSetting.Builder()
        .name("tick-interval")
        .defaultValue(5)
        .build()
    );

    private final Setting<Boolean> targetBabies = sgGeneral.add(new BoolSetting.Builder()
        .name("target-babies")
        .description("If checked babies will also be killed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgFnS.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the animal roasted.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgFnS.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Prevents flint and steel from being broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> putOutFire = sgFnS.add(new BoolSetting.Builder()
        .name("put-out-fire")
        .description("Tries to put out the fire when animal is low health, so the items don't burn.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to cook.")
            .defaultValue(Utils.asObject2BooleanOpenHashMap(
                EntityType.PIG, EntityType.COW, EntityType.SHEEP,
                EntityType.CHICKEN, EntityType.RABBIT))
            .build()
    );

    private Entity entity;
    private BlockPos lavaPosition = null;
    private int preSlot;
    private int ticks = 0;

    public Flamethrower() {
        super(Categories.World, "flamethrower", "Ignites every alive piece of food.");
    }

    @Override
    public void onDeactivate() {
        entity = null;
        lavaPosition = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (lavaPosition != null) {
            Rotations.rotate(Rotations.getYaw(lavaPosition), Rotations.getPitch(lavaPosition), -100, this::pickupLava);
        }
        entity = null;
        ticks++;
        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().getBoolean(entity.getType()) || mc.player.distanceTo(entity) > distance.get()) continue;
            if (entity.isFireImmune()) continue;
            if (entity == mc.player) continue;
            if (!targetBabies.get() && entity instanceof LivingEntity && ((LivingEntity)entity).isBaby()) continue;

            boolean success = selectSlot();

            if (success) {
                this.entity = entity;

                if (rotate.get() || mode.get() == Mode.LavaBucket) Rotations.rotate(Rotations.getYaw(entity.getBlockPos().down()), Rotations.getPitch(entity.getBlockPos().down()), -100, this::interact);
                else interact();

                return;
            }
        }
    }

    private void interact() {
        Block block = mc.world.getBlockState(entity.getBlockPos()).getBlock();
        Block bottom = mc.world.getBlockState(entity.getBlockPos().down()).getBlock();
        if (block.is(Blocks.WATER) || bottom.is(Blocks.WATER) || bottom.is(Blocks.GRASS_PATH)) return;
        if (block.is(Blocks.GRASS))  mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);
        LivingEntity animal = (LivingEntity) entity;

        if (putOutFire.get() && mode.get() == Mode.FlintAndSteel && animal.getHealth() < 1) {
            mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().west(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().east(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().north(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().south(), Direction.DOWN);
        } else {
            if (ticks >= tickInterval.get() && !entity.isOnFire()) {
                if (mode.get() == Mode.FlintAndSteel) {
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(
                        entity.getPos().subtract(new Vec3d(0, 1, 0)), Direction.UP, entity.getBlockPos().down(), false));
                }
                else if (mode.get() == Mode.LavaBucket && lavaPosition == null) {
                    BlockPos entityPos =  entity.getBlockPos().down();
                    BlockPos target = getTargetBlockPos();
                    mc.interactionManager.attackBlock(target, Direction.DOWN);
                    target = getTargetBlockPos();
                    if (target != null) {
                        mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                        lavaPosition = target;
                    }
                }
                ticks = 0;
            }

        }

        mc.player.inventory.selectedSlot = preSlot;
    }
    
    private boolean selectSlot() {
        preSlot = mc.player.inventory.selectedSlot;

        if (mode.get() == Mode.FlintAndSteel) {
            boolean findNewFlintAndSteel = false;
            if (mc.player.inventory.getMainHandStack().getItem() == Items.FLINT_AND_STEEL) {
                if (antiBreak.get() && mc.player.inventory.getMainHandStack().getDamage() >= mc.player.inventory.getMainHandStack().getMaxDamage() - 1)
                    findNewFlintAndSteel = true;
            } else if (mc.player.inventory.offHand.get(0).getItem() == Items.FLINT_AND_STEEL) {
                if (antiBreak.get() && mc.player.inventory.offHand.get(0).getDamage() >= mc.player.inventory.offHand.get(0).getMaxDamage() - 1)
                    findNewFlintAndSteel = true;
            } else {
                findNewFlintAndSteel = true;
            }

            boolean foundFlintAndSteel = !findNewFlintAndSteel;
            if (findNewFlintAndSteel) {
                int slot = InvUtils.findItemInHotbar(Items.FLINT_AND_STEEL,
                    itemStack -> (!antiBreak.get() || (antiBreak.get() && itemStack.getDamage() < itemStack.getMaxDamage() - 1)));

                if (slot != -1) {
                    mc.player.inventory.selectedSlot = slot;
                    foundFlintAndSteel = true;
                }
            }
            return foundFlintAndSteel;
        }
        else if (mode.get() == Mode.LavaBucket) {
            if (mc.player.inventory.getMainHandStack().getItem() == Items.LAVA_BUCKET) {
                return true;
            } else {
                int slot = InvUtils.findItemInHotbar(Items.LAVA_BUCKET);

                if (slot == -1) {
                    return false;
                }

                mc.player.inventory.selectedSlot = slot;
                return true;
            }
        }
        return false;
    }

    private BlockPos getTargetBlockPos() {
        Entity cameraEntity = mc.getCameraEntity();
        HitResult blockHit = cameraEntity.raycast(mc.interactionManager.getReachDistance()+1 /*just in case*/, 0.0F, false);
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return ((BlockHitResult) blockHit).getBlockPos();
    }

    private void pickupLava() {
        if (ticks < tickInterval.get()) return;
        //if (getTargetBlockPos() != lavaPosition) return;
        
        int slot = InvUtils.findItemInHotbar(Items.BUCKET);

        if (slot == -1) {
            //ChatUtils.moduleError(this, "No bucket found.");
            return;
        }

        mc.player.inventory.selectedSlot = slot;
        mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);

        lavaPosition = null;
    }
}
