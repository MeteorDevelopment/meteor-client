package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class LookingAtHud extends DoubleTextHudModule {
    public LookingAtHud(HUD hud) {
        super(hud, "looking-at", "Displays what entity/block you are looking at.", "Looking At: ");
    }

    @Override
    protected String getRight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.crosshairTarget == null || mc.world == null) return "";

        if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) return mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos()).getBlock().getName().getString();
        else if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) return ((EntityHitResult) mc.crosshairTarget).getEntity().getDisplayName().getString();
        return "";
    }
}
