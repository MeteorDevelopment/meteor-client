package minegame159.meteorclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;

public class Reflection {
    public static final ReflectionField<Integer> MinecraftClient_currentFps = new ReflectionField<>(MinecraftClient.class, "currentFps");

    public static final ReflectionField<Boolean> KeyBinding_pressed = new ReflectionField<>(KeyBinding.class, "pressed");
}
