package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.container.SlotActionType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EnderCrystalEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;

public class AutoArmor extends Module {
    public static AutoArmor INSTANCE;

    public enum Protection {
        Protection("Protection", Enchantments.PROTECTION),
        BlastProtection("Blast Protection", Enchantments.BLAST_PROTECTION),
        FireProtection("Fire Protection", Enchantments.FIRE_PROTECTION),
        ProjectileProtection("Projectile Protection", Enchantments.PROJECTILE_PROTECTION);

        private String name;
        private Enchantment enchantment;

        Protection(String name, Enchantment enchantment) {
            this.name = name;
            this.enchantment = enchantment;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private Setting<Protection> prioritizeProtection = addSetting(new EnumSetting.Builder<Protection>()
            .name("prioritize")
            .description("Which protection to prioritize.")
            .defaultValue(Protection.Protection)
            .build()
    );

    private Setting<Boolean> considerFrostWalker = addSetting(new BoolSetting.Builder()
            .name("consider-frost-walker")
            .description("Consider frost walker.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> switchToBlastProtWhenNearCrystalOrBed = addSetting(new BoolSetting.Builder()
            .name("switch-to-blast-prot-when-near-crystal-or-bed")
            .description("Switches to blast protection when near crystals or beds when not in overworld.")
            .defaultValue(true)
            .build()
    );

    private Setting<Double> crystalAndBedDetectionDistance = addSetting(new DoubleSetting.Builder()
            .name("crystal-and-bed-detection-distance")
            .description("Crystal and bed detection distance")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private BestItem helmet = new BestItem();
    private BestItem chestplate = new BestItem();
    private BestItem leggings = new BestItem();
    private BestItem boots = new BestItem();

    public AutoArmor() {
        super(Category.Player, "auto-armor", "Automatically equips the best armor.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen != null) return;

        Protection prePrioritizeProt = prioritizeProtection.get();
        if (switchToBlastProtWhenNearCrystalOrBed.get()) {
            // Check crystals
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EnderCrystalEntity && entity.distanceTo(mc.player) <= crystalAndBedDetectionDistance.get()) {
                    prioritizeProtection.set(Protection.BlastProtection);
                    break;
                }
            }

            // Check beds if not in overworld
            if (mc.world.dimension.getType() != DimensionType.OVERWORLD) {
                for (BlockEntity blockEntity : mc.world.blockEntities) {
                    if (blockEntity instanceof BedBlockEntity) {
                        float f = (float) (mc.player.x - blockEntity.getPos().getX());
                        float g = (float) (mc.player.y - blockEntity.getPos().getY());
                        float h = (float) (mc.player.z - blockEntity.getPos().getZ());
                        float distance = MathHelper.sqrt(f * f + g * g + h * h);

                        if (distance <= crystalAndBedDetectionDistance.get()) {
                            prioritizeProtection.set(Protection.BlastProtection);
                            break;
                        }
                    }
                }
            }
        }

        // Reset best items
        helmet.reset();
        chestplate.reset();
        leggings.reset();
        boots.reset();

        // Get best items
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getInvStack(i);
            if (!(itemStack.getItem() instanceof ArmorItem)) continue;

            // Helmet
            int score = getHelmetScore(itemStack);
            if (helmet.add(itemStack, score, i)) continue;

            // Chestplate
            score = getChestplateScore(itemStack);
            if (chestplate.add(itemStack, score, i)) continue;

            // Leggings
            score = getLeggingsScore(itemStack);
            if (leggings.add(itemStack, score, i)) continue;

            // Boots
            score = getBootsScore(itemStack);
            if (boots.add(itemStack, score, i)) continue;
        }

        // Apply items
        helmet.apply(5);
        chestplate.apply(6);
        leggings.apply(7);
        boots.apply(8);

        if (switchToBlastProtWhenNearCrystalOrBed.get()) prioritizeProtection.set(prePrioritizeProt);
    });

    private int getHelmetScore(ItemStack itemStack) {
        if (((ArmorItem) itemStack.getItem()).getSlotType() != EquipmentSlot.HEAD) return -1;
        int score = getBaseScore(itemStack);

        score += EnchantmentHelper.getLevel(Enchantments.AQUA_AFFINITY, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.RESPIRATION, itemStack);

        return score;
    }

    private int getChestplateScore(ItemStack itemStack) {
        if (((ArmorItem) itemStack.getItem()).getSlotType() != EquipmentSlot.CHEST) return -1;
        return getBaseScore(itemStack);
    }

    private int getLeggingsScore(ItemStack itemStack) {
        if (((ArmorItem) itemStack.getItem()).getSlotType() != EquipmentSlot.LEGS) return -1;
        return getBaseScore(itemStack);
    }

    private int getBootsScore(ItemStack itemStack) {
        if (((ArmorItem) itemStack.getItem()).getSlotType() != EquipmentSlot.FEET) return -1;
        int score = getBaseScore(itemStack);

        score += EnchantmentHelper.getLevel(Enchantments.DEPTH_STRIDER, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.FEATHER_FALLING, itemStack);
        if (considerFrostWalker.get()) score += EnchantmentHelper.getLevel(Enchantments.FROST_WALKER, itemStack);

        return score;
    }

    private int getBaseScore(ItemStack itemStack) {
        int score = 0;

        score += ((ArmorItem) itemStack.getItem()).getProtection();
        score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.THORNS, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.PROTECTION, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.FIRE_PROTECTION, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.PROJECTILE_PROTECTION, itemStack);

