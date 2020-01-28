package minegame159.meteorclient.modules.player;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;

public class FastUse extends Module {
    private Field itemUseCooldown;

    public FastUse() {
        super(Category.Player, "fast-use", "Fast item use.");

        try {
            itemUseCooldown = MinecraftClient.class.getDeclaredField("itemUseCooldown");
            itemUseCooldown.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        try {
            itemUseCooldown.set(mc, 0);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }
}
