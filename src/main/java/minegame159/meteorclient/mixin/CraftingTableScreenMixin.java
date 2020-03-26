package minegame159.meteorclient.mixin;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.screens.AutoCraftScreen;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.client.gui.screen.ingame.CraftingTableScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.container.CraftingTableContainer;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingTableScreen.class)
public abstract class CraftingTableScreenMixin extends ContainerScreen<CraftingTableContainer> {
    private MinecraftClient mc;
    private ButtonWidget autoCraftBtn;

    private boolean autoCrafting;
    private int craftingI;
    private Item[] ingredients = new Item[9];
    private int timer;

    public CraftingTableScreenMixin(CraftingTableContainer container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        mc = MinecraftClient.getInstance();

        autoCrafting = false;
        craftingI = 0;

        autoCraftBtn = addButton(new ButtonWidget(x + 30 + 3 * 18 + 4, y + 16, 70, 13, "Auto craft", button -> onAutoCraft()));
        addButton(new ButtonWidget(x + 30 + 3 * 18 + 4, y + 17 + 2 * 20, 70, 13, "Config", button -> MinecraftClient.getInstance().openScreen(new AutoCraftScreen())));
    }

    private void onAutoCraft() {
        if (!autoCrafting) {
            if (getStack(0).isEmpty()) Utils.sendMessage("#blueAutoCraft: #whiteInvalid recipe.");
            else {
                autoCrafting = true;
                craftingI = 0;
                for (int i = 1; i < 10; i++) ingredients[i - 1] = getStack(i).getItem();

                autoCraftBtn.setMessage("Stop crafting");
            }
        } else {
            stopCrafting(null);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo info) {
        if (autoCrafting) {
            //if (TickRate.INSTANCE.getTimeSinceLastTick() > 0.5) return;

            timer++;

            //if (timer < 2) return;

            if (craftingI == 0) {
                mc.interactionManager.method_2906(container.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);

                if (!getStack(0).isEmpty()) {
                    stopCrafting("Stopped because your inventory is full.");
                    return;
                } else {
                    craftingI++;
                    timer = 0;
                }
            } else {
                if (getStack(craftingI).isEmpty()) {
                    if (findIngredients(craftingI)) {
                        if (Config.INSTANCE.autoCraft.stopWhenNoIngredients) {
                            stopCrafting("Stopped because you have ran out of ingredients.");
                        }
                    }
                }/* else {
                mc.interactionManager.method_2906(container.syncId, craftingI, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.method_2906(container.syncId, craftingI, 0, SlotActionType.PICKUP, mc.player);
            }*/

                craftingI++;
                if (craftingI >= 10) craftingI = 0;
                timer = 0;
            }
        }
    }

    private boolean findIngredients(int slot) {
        if (ingredients[slot - 1] == Items.AIR) return false;

        int ingredientSlot = -1;

        for (int i = 10; i < container.slots.size(); i++) {
            if (getStack(i).getItem() == ingredients[slot - 1]) {
                ingredientSlot = i;
                break;
            }
        }

        if (ingredientSlot == -1) return true;

        moveIngredients(ingredientSlot, slot);
        return false;
    }

    private void moveIngredients(int from, int to) {
        mc.interactionManager.method_2906(container.syncId, from, 0, SlotActionType.PICKUP, mc.player);

        if (Config.INSTANCE.autoCraft.craftByOne) {
            mc.interactionManager.method_2906(container.syncId, to, 1, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.method_2906(container.syncId, from, 0, SlotActionType.PICKUP, mc.player);
        } else {
            mc.interactionManager.method_2906(container.syncId, to, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    @Inject(method = "onMouseClick", at = @At("TAIL"))
    private void onMouseClick(Slot slot, int invSlot, int button, SlotActionType slotActionType, CallbackInfo info) {
        if (autoCrafting) stopCrafting(null);
    }

    private void stopCrafting(String msg) {
        if (msg != null) Utils.sendMessage("#blueAutoCraft: #white" + msg);
        autoCrafting = false;
        autoCraftBtn.setMessage("Auto craft");
    }

    private ItemStack getStack(int slot) {
        return container.getSlot(slot).getStack();
    }
}
