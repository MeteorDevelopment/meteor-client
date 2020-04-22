package minegame159.meteorclient.modules.setting;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.screens.FriendsScreen;

public class Friends extends Module {
    public Friends() {
        super(Category.Setting, "friends", "Friend list.");
        serialize = false;
    }

    @Override
    public WidgetScreen getScreen() {
        return new FriendsScreen();
    }
}
