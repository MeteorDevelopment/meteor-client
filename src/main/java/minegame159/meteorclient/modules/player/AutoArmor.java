package minegame159.meteorclient.modules.player;

//Updated by squidoodly 15/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.DamageEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.BadRespawnPointDamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;

public class AutoArmor extends ToggleModule {
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

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Protection> prioritizeProtection = sgGeneral.add(new EnumSetting.Builder<Protection>()
            .name("prioritize")
            .description("Which protection to prioritize.")
            .defaultValue(Protection.Protection)
            .build()
    );

    private final Setting<Boolean> considerFrostWalker = sgGeneral.add(new BoolSetting.Builder()
            .name("consider-frost-walker")
            .description("Consider frost walker.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> switchToBlastProtWhenNearCrystalOrBed = sgGeneral.add(new BoolSetting.Builder()
            .name("switch-to-blast-prot-when-near-crystal-or-bed")
            .description("Switches to blast protection when near crystals or beds when not in overworld.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> crystalAndBedDetectionDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("crystal-and-bed-detection-distance")
            .description("Crystal and bed detection distance")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Stops you from breaking your armor.")
            .defaultValue(false)
            .build()
    );

    private final BestItem helmet = new BestItem();
    private final BestItem chestplate = new BestItem();
    private final BestItem leggings = new BestItem();
    private final BestItem boots = new BestItem();

    private boolean manageChestplate;

    public AutoArmor() {
        super(Category.Player, "auto-armor", "Automatically equips the best armor.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen instanceof HandledScreen<?>) return;

        doTick();
    });

    private void doTick() {
        Protection prePrioritizeProt = prioritizeProtection.get();
        if (switchToBlastProtWhenNearCrystalOrBed.get()) {
            // Check crystals
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity && entity.distanceTo(mc.player) <= crystalAndBedDetectionDistance.get()) {
                    prioritizeProtection.set(Protection.BlastProtection);
                    break;
                }
            }

            // Check beds if not in overworld
            if (mc.world.getDimension() != DimensionType.getOverworldDimensionType()) {
                for (BlockEntity blockEntity : mc.world.blockEntities) {
                    if (blockEntity instanceof BedBlockEntity) {
                        float f = (float) (mc.player.getX() - blockEntity.getPos().getX());
                        float g = (float) (mc.player.getY() - blockEntity.getPos().getY());
                        float h = (float) (mc.player.getZ() - blockEntity.getPos().getZ());
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

        // Check for elytra
        checkElytra();

        // Manage chestplate
        if (manageChestplate) {
            manageChestplate = false;
            chestplate.manage = true;
        }

        // Get best items
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if (!(itemStack.getItem() instanceof ArmorItem) || (antiBreak.get() && (itemStack.getMaxDamage() - itemStack.getDamage()) <= 11)) continue;

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
    }

    @EventHandler
    private Listener<DamageEvent> onDamage = new Listener<>(event -> {
        if (event.entity.getEntityId() != mc.player.getEntityId()) return;

        ItemStack itemStack = mc.player.inventory.getStack(39 - (6 - 5));
        if (itemStack.getItem() != Items.ELYTRA) return;

        if (event.source instanceof EntityDamageSource || event.source instanceof BadRespawnPointDamageSource) {
            manageChestplate = true;
            doTick();
        }
    });

    private void checkElytra() {
        ItemStack itemStack = mc.player.inventory.getStack(39 - (6 - 5));
        chestplate.manage = itemStack.getItem() != Items.ELYTRA;
    }

    private int getHelmetScore(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ArmorItem)) return -1;
        if (((ArmorItem) itemStack.getItem()).getSlotType() != EquipmentSlot.HEAD) return -1;
        int score = getBaseScore(itemStack);
        if(score == -1) return -1;

        score += EnchantmentHelper.getLevel(Enchantments.AQUA_AFFINITY, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.RESPIRATION, itemStack);

        return score;
    }

    private int getChestplateScore(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ArmorItem)) return -1;
        if (((ArmorItem) itemStack.getItem()).getSlotType() != EquipmentSlot.CHEST) return -1;
        if(getBaseScore(itemStack) == -1) return -1;
        return getBaseScore(itemStack);
    }

    private int getLeggingsScore(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ArmorItem)) return -1;
        if (((ArmorItem) itemStack.getItem()).getSlotType() != EquipmentSlot.LEGS) return -1;
        if(getBaseScore(itemStack) == -1) return -1;
        return getBaseScore(itemStack);
    }

    private int getBootsScore(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ArmorItem)) return -1;
        if (((ArmorItem) itemStack.getItem()).getSlotType() != EquipmentSlot.FEET) return -1;
        int score = getBaseScore(itemStack);
        if(score == -1) return -1;

        score += EnchantmentHelper.getLevel(Enchantments.DEPTH_STRIDER, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.FEATHER_FALLING, itemStack);
        if (considerFrostWalker.get()) score += EnchantmentHelper.getLevel(Enchantments.FROST_WALKER, itemStack);

        return score;
    }

    private int getBaseScore(ItemStack itemStack) {
        if((antiBreak.get() && (itemStack.getMaxDamage() - itemStack.getDamage()) <= 11)) return -1;
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
        boolean manage = true;

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
            if (!manage) return;

            ItemStack itemStack = mc.player.inventory.getStack(39 - (armorSlot - 5));
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
            ItemStack itemStack = mc.player.inventory.getStack(39 - (armorSlot - 5));
            if (EnchantmentHelper.getLevel(prioritizeProtection.get().enchantment, itemStack) > 0) return false;

            move(bestProtSlot, armorSlot);
            return true;
        }

        boolean applyBlastProt(int armorSlot) {
            if (bestBlastProtSlot == -1) return false;
            ItemStack itemStack = mc.player.inventory.getStack(39 - (armorSlot - 5));
            if (EnchantmentHelper.getLevel(prioritizeProtection.get().enchantment, itemStack) > 0) return false;

            move(bestBlastProtSlot, armorSlot);
            return true;
        }

        boolean applyFireProt(int armorSlot) {
            if (bestFireProtSlot == -1) return false;
            ItemStack itemStack = mc.player.inventory.getStack(39 - (armorSlot - 5));
            if (EnchantmentHelper.getLevel(prioritizeProtection.get().enchantment, itemStack) > 0) return false;

            move(bestFireProtSlot, armorSlot);
            return true;
        }

        boolean applyProjProt(int armorSlot) {
            if (bestProjProtSlot == -1) return false;
            ItemStack itemStack = mc.player.inventory.getStack(39 - (armorSlot - 5));
            if (EnchantmentHelper.getLevel(prioritizeProtection.get().enchantment, itemStack) > 0) return false;

            move(bestProjProtSlot, armorSlot);
            return true;
        }

        void applyOther(int score, int armorSlot) {
            if (bestOtherSlot == -1 || bestOtherScore <= score) return;
            move(bestOtherSlot, armorSlot);
        }

        void move(int from, int to) {
            boolean wasEmpty = mc.player.inventory.getStack(39 - (to - 5)).isEmpty();

            InvUtils.clickSlot(InvUtils.invIndexToSlotId(from), 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(to, 0, SlotActionType.PICKUP);

            if (!wasEmpty) InvUtils.clickSlot(InvUtils.invIndexToSlotId(from), 0, SlotActionType.PICKUP);
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
