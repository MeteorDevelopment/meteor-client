package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.NameTagItem;
import net.minecraft.util.Hand;

public class AutoNametag extends Module {
    private Setting<Double> distance = addSetting(new DoubleSetting.Builder()
            .name("distance")
            .description("Maximum distance.")
            .min(0.0)
            .defaultValue(5.0)
            .build()
    );

    public AutoNametag() {
        super(Category.Misc, "auto-nametag", "Automatically uses nametags in hother on unnamed entites. WARNING: will name all entities in specified distance.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity || entity.hasCustomName() || mc.player.distanceTo(entity) > distance.get()) continue;

            boolean findNametag = true;
            boolean offHand = false;
            if (mc.player.inventory.getMainHandStack().getItem() instanceof NameTagItem) {
                findNametag = false;
            }
            else if (mc.player.inventory.offHand.get(0).getItem() instanceof NameTagItem) {
                findNametag = false;
                offHand = true;
            }

            boolean foundNametag = !findNametag;
            if (findNametag) {
                for (int i = 0; i < 9; i++) {
                    ItemStack itemStack = mc.player.inventory.getInvStack(i);
                    if (itemStack.getItem() instanceof NameTagItem) {
                        mc.player.inventory.selectedSlot = i;
                        foundNametag = true;
                        break;
                    }
                }
            }

            if (foundNametag) {
                mc.interactionManager.interactEntity(mc.player, entity, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
                return;
            }
        }
    });
}
