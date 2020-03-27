package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.BlockListSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.List;

public class XRay extends Module {
    public static XRay INSTANCE;

    private Setting<List<Block>> blocks = addSetting(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks.")
            .defaultValue(new ArrayList<>(0))
            .onChanged(blocks1 -> {
                if (isActive()) mc.worldRenderer.reload();
            })
            .build()
    );

    private boolean fullBrightWasActive = false;

    public XRay() {
        super(Category.Render, "xray", "See only specified blocks.");
    }

    @Override
    public void onActivate() {
        FullBright fullBright = ModuleManager.INSTANCE.get(FullBright.class);
        fullBrightWasActive = fullBright.isActive();
        if (!fullBright.isActive()) fullBright.toggle();

        mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        FullBright fullBright = ModuleManager.INSTANCE.get(FullBright.class);
        if (!fullBrightWasActive && fullBright.isActive()) fullBright.toggle();

        if (!MeteorClient.isDisconnecting) mc.worldRenderer.reload();
    }

    public boolean isVisible(Block block) {
        return blocks.get().contains(block);
    }
}
