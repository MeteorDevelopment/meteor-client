/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.screens;

import meteordevelopment.meteorclient.events.meteor.HudElementBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;

import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class HudElementScreen extends WindowScreen {
    private final HudElement element;

    private WContainer settingsC1, settingsC2;
    private WKeybind keybind;
    private final Settings settings;

    public HudElementScreen(GuiTheme theme, HudElement element) {
        super(theme, element.info.title);

        this.element = element;

        settings = new Settings();
        SettingGroup sg = settings.createGroup("Anchors");
        sg.add(new BoolSetting.Builder()
            .name("auto-anchors")
            .description("Automatically assigns anchors based on the position.")
            .defaultValue(true)
            .onModuleActivated(booleanSetting -> booleanSetting.set(element.autoAnchors))
            .onChanged(aBoolean -> {
                if (aBoolean) element.box.updateAnchors();
                element.autoAnchors = aBoolean;
            })
            .build()
        );
        sg.add(new EnumSetting.Builder<XAnchor>()
            .name("x-anchor")
            .description("Horizontal anchor.")
            .defaultValue(XAnchor.Left)
            .visible(() -> !element.autoAnchors)
            .onModuleActivated(xAnchorSetting -> xAnchorSetting.set(element.box.xAnchor))
            .onChanged(element.box::setXAnchor)
            .build()
        );
        sg.add(new EnumSetting.Builder<YAnchor>()
            .name("y-anchor")
            .description("Vertical anchor.")
            .defaultValue(YAnchor.Top)
            .visible(() -> !element.autoAnchors)
            .onModuleActivated(yAnchorSetting -> yAnchorSetting.set(element.box.yAnchor))
            .onChanged(element.box::setYAnchor)
            .build()
        );
    }

    @Override
    public void initWidgets() {
        // Description
        add(theme.label(element.info.description, getWindowWidth() / 2.0));

        // Settings
        if (element.settings.sizeGroups() > 0) {
            element.settings.onActivated();

            settingsC1 = add(theme.verticalList()).expandX().widget();
            settingsC1.add(theme.settings(element.settings)).expandX();
        }

        // Anchors
        settings.onActivated();

        settingsC2 = add(theme.verticalList()).expandX().widget();
        settingsC2.add(theme.settings(settings)).expandX();

        // Bind
        WSection section = add(theme.section("Bind", true)).expandX().widget();

        // Keybind
        WHorizontalList bind = section.add(theme.horizontalList()).expandX().widget();

        bind.add(theme.label("Bind: "));
        keybind = bind.add(theme.keybind(element.keybind)).expandX().widget();
        keybind.actionOnSet = () -> Hud.get().setElementToBind(element);

        WButton reset = bind.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
        reset.action = keybind::resetBind;

        // Toggle on bind release
        WHorizontalList tobr = section.add(theme.horizontalList()).widget();

        tobr.add(theme.label("Toggle on bind release: "));
        WCheckbox tobrC = tobr.add(theme.checkbox(element.toggleOnBindRelease)).widget();
        tobrC.action = () -> element.toggleOnBindRelease = tobrC.checked;

        // Chat feedback
        WHorizontalList cf = section.add(theme.horizontalList()).widget();

        cf.add(theme.label("Chat Feedback: "));
        WCheckbox cfC = cf.add(theme.checkbox(element.chatFeedback)).widget();
        cfC.action = () -> element.chatFeedback = cfC.checked;

        add(theme.horizontalSeparator()).expandX();

        // Bottom
        WHorizontalList bottomList = add(theme.horizontalList()).expandX().widget();

        //   Active
        bottomList.add(theme.label("Active:"));
        WCheckbox active = bottomList.add(theme.checkbox(element.isActive())).widget();
        active.action = () -> {
            if (element.isActive() != active.checked) element.toggle();
        };

        //   Remove
        WMinus remove = bottomList.add(theme.minus()).expandCellX().right().widget();
        remove.action = () -> {
            element.remove();
            close();
        };
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !Hud.get().isBinding();
    }

    @Override
    public void tick() {
        super.tick();

        if (settingsC1 != null) {
            element.settings.tick(settingsC1, theme);
        }

        settings.tick(settingsC2, theme);
    }

    @Override
    protected void onRenderBefore(float delta) {
        HudEditorScreen.renderElements();
    }
    @EventHandler
    private void onElementBindChanged(HudElementBindChangedEvent event) {
        keybind.reset();
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(element.info.title, element.toTag());
    }

    @Override
    public boolean fromClipboard() {
        NbtCompound clipboard = NbtUtils.fromClipboard(element.toTag());

        if (clipboard != null) {
            element.fromTag(clipboard);
            return true;
        }

        return false;
    }

}
