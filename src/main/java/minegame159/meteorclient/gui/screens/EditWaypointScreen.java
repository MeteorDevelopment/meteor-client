package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;
import net.minecraft.world.dimension.DimensionType;

public class EditWaypointScreen extends WindowScreen {
    private final Waypoint waypoint;
    private final boolean newWaypoint;

    public EditWaypointScreen(Waypoint waypoint) {
        super(waypoint == null ? "New Waypoint" : "Edit Waypoint", true);

        this.newWaypoint = waypoint == null;
        this.waypoint = newWaypoint ? new Waypoint() : waypoint;

        if (newWaypoint) {
            this.waypoint.x = (int) mc.player.getX();
            this.waypoint.y = (int) mc.player.getY() + 2;
            this.waypoint.z = (int) mc.player.getZ();

            if (mc.world.getRegistryKey().getValue().getPath().equals("overworld")) this.waypoint.overworld = true;
            else if (mc.world.getRegistryKey().getValue().getPath().equals("the_nether")) this.waypoint.nether = true;
            else if (mc.world.getRegistryKey().getValue().getPath().equals("the_end")) this.waypoint.end = true;
        }

        initWidgets();
    }

    private void initWidgets() {
        add(new WLabel("Name:"));
        add(new WTextBox(waypoint.name, 125)).getWidget().action = textBox -> waypoint.name = textBox.text;
        row();

        add(new WLabel("Icon:"));
        WTable iconTable = add(new WTable()).getWidget();
        iconTable.add(new WButton("Prev")).getWidget().action = button -> waypoint.prevIcon();
        iconTable.add(new WIcon());
        iconTable.add(new WButton("Next")).getWidget().action = button -> waypoint.nextIcon();
        row();

        add(new WLabel("Color:"));
        WTable colorTable = add(new WTable()).getWidget();
        colorTable.add(new WQuad(waypoint.color));
        colorTable.add(new WButton(GuiRenderer.TEX_EDIT)).getWidget().action = button -> mc.openScreen(new ColorSettingScreen(new ColorSetting("", "", waypoint.color, color -> waypoint.color.set(color), null)));
        row();

        add(new WHorizontalSeparator()).fillX().expandX();
        row();

        add(new WLabel("X:"));
        add(new WIntTextBox(waypoint.x, 50)).getWidget().action = textBox -> waypoint.x = textBox.value;
        row();

        add(new WLabel("Y:"));
        add(new WIntTextBox(waypoint.y, 50)).getWidget().action = textBox -> {
            if (textBox.value < 0) textBox.setValue(0);
            else if (textBox.value > 255) textBox.setValue(255);

            waypoint.y = textBox.value;
        };
        row();

        add(new WLabel("Z:"));
        add(new WIntTextBox(waypoint.z, 50)).getWidget().action = textBox -> waypoint.z = textBox.value;
        row();

        add(new WHorizontalSeparator()).fillX().expandX();
        row();

        add(new WLabel("Visible:"));
        add(new WCheckbox(waypoint.visible)).getWidget().action = checkbox -> waypoint.visible = checkbox.checked;
        row();

        add(new WLabel("Max Visible Distance:"));
        add(new WIntEdit(0, 10000, waypoint.maxVisibleDistance)).getWidget().action = intEdit -> waypoint.maxVisibleDistance = intEdit.get();
        row();

        add(new WLabel("Scale:"));
        add(new WDoubleEdit(0, 4, waypoint.scale)).getWidget().action = doubleEdit -> {
            doubleEdit.set(Math.round(doubleEdit.get() * 1000.0) / 1000.0);
            waypoint.scale = doubleEdit.get();
        };
        row();

        add(new WHorizontalSeparator()).fillX().expandX();
        row();

        add(new WLabel("Overworld:"));
        add(new WCheckbox(waypoint.overworld)).getWidget().action = checkbox -> waypoint.overworld = checkbox.checked;
        row();

        add(new WLabel("Nether:"));
        add(new WCheckbox(waypoint.nether)).getWidget().action = checkbox -> waypoint.nether = checkbox.checked;
        row();

        add(new WLabel("End:"));
        add(new WCheckbox(waypoint.end)).getWidget().action = checkbox -> waypoint.end = checkbox.checked;
        row();

        add(new WButton("Save")).fillX().expandX().getWidget().action = button -> {
            if (newWaypoint) Waypoints.INSTANCE.add(waypoint);
            else Waypoints.INSTANCE.save();

            onClose();
        };
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
