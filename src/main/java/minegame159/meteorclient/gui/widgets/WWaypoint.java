package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.screens.EditWaypointScreen;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.dimension.DimensionType;

public class WWaypoint extends WTable {
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color GRAY = new Color(200, 200, 200);

    private final Waypoint waypoint;

    public WWaypoint(Waypoint waypoint) {
        this.waypoint = waypoint;

        add(new WIcon());

        WLabel name = add(new WLabel(waypoint.name)).getWidget();
        boolean goodDimension = false;
        DimensionType dimension = MinecraftClient.getInstance().player.dimension;
        if (waypoint.overworld && dimension == DimensionType.OVERWORLD) goodDimension = true;
        else if (waypoint.nether && dimension == DimensionType.THE_NETHER) goodDimension = true;
        else if (waypoint.end && dimension == DimensionType.THE_END) goodDimension = true;
        name.color = goodDimension ? WHITE : GRAY;

        add(new WCheckbox(waypoint.visible)).getWidget().action = checkbox -> {
            waypoint.visible = checkbox.checked;
            Waypoints.INSTANCE.save();
        };

        WTable right = add(new WTable()).fillX().right().getWidget();
        right.add(new WButton(GuiRenderer.TEX_EDIT)).getWidget().action = button -> MinecraftClient.getInstance().openScreen(new EditWaypointScreen(waypoint));
        right.add(new WMinus()).getWidget().action = minus -> Waypoints.INSTANCE.remove(waypoint);
    }

    private class WIcon extends WWidget {
        @Override
        protected void onCalculateSize() {
            width = 16;
            height = 16;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.addCustomPostOperation(() -> waypoint.renderIcon(x, y, 0));
        }
    }
}
