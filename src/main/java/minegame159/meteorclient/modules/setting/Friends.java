package minegame159.meteorclient.modules.setting;

import minegame159.meteorclient.altsfriends.FriendsScreen;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class Friends extends Module {
    public Friends() {
        super(Category.Setting, "friends", "Friend list.", true, true, false);
    }

    @Override
    public WidgetScreen getCustomScreen() {
        return new FriendsScreen();
    }
}
