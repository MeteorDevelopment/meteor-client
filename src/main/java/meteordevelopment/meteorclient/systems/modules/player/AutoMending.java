package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoMending extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgArmor = settings.createGroup("Armor");
    private final SettingGroup sgTools = settings.createGroup("Tools");

    // General settings
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("The tick delay to check for mending.")
        .defaultValue(20)
        .min(1)
        .build()
    );

    // Armor settings
    private final Setting<Boolean> mendHelmet = sgArmor.add(new BoolSetting.Builder()
        .name("mend-helmet")
        .description("Automatically mend the helmet.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> helmetThreshold = sgArmor.add(new IntSetting.Builder()
        .name("helmet-threshold")
        .description("The durability threshold to start mending the helmet.")
        .defaultValue(40)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> helmetMaxDurability = sgArmor.add(new IntSetting.Builder()
        .name("helmet-max-durability")
        .description("The maximum durability to mend the helmet.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> mendChestplate = sgArmor.add(new BoolSetting.Builder()
        .name("mend-chestplate")
        .description("Automatically mend the chestplate.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> chestplateThreshold = sgArmor.add(new IntSetting.Builder()
        .name("chestplate-threshold")
        .description("The durability threshold to start mending the chestplate.")
        .defaultValue(40)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> chestplateMaxDurability = sgArmor.add(new IntSetting.Builder()
        .name("chestplate-max-durability")
        .description("The maximum durability to mend the chestplate.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> mendLeggings = sgArmor.add(new BoolSetting.Builder()
        .name("mend-leggings")
        .description("Automatically mend the leggings.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> leggingsThreshold = sgArmor.add(new IntSetting.Builder()
        .name("leggings-threshold")
        .description("The durability threshold to start mending the leggings.")
        .defaultValue(40)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> leggingsMaxDurability = sgArmor.add(new IntSetting.Builder()
        .name("leggings-max-durability")
        .description("The maximum durability to mend the leggings.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> mendBoots = sgArmor.add(new BoolSetting.Builder()
        .name("mend-boots")
        .description("Automatically mend the boots.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> bootsThreshold = sgArmor.add(new IntSetting.Builder()
        .name("boots-threshold")
        .description("The durability threshold to start mending the boots.")
        .defaultValue(40)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> bootsMaxDurability = sgArmor.add(new IntSetting.Builder()
        .name("boots-max-durability")
        .description("The maximum durability to mend the boots.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> mendElytra = sgArmor.add(new BoolSetting.Builder()
        .name("mend-elytra")
        .description("Automatically mend the elytra.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> elytraThreshold = sgArmor.add(new IntSetting.Builder()
        .name("elytra-threshold")
        .description("The durability threshold to start mending the elytra.")
        .defaultValue(40)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> elytraMaxDurability = sgArmor.add(new IntSetting.Builder()
        .name("elytra-max-durability")
        .description("The maximum durability to mend the elytra.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    // Tools settings
    private final Setting<Boolean> mendSword = sgTools.add(new BoolSetting.Builder()
        .name("mend-sword")
        .description("Automatically mend the sword.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swordThreshold = sgTools.add(new IntSetting.Builder()
        .name("sword-threshold")
        .description("The durability threshold to start mending the sword.")
        .defaultValue(40)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> swordMaxDurability = sgTools.add(new IntSetting.Builder()
        .name("sword-max-durability")
        .description("The maximum durability to mend the sword.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> mendPickaxe = sgTools.add(new BoolSetting.Builder()
        .name("mend-pickaxe")
        .description("Automatically mend the pickaxe.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> pickaxeThreshold = sgTools.add(new IntSetting.Builder()
        .name("pickaxe-threshold")
        .description("The durability threshold to start mending the pickaxe.")
        .defaultValue(40)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> pickaxeMaxDurability = sgTools.add(new IntSetting.Builder()
        .name("pickaxe-max-durability")
        .description("The maximum durability to mend the pickaxe.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private int tickDelayLeft;

    public AutoMending() {
        super(Categories.Player, "auto-mending", "Automatically mends items in your offhand using XP bottles.");
    }

    @Override
    public void onActivate() {
        tickDelayLeft = tickDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (tickDelayLeft <= 0) {
            tickDelayLeft = tickDelay.get();

            if (mendElytra.get() && mendItem(findItemInInventory(Items.ELYTRA), elytraThreshold.get(), elytraMaxDurability.get())) return;
            if (mendChestplate.get() && mendItem(findItemInInventory(Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE), chestplateThreshold.get(), chestplateMaxDurability.get())) return;
            if (mendLeggings.get() && mendItem(findItemInInventory(Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS), leggingsThreshold.get(), leggingsMaxDurability.get())) return;
            if (mendBoots.get() && mendItem(findItemInInventory(Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS), bootsThreshold.get(), bootsMaxDurability.get())) return;
            if (mendHelmet.get() && mendItem(findItemInInventory(Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET), helmetThreshold.get(), helmetMaxDurability.get())) return;
            if (mendSword.get() && mendItem(findItemInInventory(Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD), swordThreshold.get(), swordMaxDurability.get())) return;
            if (mendPickaxe.get() && mendItem(findItemInInventory(Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE), pickaxeThreshold.get(), pickaxeMaxDurability.get())) return;
        } else {
            tickDelayLeft--;
        }
    }

    private ItemStack findItemInInventory(Item... items) {
        for (Item item : items) {
            for (ItemStack stack : mc.player.getInventory().main) {
                if (stack.getItem() == item) return stack;
            }
            for (ItemStack stack : mc.player.getInventory().armor) {
                if (stack.getItem() == item) return stack;
            }
        }
        return null;
    }

    private boolean mendItem(ItemStack stack, int threshold, int maxDurability) {
        if (stack == null) return false;

        int currentDurability = stack.getMaxDamage() - stack.getDamage();
        int percentage = (currentDurability * 100) / stack.getMaxDamage();

        if (percentage <= threshold && percentage < maxDurability) {
            int slot = mc.player.getInventory().indexOf(stack);
            if (slot == -1) return false;

            InvUtils.move().from(slot).to(SlotUtils.OFFHAND);
            throwXPBottles();
            InvUtils.move().from(SlotUtils.OFFHAND).to(slot);
            return true;
        }
        return false;
    }

    private void throwXPBottles() {
        FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        while (needsMending(mc.player.getOffHandStack()) && exp.found()) {
            FindItemResult finalExp = exp;
            Rotations.rotate(mc.player.getYaw(), 90, () -> {
                if (finalExp.getHand() != null) {
                    mc.interactionManager.interactItem(mc.player, finalExp.getHand());
                } else {
                    InvUtils.swap(finalExp.slot(), true);
                    mc.interactionManager.interactItem(mc.player, finalExp.getHand());
                    InvUtils.swapBack();
                }
            });
            exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        }
    }

    private boolean needsMending(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() == Items.AIR) return false;
        int maxDurability = stack.getMaxDamage();
        int currentDurability = maxDurability - stack.getDamage();
        int percentage = (currentDurability * 100) / maxDurability;
        return percentage < 100;
    }
}
