package minegame159.meteorclient.mixininterface;

import net.minecraft.client.util.InputUtil;

public interface IKeyBinding {
    public void setPressed(boolean pressed);

    public InputUtil.KeyCode getKey();
}
