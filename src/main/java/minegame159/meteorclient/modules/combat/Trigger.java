package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

public class Trigger extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> onlyWhenHoldingAttack = sgGeneral.add(new BoolSetting.Builder()
            .name("only-when-holding-attack")
            .description("Attacks only when you are holding left click.")
            .defaultValue(false)
            .build()
    );

    public Trigger() {
        super(Category.Combat, "trigger", "Automatically attacks when you look at entities.");
    }

    @EventHandler
    private Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mc.player.getHealth() <= 0 || mc.player.getAttackCooldownProgress(0.5f) < 1) return;
        if (!(mc.targetedEntity instanceof LivingEntity)) return;
        if (((LivingEntity) mc.targetedEntity).getHealth() <= 0) return;

        if (onlyWhenHoldingAttack.get()) {
            if (mc.options.keyAttack.isPressed()) attack();
        } else {
            attack();
        }
    });

    private void attack() {
        mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
