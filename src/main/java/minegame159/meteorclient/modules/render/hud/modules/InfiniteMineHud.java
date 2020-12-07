package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.InfinityMiner;
import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.block.Block;

public class InfiniteMineHud extends DoubleTextHudModule {
    public InfiniteMineHud(HUD hud) {
        super(hud, "infmine", "Details Regarding Infinity Mine", "Infinity Mine: ");
    }

    @Override
    protected String getRight() {
        InfinityMiner infinityMiner = ModuleManager.INSTANCE.get(InfinityMiner.class);
        if (!infinityMiner.isActive()) return "Disabled";

        switch (infinityMiner.getMode()) {
            case HOME:
                int[] coords = infinityMiner.getHomeCoords();
                return "Heading Home: " + coords[0] + " " + coords[1] + " " + coords[2];
            case TARGET:
                return "Mining: " + infinityMiner.getCurrentTarget().getName().getString();
            case REPAIR:
                return "Repair-Mining: " + infinityMiner.getCurrentTarget().getName().getString();
            case STILL:
                return "Resting";
            default:
                return "";
        }
    }
}
