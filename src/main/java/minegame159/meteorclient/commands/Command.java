package minegame159.meteorclient.commands;

import net.minecraft.client.MinecraftClient;

public abstract class Command {
    protected static MinecraftClient MC;

    public final String name;
    public final String description;

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
        MC = MinecraftClient.getInstance();
    }

    public abstract void run(String[] args);
}
