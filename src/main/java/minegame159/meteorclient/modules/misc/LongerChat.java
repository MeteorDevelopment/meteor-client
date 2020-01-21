package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;

public class LongerChat extends Module {
    public static boolean active;
    public static IntSetting lines = new IntSetting("lines", "Chat lines.", 1000, 1, null);

    public LongerChat() {
        super(Category.Misc, "longer-chat", "Makes chat longer.", lines);
    }

    @Override
    public void onActivate() {
        active = true;
    }

    @Override
    public void onDeactivate() {
        active = false;
    }
}
