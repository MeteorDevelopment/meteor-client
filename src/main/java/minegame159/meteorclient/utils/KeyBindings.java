package minegame159.meteorclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;

public class KeyBindings {
    private static final GameOptions options = MinecraftClient.getInstance().options;

    public static final KeyBindingHandler use = new KeyBindingHandler(options.keyUse);

    public static final KeyBindingHandler forward = new KeyBindingHandler(options.keyForward);
    public static final KeyBindingHandler sprint = new KeyBindingHandler(options.keySprint);
}
