/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CrashReport.class)
public class CrashReportMixin {
    @Inject(method = "addStackTrace", at = @At("TAIL"))
    private void onAddStackTrace(StringBuilder sb, CallbackInfo info) {
        if (Modules.get() != null) {
            sb.append("\n\n");
            sb.append("<--- Meteor Client --->\n");
            sb.append("Version: ").append(MeteorClient.VERSION).append("\n");

            if (!MeteorClient.DEV_BUILD.isEmpty()) {
                sb.append("Dev Build: ").append(MeteorClient.DEV_BUILD).append("\n");
            }

            boolean modulesActive = false;
            for (Category category : Modules.loopCategories()) {
                List<Module> modules = Modules.get().getGroup(category);
                boolean active = false;

                for (Module module : modules) {
                    if (module != null && module.isActive()) {
                        active = true;
                        if (!modulesActive) {
                            modulesActive = true;
                            sb.append("\n-- Active Modules --\n");
                        }
                        break;
                    }
                }

                if (active) {
                    sb.append("\n[").append(category).append("]:\n");

                    for (Module module : modules) {
                        if (module != null && module.isActive()) {
                            sb.append(module.name).append("\n");
                        }
                    }
                }
            }

            if (Hud.get().active) {
                boolean hudActive = false;
                for (HudElement element : Hud.get()) {
                    if (!element.isActive()) continue;

                    if (!hudActive) {
                        hudActive = true;
                        sb.append("\n-- Active Hud Elements --\n");
                    }

                    sb.append(element.info.name).append("\n");
                }
            }
        }
    }
}
