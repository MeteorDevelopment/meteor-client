package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.StringUtils;

public class RotationHud extends DoubleTextHudModule {
    public RotationHud(HUD hud) {
        super(hud, "rotation", "Displays your rotation", "invalid ");
    }

    @Override
    protected String getRight() {
        MinecraftClient mc = MinecraftClient.getInstance();

        Direction direction = Direction.fromRotation(mc.gameRenderer.getCamera().getYaw());
        String axis = "invalid";
        switch (direction) {
            case NORTH: axis = "-Z"; break;
            case SOUTH: axis = "+Z"; break;
            case WEST:  axis = "-X"; break;
            case EAST:  axis = "+X"; break;
        }

        float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
        if (yaw < 0) yaw += 360;
        if (yaw > 180) yaw -= 360;

        float pitch = mc.gameRenderer.getCamera().getPitch() % 360;
        if (pitch < 0) pitch += 360;
        if (pitch > 180) pitch -= 360;

        setLeft(String.format("%s %s ", StringUtils.capitalize(direction.getName()), axis));
        return String.format("(%.1f, %.1f)", yaw, pitch);
    }
}
