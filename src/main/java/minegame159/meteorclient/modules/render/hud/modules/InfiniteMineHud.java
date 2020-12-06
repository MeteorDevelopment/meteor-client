package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.InfinityMiner;
import minegame159.meteorclient.modules.render.hud.HUD;

import java.util.Arrays;

public class InfiniteMineHud extends DoubleTextHudModule {
    public InfiniteMineHud(HUD hud) {
        super(hud, "infmine", "Details Regarding Infinity Mine", "Infinity Mine: ");
    }

    @Override
    protected String getRight() {
        InfinityMiner infinityMiner = ModuleManager.INSTANCE.get(InfinityMiner.class);
        if (!infinityMiner.isActive()) return "Disabled";
        else if (InfinityMiner.Mode.HOME == infinityMiner.getMode()) {
            int[] coords = infinityMiner.getHomeCoords();
            return "Heading Home: " + coords[0] + " " + coords[1] + " " + coords[2];
        } else if (InfinityMiner.Mode.TARGET == infinityMiner.getMode()) {
            return "Mining: " + formatTargetBlock(infinityMiner.getCurrentTarget());
        } else if (InfinityMiner.Mode.REPAIR == infinityMiner.getMode()) {
            return "Repair-Mining: " + formatTargetBlock(infinityMiner.getCurrentTarget());
        } else {
            return "Resting";
        }
    }

    private String formatTargetBlock(String target) {
        StringBuilder formattedTarget = new StringBuilder();
        for (String token : target.split("_"))
            formattedTarget.append(token.substring(0, 1).toUpperCase()).append(token.substring(1)).append(" ");
        return formattedTarget.toString();
    }
}
