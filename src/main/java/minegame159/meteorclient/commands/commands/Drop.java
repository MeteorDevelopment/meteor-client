package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.container.SlotActionType;

public class Drop extends Command {
    public Drop() {
        super("drop", "Drops things.");
    }

    @Override
    public void run(String[] args) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player.isSpectator()) {
            Chat.error("Can't drop items while in spectator.");
            return;
        }

        if (args.length == 0) {
            sendErrorMessage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "hand":
                mc.player.dropSelectedItem(true);
                break;
            case "offhand":
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(InvUtils.OFFHAND_SLOT), 1, SlotActionType.THROW);
                break;
            case "hotbar":
                for (int i = 0; i < 9; i++) {
                    InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                }
                break;
            case "inventory":
                for (int i = 9; i < mc.player.inventory.main.size(); i++) {
                    InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                }
                break;
            case "all":
                for (int i = 0; i < mc.player.inventory.main.size(); i++) {
                    InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                }
                break;
            default:
                sendErrorMessage();
                break;
        }
    }

    private void sendErrorMessage() {
        Chat.error("You need to select a mode. (hand, offhand, hotbar, inventory, all)");
    }
}
