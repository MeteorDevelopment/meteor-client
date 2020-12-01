package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.utils.Chat;

public class FakePlayerCommand extends Command {
    public FakePlayerCommand(){
        super("fakeplayer", "Enchants the currently held item with almost every enchantment (must be in creative)");
    }

    @Override
    public void run(String[] args) {

        if (!ModuleManager.INSTANCE.get(FakePlayer.class).isActive()) {
            Chat.error("The FakePlayer module must be enabled to use this command.");
            return;
        }

        if (args.length == 0) {
            Chat.error("Please include an argument. Use \"spawn\" to spawn a new fakeplayer or \"clear\" to clear all fakeplayers from the world.");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "spawn":
                ModuleManager.INSTANCE.get(FakePlayer.class).spawnFakePlayer();
                break;
            case "clear":
                ModuleManager.INSTANCE.get(FakePlayer.class).clearFakePlayers();
                break;
            default:
                Chat.error("Invalid argument.");
                break;
        }
    }
}