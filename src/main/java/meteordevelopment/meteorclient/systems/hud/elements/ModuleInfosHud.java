/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.List;

public class ModuleInfosHud extends HudElement {
    public static final HudElementInfo<ModuleInfosHud> INFO = new HudElementInfo<>(Hud.GROUP, "module-infos", "Displays if selected modules are enabled or disabled.", ModuleInfosHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("Which modules to display")
        .defaultValue(KillAura.class, CrystalAura.class, AnchorAura.class, BedAura.class, Surround.class)
        .build()
    );

    private final Setting<Boolean> additionalInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("additional-info")
        .description("Shows additional info from the module next to the name in the module info list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> textShadow = sgGeneral.add(new BoolSetting.Builder()
        .name("text-shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> moduleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("module-color")
        .description("Module color.")
        .defaultValue(new SettingColor())
        .build()
    );

    private final Setting<SettingColor> onColor = sgGeneral.add(new ColorSetting.Builder()
        .name("on-color")
        .description("Color when module is on.")
        .defaultValue(new SettingColor(25, 225, 25))
        .build()
    );

    private final Setting<SettingColor> offColor = sgGeneral.add(new ColorSetting.Builder()
        .name("off-color")
        .description("Color when module is off.")
        .defaultValue(new SettingColor(225, 25, 25))
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal alignment.")
        .defaultValue(Alignment.Auto)
        .build()
    );

    public ModuleInfosHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (Modules.get() == null || modules.get().isEmpty()) {
            renderer.text("Module Info", x, y, moduleColor.get(), textShadow.get());
            setSize(renderer.textWidth("Module Info"), renderer.textHeight());
            return;
        }

        double y = this.y;

        double width = 0;
        double height = 0;

        int i = 0;
        for (Module module : modules.get()) {
            double moduleWidth = renderer.textWidth(module.title) + renderer.textWidth(" ");
            String text = null;

            if (module.isActive()) {
                if (additionalInfo.get()) {
                    String info = module.getInfoString();
                    if (info != null) text = info;
                }

                if (text == null) text = "ON";
            }
            else text = "OFF";
            moduleWidth += renderer.textWidth(text);

            double x = this.x + alignX(moduleWidth, alignment.get());
            x = renderer.text(module.title, x, y, moduleColor.get(), textShadow.get());
            renderer.text(text, x + renderer.textWidth(" "), y, module.isActive() ? onColor.get() : offColor.get(), textShadow.get());
            y += renderer.textHeight() + 2;

            width = Math.max(width, moduleWidth);
            height += renderer.textHeight();
            if (i > 0) height += 2;

            i++;
        }

        setSize(width, height);
    }
}
