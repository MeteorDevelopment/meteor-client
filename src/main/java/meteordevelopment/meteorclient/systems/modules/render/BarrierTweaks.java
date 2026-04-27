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
    private final Setting<ListMode> mode = sgGeneral.add(new EnumSetting.Builder<BarrierTweaks.ListMode>()
        .name("mode")
        .description("The mode to use for displaying barriers.")
        .defaultValue(ListMode.Always)
        .build()
    );
    public final Setting<Integer> maxAge = sgGeneral.add(new IntSetting.Builder()
        .name("max-age")
        .description("Changes the delay in ticks of the particle being deleted after breaking the block.")
        .defaultValue(20)
        .min(0)
        .sliderMax(100)
        .build()
    );

    public BarrierTweaks() {
        super(Categories.Render, "barrier-tweaks", "Changes how barrier blocks are displayed.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (mode.get() == ListMode.WhenHoldingBarrier && !mc.player.getMainHandStack().isOf(Blocks.BARRIER.asItem())) return;

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

    public enum ListMode {
        Always,
        WhenHoldingBarrier
    }
}
