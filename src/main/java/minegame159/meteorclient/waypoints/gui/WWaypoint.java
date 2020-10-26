package minegame159.meteorclient.waypoints.gui;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Dimension;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;
import net.minecraft.client.MinecraftClient;

public class WWaypoint extends WTable {
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color GRAY = new Color(200, 200, 200);

    public WWaypoint(Waypoint waypoint) {
        // Icon
        add(new WWaypointIcon(waypoint));

        // Name
        WLabel name = add(new WLabel(waypoint.name)).getWidget();
        boolean goodDimension = false;
        Dimension dimension = Utils.getDimension();
        if (waypoint.overworld && dimension == Dimension.Overworld) goodDimension = true;
        else if (waypoint.nether && dimension == Dimension.Nether) goodDimension = true;
        else if (waypoint.end && dimension == Dimension.End) goodDimension = true;
        name.color = goodDimension ? WHITE : GRAY;

        // Visible, edit, remove
        WTable right = add(new WTable()).fillX().right().getWidget();
        WCheckbox visible = right.add(new WCheckbox(waypoint.visible)).getWidget();
        visible.action = () -> {
            waypoint.visible = visible.checked;
            Waypoints.INSTANCE.save();
        };
        right.add(new WButton(WButton.ButtonRegion.Edit)).getWidget().action = () -> MinecraftClient.getInstance().openScreen(new EditWaypointScreen(waypoint));
        right.add(new WMinus()).getWidget().action = () -> Waypoints.INSTANCE.remove(waypoint);
    }
}
