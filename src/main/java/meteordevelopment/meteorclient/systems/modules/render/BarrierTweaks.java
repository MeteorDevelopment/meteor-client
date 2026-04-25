package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.particle.Particle;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;

public class BarrierTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The radius to search for barrier blocks. WARNING: High values may lag")
        .defaultValue(32)
        .min(1)
        .sliderMax(64)
        .build()
    );

    public BarrierTweaks() {
        super(Categories.Render, "barrier-tweaks", "Displays barriers without distance-based flickering.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        BlockIterator.register(range.get(), range.get(), (pos, blockState) -> {
            if (blockState.isOf(Blocks.BARRIER)) {
                // To remove flickering, we spawn the particle every tick.
                Particle particle = mc.particleManager.addParticle(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.getDefaultState()),
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    0.0, 0.0, 0.0
                );
            }
        });
    }
}
