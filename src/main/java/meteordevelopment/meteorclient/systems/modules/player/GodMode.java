/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;

public class GodMode extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> fullFood = sgGeneral.add(new BoolSetting.Builder()
        .name("full-food")
        .description("Sets the food level client-side to max.")
        .defaultValue(true)
        .build()
    );

    public GodMode() {
        super(Categories.Player, "god-mode",
            "Allows you to keep playing after you die. Works on Forge, Fabric and Vanilla servers."
        );
    }

    private boolean active = false;

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        this.active = false;
        this.warning("You are no longer in a god like mode!");
        if (this.mc.player != null && this.mc.player.networkHandler != null) {
            this.mc.player.requestRespawn();
            this.info("Respawn request has been sent to the server.");
        }
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        this.active = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.mc.player.getHealth() < 1f) this.mc.player.setHealth(20f);
        if (this.fullFood.get() && this.mc.player.getHungerManager().getFoodLevel() < 20) {
            this.mc.player.getHungerManager().setFoodLevel(20);
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof DeathScreen) {
            event.cancel();
            if (!this.active) {
                this.active = true;
                this.info(
                    "You are now in a god like mode. " +
                    "From now on you'll have to reconnect to break/place blocks, " +
                    "you can't collect xp or items from ground and " +
                    "you'll be invisible on some servers too."
                );
            }
        }
    }

}
