/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WMinus;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.macros.EditMacroScreen;
import minegame159.meteorclient.macros.Macro;
import minegame159.meteorclient.macros.Macros;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;

public class TopBarMacros extends TopBarWindowScreen {
    public TopBarMacros() {
        super(TopBarType.Macros);

        refreshWidgetsOnInit = true;
    }

    @Override
    protected void initWidgets() {
        // Macros
        if (Macros.get().getAll().size() > 0) {
            WTable t = add(new WTable()).getWidget();

            for (Macro macro : Macros.get()) {
                t.add(new WLabel(macro.name + " (" + Utils.getKeyName(macro.key) + ")"));

                WButton edit = t.add(new WButton(WButton.ButtonRegion.Edit)).getWidget();
                edit.action = () -> MinecraftClient.getInstance().openScreen(new EditMacroScreen(macro));

                WMinus remove = t.add(new WMinus()).getWidget();
                remove.action = () -> {
                    Macros.get().remove(macro);

                    clear();
                    initWidgets();
                };

                t.row();
            }
            row();
        }

        // Create macro
        WButton create = add(new WButton("Create")).fillX().expandX().getWidget();
        create.action = () -> MinecraftClient.getInstance().openScreen(new EditMacroScreen(null));
    }
}
