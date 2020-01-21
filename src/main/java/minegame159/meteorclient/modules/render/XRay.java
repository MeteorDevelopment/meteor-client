package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.BlockShouldDrawSideEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.util.registry.Registry;

public class XRay extends Module {
    public XRay() {
        super(Category.Render, "xray", "See only specified blocks.");
    }

    @Override
    public void onActivate() {
        mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        mc.worldRenderer.reload();
    }

    @SubscribeEvent
    private void onBlockShouldRenderSide(BlockShouldDrawSideEvent e) {
        e.shouldRenderSide = isVisible(e.state.getBlock());
        e.setCancelled(true);
    }

    private boolean isVisible(Block block) {
        String id = Registry.BLOCK.getId(block).toString();
        return id.endsWith("_ore");
    }
}
