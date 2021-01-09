package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.client.options.Perspective;

public class FreeRotate extends Module {

    public FreeRotate() {
        super(Category.Render, "free-rotate", "Allows you to freely rotate your camera in third person.");
    }

    public float cameraYaw;
    public float cameraPitch;

    public boolean shouldRotate() {
        return isActive() && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK;
    }
}