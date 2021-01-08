package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.client.options.Perspective;

public class FreeRotate extends Module {

    public FreeRotate() {
        super(Category.Render, "free-rotate", "Allows you to freely rotate your camera in third person.");
    }

    private float cameraYaw;
    private float cameraPitch;

    @Override
    public void onDeactivate() {
        cameraPitch = mc.player.pitch;
        cameraYaw = mc.player.yaw;
    }

    @Override
    public void onActivate() {
        cameraPitch = mc.player.pitch;
        cameraYaw = mc.player.yaw;
    }

    public boolean shouldRotate() {
        return isActive() && mc.options.getPerspective() != Perspective.FIRST_PERSON;
    }

    public void setRotation(float yaw, float pitch) {
        cameraYaw = yaw;
        cameraPitch = pitch;
    }

    public float getYaw() {
        return cameraYaw;
    }

    public float getPitch() {
        return cameraPitch;
    }
}