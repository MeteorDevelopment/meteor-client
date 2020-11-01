package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.NameTagItem;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class AutoNametag extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Which entities to nametag.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );
    
    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("distance")
            .description("Maximum distance.")
            .min(0.0)
            .defaultValue(5.0)
            .build()
    );

    public AutoNametag() {
        super(Category.Misc, "auto-nametag", "Automatically uses nametags in hotbar on unnamed entites. WARNING: will name all entities in specified distance.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().contains(entity.getType()) || entity.hasCustomName() || mc.player.distanceTo(entity) > distance.get()) continue;

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
                    ItemStack itemStack = mc.player.inventory.getStack(i);
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
