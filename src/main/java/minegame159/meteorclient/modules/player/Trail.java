package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.ParticleEffectListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.particle.ParticleEffect;

import java.util.ArrayList;
import java.util.List;

public class Trail extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<ParticleEffect>> particles = sgGeneral.add(new ParticleEffectListSetting.Builder()
            .name("particles")
            .description("Particles to draw.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> pause = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-when-stationary")
            .description("Whether or not to add particles when you are not moving.")
            .defaultValue(true)
            .build()
    );


    public Trail() {
        super(Category.Render, "trail", "Renders a trail behind your player.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (pause.get() && mc.player.input.movementForward == 0f && mc.player.input.movementSideways == 0f && !mc.options.keyJump.isPressed()) return;
        for (ParticleEffect particleEffect : particles.get()) {
            mc.world.addParticle(particleEffect, mc.player.getX(), mc.player.getY(), mc.player.getZ(), 0, 0, 0);
        }
    });
}
