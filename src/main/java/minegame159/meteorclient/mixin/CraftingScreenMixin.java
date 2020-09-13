package minegame159.meteorclient.mixin;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.screens.AutoCraftScreen;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends HandledScreen<CraftingScreenHandler> implements RecipeBookProvider {
    @Shadow @Final private RecipeBookWidget recipeBook;
    @Shadow private boolean narrow;
    private MinecraftClient mc;
    private ButtonWidget autoCraftBtn;
    private ButtonWidget configBtn;

    private boolean autoCrafting;
    private int craftingI;
    private Item[] ingredients = new Item[9];
    private int timer;

    public CraftingScreenMixin(CraftingScreenHandler container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        mc = MinecraftClient.getInstance();

        autoCrafting = false;
        craftingI = 0;

        autoCraftBtn = addButton(new ButtonWidget(x + 30 + 3 * 18 + 4, y + 16, 70, 13, new LiteralText("Auto craft"), button -> onAutoCraft()));
        configBtn = addButton(new ButtonWidget(x + 30 + 3 * 18 + 4, y + 17 + 2 * 20, 70, 13, new LiteralText("Config"), button -> MinecraftClient.getInstance().openScreen(new AutoCraftScreen())));
    }

    private void onAutoCraft() {
        if (!autoCrafting) {
            if (getStack(0).isEmpty()) Chat.error("Invalid recipe.");
            else {
                autoCrafting = true;
                craftingI = 0;
                for (int i = 1; i < 10; i++) ingredients[i - 1] = getStack(i).getItem();

                autoCraftBtn.setMessage(new LiteralText("Stop crafting"));
            }
        } else {
            stopCrafting(null);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo info) {
        int x = recipeBook.findLeftEdge(this.narrow, this.width, this.backgroundWidth) + 30 + 3 * 18 + 4;
        autoCraftBtn.x = x;
        configBtn.x = x;

        if (autoCrafting) {
            //if (TickRate.INSTANCE.getTimeSinceLastTick() > 0.5) return;

            timer++;

            //if (timer < 2) return;

            if (craftingI == 0) {
                InvUtils.clickSlot(0, 0, SlotActionType.QUICK_MOVE);

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
                        if (Config.INSTANCE.autoCraft.isStopWhenNoIngredients()) {
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

        for (int i = 10; i < handler.slots.size(); i++) {
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
        InvUtils.clickSlot(from, 0, SlotActionType.PICKUP);

        if (Config.INSTANCE.autoCraft.isCraftByOne()) {
            InvUtils.clickSlot(to, 1, SlotActionType.PICKUP);
            InvUtils.clickSlot(from, 0, SlotActionType.PICKUP);
        } else {
            InvUtils.clickSlot(to, 0, SlotActionType.PICKUP);
        }
    }

    @Inject(method = "onMouseClick", at = @At("TAIL"))
    private void onMouseClick(Slot slot, int invSlot, int button, SlotActionType slotActionType, CallbackInfo info) {
        if (autoCrafting) stopCrafting(null);
    }

    private void stopCrafting(String msg) {
        if (msg != null) Chat.info(msg);
        autoCrafting = false;
        autoCraftBtn.setMessage(new LiteralText("Auto craft"));
    }

    private ItemStack getStack(int slot) {
        ItemStack itemStack = handler.getSlot(slot).getStack();
        return itemStack == null ? ItemStack.EMPTY : itemStack;
    }
}
