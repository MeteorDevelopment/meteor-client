package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IKeyBinding;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(KeyBinding.class)
public class KeyBindingMixin implements IKeyBinding {
    @Shadow private boolean pressed;

    @Shadow private InputUtil.Key boundKey;

    @Override
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    @Override
    public InputUtil.Key getBoundKey() {
        return boundKey;
    }
}
