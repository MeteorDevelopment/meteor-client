package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.Arrays;
import java.util.List;

public class AntiHazard extends Module {
	
    public enum Mode {
        Packet,
        Vanilla
    }
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private boolean Hazard = false;

    private final Setting<Sneak.Mode> mode = sgGeneral.add(new EnumSetting.Builder<Sneak.Mode>()
        .name("mode")
        .description("Which method to sneak.")
        .defaultValue(Sneak.Mode.Vanilla)
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Which blocks to sneak on")
        .defaultValue(Arrays.asList(Blocks.MAGMA_BLOCK))
        .build()
    );

    public AntiHazard() {
        super(Categories.Movement, "anti-hazard", "Sneak on magma.");
    }

    public boolean doPacket() {
        return isActive() && Hazard && !Modules.get().isActive(Freecam.class) && mode.get() == Sneak.Mode.Packet;
    }

    public boolean doVanilla() {
        return isActive() && Hazard && !Modules.get().isActive(Freecam.class) && mode.get() == Sneak.Mode.Vanilla;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        Vec3d PredPos = mc.player.getPos()
            .add(mc.player.getVelocity().normalize().multiply(0.6, 0, 0.6))
            .add(0, -0.5, 0);

        BlockState blockState = mc.world.getBlockState(new BlockPos(PredPos.x, PredPos.y, PredPos.z));
        Hazard =
            blocks.get().contains(blockState.getBlock()) ||
            blocks.get().contains(mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock());
    }
}