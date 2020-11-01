package minegame159.meteorclient.modules.movement;

//Created by squidoodly 10/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IHorseBaseEntity;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseBaseEntity;

public class EntityControl extends ToggleModule {
    public EntityControl(){super(Category.Movement, "entity-control", "Lets you control horses, donkeys and mules without a saddle.");}

    @Override
    public void onDeactivate() {
        mc.world.getEntities().forEach(entity -> {
            if (entity instanceof HorseBaseEntity) {
                ((IHorseBaseEntity) entity).setSaddled(false);
            }
        });
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        mc.world.getEntities().forEach(entity -> {
            if (entity instanceof HorseBaseEntity) {
                ((IHorseBaseEntity) entity).setSaddled(true);
            }
        });

        Entity entity = mc.player.getVehicle();
        if (entity == null) return;

        if (entity instanceof HorseBaseEntity) {
            ((HorseBaseEntity) entity).setAiDisabled(true);
            ((HorseBaseEntity) entity).setTame(true);
        }
    });
}
