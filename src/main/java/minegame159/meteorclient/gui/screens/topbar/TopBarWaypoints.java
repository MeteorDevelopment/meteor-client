package minegame159.meteorclient.gui.screens.topbar;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.WaypointListChangedEvent;
import minegame159.meteorclient.gui.TopBarType;
import minegame159.meteorclient.gui.screens.EditWaypointScreen;
import minegame159.meteorclient.gui.widgets.WPlus;
import minegame159.meteorclient.gui.widgets.WWaypoint;
import minegame159.meteorclient.gui.widgets.WWindow;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;

public class TopBarWaypoints extends TopBarScreen implements Listenable {
    private final WWindow window;

    public TopBarWaypoints() {
        super(TopBarType.Waypoints);

        window = add(new WWindow(title, true)).centerXY().getWidget();

        initWidgets();
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void initWidgets() {
        boolean hasWaypoints = false;
        for (Waypoint waypoint : Waypoints.INSTANCE) {
            window.add(new WWaypoint(waypoint)).fillX().expandX();
            window.row();

            hasWaypoints = true;
        }

        if (Utils.canUpdate()) {
            if (!hasWaypoints) window.row();

            WPlus plus = window.add(new WPlus()).fillX().right().getWidget();
            plus.action = plus1 -> mc.openScreen(new EditWaypointScreen(null));
        }
    }

    @EventHandler
    private final Listener<WaypointListChangedEvent> onWaypointListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void clear() {
        window.clear();
    }

    @Override
    public void onClose() {
        super.onClose();
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }
}
