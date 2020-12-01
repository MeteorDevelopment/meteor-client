package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.FakePlayerEntity;

public class FakePlayer extends ToggleModule {

    public FakePlayer() {
        super(Category.Player, "fake-player", "Spawns a clientside fake player.");
    }

    private FakePlayerEntity fakePlayer;

    @Override
    public void onActivate() {
        fakePlayer = new FakePlayerEntity();
    }

    @Override
    public void onDeactivate() {
        fakePlayer.despawn();
    }
}