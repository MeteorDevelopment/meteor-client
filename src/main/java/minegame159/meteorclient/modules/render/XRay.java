package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.BlockShouldRenderSideEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import net.minecraft.block.Block;
import net.minecraft.util.registry.Registry;

public class XRay extends Module {
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

        mc.worldRenderer.reload();
    }

    @EventHandler
    private Listener<BlockShouldRenderSideEvent> onBlockShouldRenderSide = new Listener<>(event -> {
        event.shouldRenderSide = isVisible(event.state.getBlock());
        event.cancel();
    });

    private boolean isVisible(Block block) {
        String id = Registry.BLOCK.getId(block).toString();
        return id.endsWith("_ore");
    }
}
