package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.client.MinecraftClient;

public class FpsHud extends DoubleTextHudModule {
    public FpsHud(HUD hud) {
        super(hud, "fps", "Displays your fps.", "Fps: ");
    }

    @Override
    protected String getRight() {
        return Integer.toString(((IMinecraftClient) MinecraftClient.getInstance()).getFps());
    }
}
