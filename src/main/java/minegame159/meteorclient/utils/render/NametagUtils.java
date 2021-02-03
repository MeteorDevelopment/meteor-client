package minegame159.meteorclient.utils.render;

import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;

public class NametagUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void begin(RenderEvent event, Entity entity, double scale) {
        begin(
                event,
                entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta,
                entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta + entity.getHeight() + 0.25,
                entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta,
                scale,
                Utils.distanceToCamera(entity)
        );
    }

    public static void begin(RenderEvent event, double x, double y, double z, double scale, double distance) {
        Camera camera = mc.gameRenderer.getCamera();
        double s = 0.01 * scale;
        if (distance > 8) s *= distance / 8;

        Matrices.push();
        Matrices.translate(x - event.offsetX, y - event.offsetY, z - event.offsetZ);
        Matrices.rotate(-camera.getYaw(), 0, 1, 0);
        Matrices.rotate(camera.getPitch(), 1, 0, 0);
        Matrices.scale(-s, -s, s);
    }

    public static void end() {
        Matrices.pop();
    }
}
