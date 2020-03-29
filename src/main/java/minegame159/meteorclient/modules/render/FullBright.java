package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.Option;

public class FullBright extends ToggleModule {
    public FullBright() {
        super(Category.Render, "full-bright", "No more darkness.");
    }

    @Override
    public void onActivate() {
        MinecraftClient.getInstance().options.gamma = 16;
    }

    @Override
    public void onDeactivate() {
        mc.options.gamma = Option.GAMMA.get(mc.options);
    }
}
