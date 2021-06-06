/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.EntityAddedEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

import static minegame159.meteorclient.utils.player.ChatUtils.formatCoords;

public class EntityLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entites")
            .description("Select specific entities.")
            .defaultValue(new Object2BooleanOpenHashMap<>(0))
            .build()
    );

    private final Setting<Boolean> playerNames = sgGeneral.add(new BoolSetting.Builder()
            .name("player-names")
            .description("Shows the player's name.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("friends")
            .description("Logs friends.")
            .defaultValue(true)
            .build()
    );

    public EntityLogger() {
        super(Categories.World, "entity-logger", "Sends a client-side chat alert if a specified entity appears in render distance.");
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity.getUuid().equals(mc.player.getUuid())) return;

        if (entities.get().getBoolean(event.entity.getType())) {
            if (event.entity instanceof PlayerEntity) {
                if (!friends.get() && Friends.get().get((PlayerEntity) event.entity) != null) return;
            }

            String name;
            if (playerNames.get() && event.entity instanceof PlayerEntity) name = event.entity.getEntityName() + " (Player)";
            else name = event.entity.getType().getName().getString();

            MutableText text = new LiteralText(name + " ").formatted(Formatting.WHITE);
            text.append(new LiteralText(" has spawned at ").formatted(Formatting.GRAY));
            text.append(formatCoords(event.entity.getPos()));
            text.append(new LiteralText(".").formatted(Formatting.GRAY));
            info(text);
        }
    }
}
