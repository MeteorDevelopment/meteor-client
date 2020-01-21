package minegame159.meteorclient.utils;

import net.minecraft.client.options.KeyBinding;

public class KeyBindingHandler {
    private KeyBinding keyBinding;

    public KeyBindingHandler(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
    }

    public boolean isPressed() {
        return Reflection.KeyBinding_pressed.get(keyBinding);
    }

    public void setPressed(boolean pressed) {
        Reflection.KeyBinding_pressed.set(keyBinding, pressed);
    }
}
