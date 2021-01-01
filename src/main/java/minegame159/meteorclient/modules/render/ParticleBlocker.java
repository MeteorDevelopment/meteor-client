package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.ParticleEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.ParticleEffectListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.particle.ParticleEffect;

import java.util.ArrayList;
import java.util.List;

public class ParticleBlocker extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<ParticleEffect>> particles = sgGeneral.add(new ParticleEffectListSetting.Builder()
            .name("particles")
            .description("Particles to block.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    public ParticleBlocker() {
        super(Category.Render, "particle-blocker", "Stops specified particles from rendering.");
    }

    @EventHandler
    private final Listener<ParticleEvent> onRenderParticle = new Listener<>(event -> {
        if (event.particle != null && particles.get().contains(event.particle)) event.cancel();
    });
}
