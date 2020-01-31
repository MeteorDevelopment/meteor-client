package minegame159.meteorclient.modules.combat;

import com.google.common.collect.Streams;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.BoolSettingBuilder;
import minegame159.meteorclient.settings.builders.DoubleSettingBuilder;
import minegame159.meteorclient.settings.builders.EnumSettingBuilder;
import minegame159.meteorclient.utils.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

public class KillAura extends Module {
    public enum Priority {
        LowestDistance,
        HighestDistance,
        LowestHealth,
        HighestHealth
    }

    public Setting<Boolean> players = addSetting(new BoolSettingBuilder()
            .name("players")
            .description("Attack players.")
            .defaultValue(true)
            .build()
    );

    public Setting<Boolean> animals = addSetting(new BoolSettingBuilder()
            .name("animals")
            .description("Attack animals.")
            .defaultValue(true)
            .build()
    );

    public Setting<Boolean> mobs = addSetting(new BoolSettingBuilder()
            .name("mobs")
            .description("Attack mobs.")
            .defaultValue(true)
            .build()
    );

    public Setting<Double> range = addSetting(new DoubleSettingBuilder()
            .name("range")
            .description("Attack range.")
            .defaultValue(5.5)
            .min(0.0)
            .build()
    );

    public Setting<Boolean> ignoreWalls = addSetting(new BoolSettingBuilder()
            .name("ignore-walls")
            .description("Attack through walls.")
            .defaultValue(true)
            .build()
    );

    public Setting<Priority> priority = addSetting(new EnumSettingBuilder<Priority>()
            .name("priority")
            .description("What entities to target.")
            .defaultValue(Priority.LowestHealth)
            .build()
    );

    public KillAura() {
        super(Category.Combat, "kill-aura", "Automatically attacks entities.");
    }

    private boolean isInRange(Entity entity) {
        return entity.distanceTo(mc.player) <= range.value();
    }

    private boolean canAttackEntity(Entity entity) {
        if (entity.getUuid().equals(mc.player.getUuid())) return false;
        if (EntityUtils.isPlayer(entity) && players.value()) return true;
        if (EntityUtils.isAnimal(entity) && animals.value()) return true;
        return EntityUtils.isMob(entity) && mobs.value();
    }

    private boolean canSeeEntity(Entity entity) {
        return ignoreWalls.value() || mc.player.canSee(entity);
    }

    private int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }

    private int sort(LivingEntity e1, LivingEntity e2) {
        switch (priority.value()) {
            case LowestDistance:  return Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player));
            case HighestDistance: return invertSort(Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
            case LowestHealth:    return Float.compare(e1.getHealth(), e2.getHealth());
            case HighestHealth:   return invertSort(Float.compare(e1.getHealth(), e2.getHealth()));
            default:              return 0;
        }
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.player.getHealth() <= 0 || mc.player.getAttackCooldownProgress(0.5f) < 1) return;

        Streams.stream(mc.world.getEntities())
                .filter(this::isInRange)
                .filter(this::canAttackEntity)
                .filter(this::canSeeEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity.getHealth() > 0)
                .min(this::sort)
                .ifPresent(entity -> {
                    mc.interactionManager.attackEntity(mc.player, entity);
                    mc.player.swingHand(Hand.MAIN_HAND);
                });
    });
}
