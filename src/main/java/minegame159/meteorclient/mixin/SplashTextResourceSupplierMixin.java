/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApply(List<String> list, ResourceManager resourceManager, Profiler profiler, CallbackInfo info) {
        list.add("Meteor on Crack!");
        list.add("Star Meteor Client on GitHub!");
        list.add("All hail Meteor!");
        list.add("NO HACKS");
        list.add("Based utility mod.");
        list.add("based");
        list.add("omg-skidded-client");
        list.add("no optifine, use sodium");
        list.add("#info");
        list.add("Bruh");
        list.add(":bruh:");
        list.add(":EZ:");
        list.add(":kekw:");
        list.add("don't say the n word or muted");
        list.add("OK :monkey:");
        list.add("OK retard.");
        list.add("Auto Minecraft");
        list.add("Where is auto mount bypass dupe?!?@?");
        list.add("popbob sex dupe");
        list.add(":yes:");
        list.add(".cat");
        list.add(".snale");
        list.add(".monkey");
        list.add(":widesnail:");
        list.add(":monkey:");
        list.add("minegame159, where is he going");
        list.add("bigrat.monster");
        list.add("snale moment");
        list.add("inertia moment");
    }
}
