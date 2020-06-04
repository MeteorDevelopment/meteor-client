package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Pool;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.item.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Color> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("Color.")
            .defaultValue(new Color(255, 150, 0))
            .build()
    );

    private final Pool<Vec3d> vec3ds = new Pool<>(() -> new Vec3d(0, 0, 0));
    private final List<Vec3d> path = new ArrayList<>();

    public Trajectories() {
        super(Category.Render, "trajectories", "Displays trajectory of holding items.");
    }

    @EventHandler
    private Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (!Utils.isThrowable(mc.player.getMainHandStack().getItem())) return;

        calculatePath(event.tickDelta);

        Vec3d lastPoint = null;
        for (Vec3d point : path) {
            if (lastPoint != null) ShapeBuilder.line(lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z, color.get());
            lastPoint = point;
        }
    });

    private void calculatePath(float tickDelta) {
        // Clear path and target
        for (Vec3d point : path) vec3ds.free(point);
        path.clear();

        Item item = mc.player.getMainHandStack().getItem();

        // Calculate starting position
        double x = mc.player.lastRenderX + (mc.player.x - mc.player.lastRenderX) * tickDelta - Math.cos(Math.toRadians(mc.player.yaw)) * 0.16;
        double y = mc.player.lastRenderY + (mc.player.y - mc.player.lastRenderY) * tickDelta + mc.player.getStandingEyeHeight() - 0.1;
        double z = mc.player.lastRenderZ + (mc.player.z - mc.player.lastRenderZ) * tickDelta - Math.sin(Math.toRadians(mc.player.yaw)) * 0.16;

        // Motion factor. Arrows go faster than snowballs and all that
        double velocityFactor = item instanceof RangedWeaponItem ? 1.0 : 0.4;

        double yaw = Math.toRadians(mc.player.yaw);
        double pitch = Math.toRadians(mc.player.pitch);

        // Calculate starting motion
        double velocityX = -Math.sin(yaw) * Math.cos(pitch) * velocityFactor;
        double velocityY = -Math.sin(pitch) * velocityFactor;
        double velocityZ = Math.cos(yaw) * Math.cos(pitch) * velocityFactor;

        // 3D Pythagorean theorem. Returns the length of the arrowMotion vector.
        double velocity = Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);

        velocityX /= velocity;
        velocityY /= velocity;
        velocityZ /= velocity;

        // Apply bow charge
        if(item instanceof RangedWeaponItem) {
            float bowPower = (72000 - mc.player.getItemUseTimeLeft()) / 20.0f;
            bowPower = (bowPower * bowPower + bowPower * 2.0f) / 3.0f;

            if(bowPower > 1 || bowPower <= 0.1F) bowPower = 1;

            bowPower *= 3F;
            velocityX *= bowPower;
            velocityY *= bowPower;
            velocityZ *= bowPower;

        } else {
            velocityX *= 1.5;
            velocityY *= 1.5;
            velocityZ *= 1.5;
        }

        double gravity = getProjectileGravity(item);
        Vec3d eyesPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);

        while (true) {
            // aAdd to path
            Vec3d pos = addToPath(x, y, z);

            // aApply motion
            x += velocityX * 0.1;
            y += velocityY * 0.1;
            z += velocityZ * 0.1;

            // aApply air friction
            velocityX *= 0.999;
            velocityY *= 0.999;
            velocityZ *= 0.999;

            // Apply gravity
            velocityY -= gravity * 0.1;

            // Check if chunk is loaded
            int chunkX = (int) (x / 16);
            int chunkZ = (int) (z / 16);
            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) break;

            // Check for collision
            RayTraceContext context = new RayTraceContext(eyesPos, pos, RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, mc.player);
            HitResult result = mc.world.rayTrace(context);
            if (result.getType() != HitResult.Type.MISS) break;
        }
    }

    private double getProjectileGravity(Item item) {
        if(item instanceof BowItem || item instanceof CrossbowItem) return 0.05;
        if(item instanceof PotionItem) return 0.4;
        if(item instanceof FishingRodItem) return 0.15;
        if(item instanceof TridentItem) return 0.015;

        return 0.03;
    }

    private Vec3d addToPath(double x, double y, double z) {
        Vec3d point = vec3ds.get();
        ((IVec3d) point).set(x, y, z);
        path.add(point);

        return point;
    }
}
