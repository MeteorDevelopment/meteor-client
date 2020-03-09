package minegame159.meteorclient.modules.combat;

import com.google.common.collect.Streams;
import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.entity.decoration.EnderCrystalEntity;
import net.minecraft.util.Hand;

import java.util.Comparator;

public class CrystalAura extends Module {
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

    public CrystalAura() {
        super(Category.Combat, "crystal-aura", "Attacks all crystals.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.player.getHealth() <= 0) return;

        Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof EnderCrystalEntity)
                .filter(entity -> entity.distanceTo(mc.player) <= range.get())
                .filter(entity -> ignoreWalls.get() || mc.player.canSee(entity))
                .min(Comparator.comparingDouble(o -> o.distanceTo(mc.player)))
                .ifPresent(entity -> {
                    mc.interactionManager.attackEntity(mc.player, entity);
                    mc.player.swingHand(Hand.MAIN_HAND);
                });
    }, EventPriority.HIGH);
}
