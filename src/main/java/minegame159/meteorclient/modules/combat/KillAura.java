package minegame159.meteorclient.modules.combat;

import com.google.common.collect.Streams;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.altsfriends.FriendManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class KillAura extends Module {
    public enum Priority {
        LowestDistance,
        HighestDistance,
        LowestHealth,
        HighestHealth
    }

    public Setting<Boolean> players = addSetting(new BoolSetting.Builder()
            .name("players")
            .description("Attack players.")
            .defaultValue(true)
            .build()
    );

    public Setting<Boolean> friends = addSetting(new BoolSetting.Builder()
            .name("friends")
            .description("Attack friends, useful only if attack friends is on.")
            .defaultValue(false)
            .build()
    );

    public Setting<Boolean> animals = addSetting(new BoolSetting.Builder()
            .name("animals")
            .description("Attack animals.")
            .defaultValue(true)
            .build()
    );

    public Setting<Boolean> mobs = addSetting(new BoolSetting.Builder()
            .name("mobs")
            .description("Attack mobs.")
            .defaultValue(true)
            .build()
    );

    public Setting<Double> range = addSetting(new DoubleSetting.Builder()
            .name("range")
            .description("Attack range.")
            .defaultValue(5.5)
            .min(0.0)
            .build()
    );

    public Setting<Boolean> ignoreWalls = addSetting(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Attack through walls.")
            .defaultValue(true)
            .build()
    );

    public Setting<Priority> priority = addSetting(new EnumSetting.Builder<Priority>()
            .name("priority")
            .description("What entities to target.")
            .defaultValue(Priority.LowestHealth)
            .build()
    );

    public KillAura() {
        super(Category.Combat, "kill-aura", "Automatically attacks entities.");
    }

    private boolean isInRange(Entity entity) {
        return entity.distanceTo(mc.player) <= range.get();
    }

    private boolean canAttackEntity(Entity entity) {
        if (entity.getUuid().equals(mc.player.getUuid())) return false;
        if (EntityUtils.isPlayer(entity) && players.get()) {
            if (!friends.get()) return true;
            return !FriendManager.INSTANCE.contains((PlayerEntity) entity);
        }
        if (EntityUtils.isAnimal(entity) && animals.get()) return true;
        return EntityUtils.isMob(entity) && mobs.get();
    }

    private boolean canSeeEntity(Entity entity) {
        return ignoreWalls.get() || mc.player.canSee(entity);
    }

    private int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }

    private int sort(LivingEntity e1, LivingEntity e2) {
        switch (priority.get()) {
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
