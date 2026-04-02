/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.mixin.AbstractMountInventoryMenuAccessor;
import meteordevelopment.meteorclient.mixin.CreativeModeInventoryScreenAccessor;
import meteordevelopment.meteorclient.mixin.CreativeModeTabsAccessor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SlotUtils {
    /**
     * These constants refer to the slot index of relevant player slots. They are used when dealing directly with the
     * player inventory - e.g. {@code mc.player.getInventory().getSelectedSlot()} returns the slot index of your
     * selected slot (i.e. main hand).
     *
     * @see net.minecraft.world.entity.player.Inventory
     * @see Slot#index
     */
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 8;
    public static final int MAIN_START = 9;
    public static final int MAIN_END = 35;
    public static final int ARMOR_START = 36;
    public static final int ARMOR_END = 39;
    public static final int OFFHAND = 40;

    private SlotUtils() {
    }

    /**
     * Slot ids are used when inventory interactions have to be communicated to the server - you'll only find references
     * to slot ids when dealing with screen handlers or slot/inventory packets. All the methods in this class are used
     * to translate slot indices to the ids for each handled screen.
     *
     * @see <a href="https://minecraft.wiki/w/Java_Edition_protocol/Inventory">the minecraft.wiki page</a> for every slot id
     * @see MultiPlayerGameMode#handleInventoryMouseClick(int, int, int, ClickType, Player)
     * @see AbstractContainerMenu#doClick(int, int, ClickType, Player)
     * @see Slot#index
     */
    public static int indexToId(int i) {
        if (mc.player == null) return -1;
        AbstractContainerMenu handler = mc.player.containerMenu;

        if (handler instanceof InventoryMenu) return survivalInventory(i);
        if (handler instanceof CreativeModeInventoryScreen.ItemPickerMenu) return creativeInventory(i);
        if (handler instanceof ChestMenu chestMenu)
            return genericContainer(i, chestMenu.getRowCount());
        if (handler instanceof CraftingMenu) return craftingTable(i);
        if (handler instanceof FurnaceMenu) return furnace(i);
        if (handler instanceof BlastFurnaceMenu) return furnace(i);
        if (handler instanceof SmokerMenu) return furnace(i);
        if (handler instanceof DispenserMenu) return generic3x3(i);
        if (handler instanceof EnchantmentMenu) return enchantmentTable(i);
        if (handler instanceof BrewingStandMenu) return brewingStand(i);
        if (handler instanceof MerchantMenu) return villager(i);
        if (handler instanceof BeaconMenu) return beacon(i);
        if (handler instanceof AnvilMenu) return anvil(i);
        if (handler instanceof HopperMenu) return hopper(i);
        if (handler instanceof ShulkerBoxMenu) return genericContainer(i, 3);
        if (handler instanceof HorseInventoryMenu) return horse(handler, i);
        if (handler instanceof CartographyTableMenu) return cartographyTable(i);
        if (handler instanceof GrindstoneMenu) return grindstone(i);
        if (handler instanceof LecternMenu) return lectern();
        if (handler instanceof LoomMenu) return loom(i);
        if (handler instanceof StonecutterMenu) return stonecutter(i);
        if (handler instanceof CrafterMenu) return crafter(i);
        if (handler instanceof SmithingMenu) return smithingTable(i);

        return -1;
    }

    private static int survivalInventory(int i) {
        if (isHotbar(i)) return 36 + i;
        if (isArmor(i)) return 5 + (i - 36);
        if (i == OFFHAND) return 45;
        return i;
    }

    private static int creativeInventory(int i) {
        if (CreativeModeInventoryScreenAccessor.meteor$getSelectedTab() != BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabsAccessor.meteor$getInventory()))
            return -1;
        return survivalInventory(i);
    }

    private static int genericContainer(int i, int rows) {
        if (isHotbar(i)) return (rows + 3) * 9 + i;
        if (isMain(i)) return rows * 9 + (i - 9);
        return -1;
    }

    private static int craftingTable(int i) {
        if (isHotbar(i)) return 37 + i;
        if (isMain(i)) return i + 1;
        return -1;
    }

    private static int furnace(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int generic3x3(int i) {
        if (isHotbar(i)) return 36 + i;
        if (isMain(i)) return i;
        return -1;
    }

    private static int enchantmentTable(int i) {
        if (isHotbar(i)) return 29 + i;
        if (isMain(i)) return 2 + (i - 9);
        return -1;
    }

    private static int brewingStand(int i) {
        if (isHotbar(i)) return 32 + i;
        if (isMain(i)) return 5 + (i - 9);
        return -1;
    }

    private static int villager(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int beacon(int i) {
        if (isHotbar(i)) return 28 + i;
        if (isMain(i)) return 1 + (i - 9);
        return -1;
    }

    private static int anvil(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int hopper(int i) {
        if (isHotbar(i)) return 32 + i;
        if (isMain(i)) return 5 + (i - 9);
        return -1;
    }

    private static int horse(AbstractContainerMenu handler, int i) {
        LivingEntity entity = ((AbstractMountInventoryMenuAccessor) handler).meteor$getMount();

        if (entity instanceof Llama llamaEntity) {
            int strength = llamaEntity.getStrength();
            if (isHotbar(i)) return (2 + 3 * strength) + 28 + i;
            if (isMain(i)) return (2 + 3 * strength) + 1 + (i - 9);
        } else if (entity instanceof Horse || entity instanceof SkeletonHorse
            || entity instanceof ZombieHorse || entity instanceof Camel) {
            if (isHotbar(i)) return 29 + i;
            if (isMain(i)) return 2 + (i - 9);
        } else if (entity instanceof AbstractChestedHorse abstractDonkeyEntity) {
            boolean chest = abstractDonkeyEntity.hasChest();
            if (isHotbar(i)) return (chest ? 44 : 29) + i;
            if (isMain(i)) return (chest ? 17 : 2) + (i - 9);
        }

        return -1;
    }

    private static int cartographyTable(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int grindstone(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int lectern() {
        return -1;
    }

    private static int loom(int i) {
        if (isHotbar(i)) return 31 + i;
        if (isMain(i)) return 4 + (i - 9);
        return -1;
    }

    private static int stonecutter(int i) {
        if (isHotbar(i)) return 29 + i;
        if (isMain(i)) return 2 + (i - 9);
        return -1;
    }

    private static int crafter(int i) {
        if (isHotbar(i)) return 36 + i;
        if (isMain(i)) return i;
        return -1;
    }

    private static int smithingTable(int i) {
        if (isHotbar(i)) return 31 + i;
        if (isMain(i)) return 4 + (i - 9);
        return -1;
    }

    // Utils

    public static boolean isHotbar(int slotIndex) {
        return slotIndex >= HOTBAR_START && slotIndex <= HOTBAR_END;
    }

    public static boolean isMain(int slotIndex) {
        return slotIndex >= MAIN_START && slotIndex <= MAIN_END;
    }

    public static boolean isArmor(int slotIndex) {
        return slotIndex >= ARMOR_START && slotIndex <= ARMOR_END;
    }
}
