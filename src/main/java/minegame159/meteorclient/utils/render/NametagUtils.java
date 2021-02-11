package minegame159.meteorclient.utils.render;

import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.mixininterface.IMatrix4f;
import minegame159.meteorclient.mixininterface.IQuaternion;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

import static org.lwjgl.opengl.GL11.*;

public class NametagUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Quaternion quat = new Quaternion(0, 0, 0, 0);

    private static Matrix4f model;
    private static Matrix4f projection;
    private static Vec3d camera, cameraNegated;
    private static double windowScale;

    private static double scale;

    public static void onRender(MatrixStack matrices, Matrix4f projection) {
        model = matrices.peek().getModel().copy();
        NametagUtils.projection = projection;

        camera = mc.gameRenderer.getCamera().getPos();
        cameraNegated = camera.negate();

        windowScale = mc.getWindow().calculateScaleFactor(1, mc.forcesUnicodeFont());
    }

    public static boolean to2D(Vec3d pos, double scale) {
        NametagUtils.scale = getScale(pos) * scale;

        quat.set((float) (cameraNegated.x + pos.getX()), (float) (cameraNegated.y + pos.getY()), (float) (cameraNegated.z + pos.getZ()), 1);

        IMatrix4f mm = ((IMatrix4f) (Object) model);
        IMatrix4f pm = ((IMatrix4f) (Object) projection);

        Quaternion out = pm.multiplyMatrix(mm.multiplyMatrix(quat));

        if (out.getW() <= 0.0f) return false;

        ((IQuaternion) (Object) out).toScreen();
        float x = out.getX() * mc.getWindow().getWidth();
        float y = out.getY() * mc.getWindow().getHeight();

        if (Float.isInfinite(x) || Float.isInfinite(y)) return false;

        ((IVec3d) pos).set(x / windowScale, mc.getWindow().getHeight() - y / windowScale, out.getZ());
        return true;
    }

    public static void begin(Vec3d pos) {
        glPushMatrix();
        glTranslated(pos.x, pos.y, 0);
        glScaled(scale, scale, 1);
    }

    public static void end() {
        glPopMatrix();
    }

    private static double getScale(Vec3d pos) {
        double dist = camera.distanceTo(pos);
        return Utils.clamp(1 - dist * 0.01, 0.5, Integer.MAX_VALUE);
    }

    // Remove below

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

    public static void endOld() {
        Matrices.pop();
    }
}
