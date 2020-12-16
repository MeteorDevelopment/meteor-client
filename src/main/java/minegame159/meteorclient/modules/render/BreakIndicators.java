package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.mixininterface.IClientPlayerInteractionManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import java.util.HashMap;
import java.util.Map;

public class BreakIndicators extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> multiple = sgGeneral.add(new BoolSetting.Builder()
            .name("multiple")
            .description("Renders block breaking from other players as well.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> hideVanillaIndicators = sgGeneral.add(new BoolSetting.Builder()
            .name("hide-vanilla-indicators")
            .description("Hides the vanilla (or resource-pack) break indicators.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> smoothAnim = sgGeneral.add(new BoolSetting.Builder()
            .name("smooth-animation")
            .description("Renders a smooth animation at block you break by yourself.")
            .defaultValue(true)
            .build()
    );


    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final Setting<Color> gradientColor1 = sgColors.add(new ColorSetting.Builder()
            .name("gradient-color-1")
            .description("The color for the non-broken block.")
            .defaultValue(new Color(25, 252, 25, 100))
            .build()
    );
    private final Setting<Color> gradientColor2 = sgColors.add(new ColorSetting.Builder()
            .name("gradient-color-2")
            .description("The color for the fully-broken block.")
            .defaultValue(new Color(255, 25, 25, 100))
            .build()
    );

    public final Map<Integer, BlockBreakingInfo> blocks = new HashMap<>();

    public BreakIndicators() {
        super(Category.Render, "break-indicators", "Renders the progress of a block being broken.");
    }

    @Override
    public void onDeactivate() {
        blocks.clear();
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        IClientPlayerInteractionManager iam;
        boolean smooth;
        if (smoothAnim.get()) {
            iam = (IClientPlayerInteractionManager) mc.interactionManager;
            BlockPos pos = iam.getCurrentBreakingBlockPos();
            smooth = pos != null && iam.getBreakingProgress() > 0;

            if(smooth && blocks.values().stream().noneMatch(info -> info.getPos().equals(pos)))
                blocks.put(mc.player.getEntityId(), new BlockBreakingInfo(mc.player.getEntityId(), pos));
        } else {
            iam = null;
            smooth = false;
        }

        blocks.values().forEach(info -> {
            BlockPos pos = info.getPos();
            int stage = info.getStage();

            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getOutlineShape(mc.world, pos);
            if (shape.isEmpty())
                return;
            Box orig = shape.getBoundingBox();
            Box box = orig;

            double shrinkFactor;
            if (smooth && iam.getCurrentBreakingBlockPos().equals(pos)) {
                shrinkFactor = 1d - iam.getBreakingProgress();
            } else {
                shrinkFactor = (9 - (stage + 1)) / 9d;
            }
            double progress = 1d - shrinkFactor;

            box = box.shrink(
                    box.getXLength() * shrinkFactor,
                    box.getYLength() * shrinkFactor,
                    box.getZLength() * shrinkFactor
            );

            double xShrink = (orig.getXLength() * shrinkFactor) / 2;
            double yShrink = (orig.getYLength() * shrinkFactor) / 2;
            double zShrink = (orig.getZLength() * shrinkFactor) / 2;

            double x1 = pos.getX() + box.minX + xShrink;
            double y1 = pos.getY() + box.minY + yShrink;
            double z1 = pos.getZ() + box.minZ + zShrink;
            double x2 = pos.getX() + box.maxX + xShrink;
            double y2 = pos.getY() + box.maxY + yShrink;
            double z2 = pos.getZ() + box.maxZ + zShrink;

            Color c1 = gradientColor1.get();
            Color c2 = gradientColor2.get();

            // gradient
            Color c = new Color(
                    (int) Math.round(c1.r + (c2.r - c1.r) * progress),
                    (int) Math.round(c1.g + (c2.g - c1.g) * progress),
                    (int) Math.round(c1.b + (c2.b - c1.b) * progress),
                    (int) Math.round(c1.a + (c2.a - c1.a) * progress)
            );
            Color edgesC = new Color(c);
            edgesC.a = (int) Math.min(edgesC.a * 1.5, 255d);
            ShapeBuilder.boxEdges(x1, y1, z1, x2, y2, z2,edgesC);
            ShapeBuilder.boxSides(x1, y1, z1, x2, y2, z2,c);
        });
    });
}
