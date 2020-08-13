package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.container.ShulkerBoxContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class Peek extends Command {
    private static ItemStack[] ITEMS = new ItemStack[27];

    public Peek() {
        super("peek", "Lets you see whats inside shulker boxes.");
    }

    @Override
    public void run(String[] args) {
        PlayerEntity player = MinecraftClient.getInstance().player;

        ItemStack itemStack;
        if (Utils.isShulker(player.getMainHandStack().getItem())) itemStack = player.getMainHandStack();
        else if (Utils.isShulker(player.getOffHandStack().getItem())) itemStack = player.getOffHandStack();
        else {
            Chat.error("You must be holding a shulker box.");
            return;
        }

        Utils.getItemsInContainerItem(itemStack, ITEMS);
        MeteorClient.INSTANCE.screenToOpen = new PeekShulkerBoxScreen(new ShulkerBoxContainer(0, player.inventory, new BasicInventory(ITEMS)), player.inventory, itemStack.getName());
    }

    private static class PeekShulkerBoxScreen extends ShulkerBoxScreen {
        public PeekShulkerBoxScreen(ShulkerBoxContainer shulkerBoxContainer, PlayerInventory playerInventory, Text text) {
            super(shulkerBoxContainer, playerInventory, text);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return false;
        }
    }
}
