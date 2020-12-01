package minegame159.meteorclient.modules.player;

import com.google.common.collect.Lists;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.FakePlayerEntity;

import java.util.List;

public class FakePlayer extends ToggleModule {

    public FakePlayer() {
        super(Category.Player, "fake-player", "Spawns a clientside fake player.");
    }

    private final List<FakePlayerEntity> players = Lists.newArrayList();

    @Override
    public void onDeactivate() {
        clearFakePlayers();
    }

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();

        WButton spawn = table.add(new WButton("Spawn")).getWidget();
        spawn.action = () -> spawnFakePlayer();
        table.add(new WLabel("Spawns a FakePlayer."));

        WButton clear = table.add(new WButton("Clear")).getWidget();
        clear.action = () -> clearFakePlayers();
        table.add(new WLabel("Clears FakePlayers from the world."));

        return table;
    }

    public void spawnFakePlayer() {
        if (isActive()) {
            FakePlayerEntity fakeFlayer = new FakePlayerEntity();
            players.add(fakeFlayer);
        }
    }

    public void clearFakePlayers() {
        for (FakePlayerEntity player : players) {
            player.despawn();
        }
        players.clear();
    }
}