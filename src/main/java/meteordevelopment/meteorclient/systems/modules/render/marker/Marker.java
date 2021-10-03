/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;

public class Marker extends Module {
    public final Markers markers;
    private ArrayList<BaseMarker> markerList = new ArrayList<>();

    public Marker() {
        super(Categories.Render, "marker", "Renders shapes. Useful for large scale projects");

        markers = new Markers();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (BaseMarker marker : markerList) if (marker.isActive()) marker.tick();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (BaseMarker marker : markerList) if (marker.isActive()) marker.render(event);
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        for (BaseMarker marker : markerList) {
            if (marker.isActive()) {
                if (event.action == KeyAction.Press) marker.onKeyPress(event.key);
                else if (event.action == KeyAction.Release) marker.onKeyRelease(event.key);
            }
        }
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        NbtList list = new NbtList();
        markerList.forEach(marker -> {
            NbtCompound mTag = new NbtCompound();
            mTag.putString("type", marker.getTypeName());
            mTag.put("marker", marker.toTag());

            list.add(mTag);
        });

        tag.put("markers", list);
        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);

        NbtList list = tag.getList("markers", 9);
        markerList.clear();
        for (NbtElement tagII : list) {
            NbtCompound tagI = (NbtCompound) tagII;

            String type = tagI.getString("type");
            BaseMarker marker = markers.createMarker(type);

            if (marker != null) {
                NbtCompound markerTag = (NbtCompound) tagI.get("marker");
                if (markerTag != null) marker.fromTag(markerTag);

                markerList.add(marker);
            }
        }

        return this;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillList(theme, list);
        return list;
    }

    protected void fillList(GuiTheme theme, WVerticalList list) {
        // Marker List
        for (BaseMarker marker : markerList) {
            WHorizontalList hList = list.add(theme.horizontalList()).expandX().widget();

            // Name
            WLabel label = hList.add(theme.label(marker.name.get())).expandX().widget();
            label.tooltip = marker.description.get();

            // Toggle
            WCheckbox checkbox = hList.add(theme.checkbox(marker.isActive())).widget();
            checkbox.action = () -> {
                if (marker.isActive() != checkbox.checked) marker.toggle();
            };

            // Edit
            WButton edit = hList.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> {
                mc.setScreen(marker.getScreen(theme));
            };

            // Remove
            WMinus remove = hList.add(theme.minus()).widget();
            remove.action = () -> {
                markerList.remove(marker);
                list.clear();
                fillList(theme, list);
            };
        }

        // Bottom
        WHorizontalList bottom = list.add(theme.horizontalList()).expandX().widget();

        String[] names = new String[markers.getNames().size()];
        markers.getNames().toArray(names);
        WDropdown<String> newMarker = bottom.add(theme.dropdown(names, markers.getNames().get(0))).widget();
        WButton add = bottom.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            String name = newMarker.get();
            markerList.add(markers.createMarker(name));

            list.clear();
            fillList(theme, list);
        };

    }
}
