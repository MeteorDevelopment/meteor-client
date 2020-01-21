package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.client.MinecraftClient;

public class FullBright extends Module {
    private double defaultGamma;

    public FullBright() {
        super(Category.Render, "full-bright", "No more darkness.");
    }

    @Override
    public void onActivate() {
        defaultGamma = mc.options.gamma;
        MinecraftClient.getInstance().options.gamma = 16;
    }

    @Override
    public void onDeactivate() {
        mc.options.gamma = defaultGamma;
    }
}