        return score;
    }

    private class BestItem {
        int bestProtScore = -1;
        int bestProtSlot = -1;

        int bestBlastProtScore = -1;
        int bestBlastProtSlot = -1;

        int bestFireProtScore = -1;
        int bestFireProtSlot = -1;

        int bestProjProtScore = -1;
        int bestProjProtSlot = -1;

        int bestOtherScore = -1;
        int bestOtherSlot = -1;

        boolean add(ItemStack itemStack, int score, int slot) {
            if (score == -1) return false;

            if (EnchantmentHelper.getLevel(Enchantments.PROTECTION, itemStack) > 0) {
                if (score > bestProtScore) {
                    bestProtScore = score;
                    bestProtSlot = slot;
                }
            } else if (EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, itemStack) > 0) {
                if (score > bestBlastProtScore) {
                    bestBlastProtScore = score;
                    bestBlastProtSlot = slot;
                }
            } else if (EnchantmentHelper.getLevel(Enchantments.FIRE_PROTECTION, itemStack) > 0) {
                if (score > bestFireProtScore) {
                    bestFireProtScore = score;
                    bestFireProtSlot = slot;
                }
            } else if (EnchantmentHelper.getLevel(Enchantments.PROJECTILE_PROTECTION, itemStack) > 0) {
                if (score > bestProjProtScore) {
                    bestProjProtScore = score;
                    bestProjProtSlot = slot;
                }
            } else {
                if (score > bestOtherScore) {
                    bestOtherScore = score;
                    bestOtherSlot = slot;
                }
            }

            return true;
        }

        void apply(int armorSlot) {
            ItemStack itemStack = mc.player.inventory.getInvStack(39 - (armorSlot - 5));
            int score = -1;
            if (!itemStack.isEmpty()) {
                score = getHelmetScore(itemStack);
                if (score == -1) score = getChestplateScore(itemStack);
                if (score == -1) score = getLeggingsScore(itemStack);
                if (score == -1) score = getBootsScore(itemStack);
            }

            if (prioritizeProtection.get() == Protection.Protection) {
                if (!applyProt(armorSlot)) applyOther(score, armorSlot);
            } else if (prioritizeProtection.get() == Protection.BlastProtection) {
                if (!applyBlastProt(armorSlot)) if (!applyProt(armorSlot)) applyOther(score, armorSlot);
            } else if (prioritizeProtection.get() == Protection.FireProtection) {
                if (!applyFireProt(armorSlot)) if (!applyProt(armorSlot)) applyOther(score, armorSlot);
            } else if (prioritizeProtection.get() == Protection.ProjectileProtection) {
                if (!applyProjProt(armorSlot)) if (!applyProt(armorSlot)) applyOther(score, armorSlot);
            }
        }

        boolean applyProt(int armorSlot) {
            if (bestProtSlot == -1) return false;
            ItemStack itemStack = mc.player.inventory.getInvStack(39 - (armorSlot - 5));
            if (EnchantmentHelper.getLevel(prioritizeProtection.get().enchantment, itemStack) > 0) return false;

            move(bestProtSlot, armorSlot);
            return true;
        }

        boolean applyBlastProt(int armorSlot) {
            if (bestBlastProtSlot == -1) return false;
            ItemStack itemStack = mc.player.inventory.getInvStack(39 - (armorSlot - 5));
            if (EnchantmentHelper.getLevel(prioritizeProtection.get().enchantment, itemStack) > 0) return false;

            move(bestBlastProtSlot, armorSlot);
            return true;
        }

        boolean applyFireProt(int armorSlot) {
            if (bestFireProtSlot == -1) return false;
            ItemStack itemStack = mc.player.inventory.getInvStack(39 - (armorSlot - 5));
            if (EnchantmentHelper.getLevel(prioritizeProtection.get().enchantment, itemStack) > 0) return false;

            move(bestFireProtSlot, armorSlot);
            return true;
        }

        boolean applyProjProt(int armorSlot) {
            if (bestProjProtSlot == -1) return false;
            ItemStack itemStack = mc.player.inventory.getInvStack(39 - (armorSlot - 5));
            if (EnchantmentHelper.getLevel(prioritizeProtection.get().enchantment, itemStack) > 0) return false;

            move(bestProjProtSlot, armorSlot);
            return true;
        }

        void applyOther(int score, int armorSlot) {
            if (bestOtherSlot == -1 || bestOtherScore <= score) return;
            move(bestOtherSlot, armorSlot);
        }

        void move(int from, int to) {
            boolean wasEmpty = mc.player.inventory.getInvStack(39 - (to - 5)).isEmpty();

            mc.interactionManager.method_2906(0, Utils.invIndexToSlotId(from), 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.method_2906(0, to, 0, SlotActionType.PICKUP, mc.player);

            if (!wasEmpty) mc.interactionManager.method_2906(0, Utils.invIndexToSlotId(from), 0, SlotActionType.PICKUP, mc.player);
        }

        void reset() {
            bestProtScore = -1;
            bestProtSlot = -1;

            bestBlastProtScore = -1;
            bestBlastProtSlot = -1;

            bestFireProtScore = -1;
            bestFireProtSlot = -1;

            bestProjProtScore = -1;
            bestProjProtSlot = -1;

            bestOtherScore = -1;
            bestOtherSlot = -1;
        }
    }
}
