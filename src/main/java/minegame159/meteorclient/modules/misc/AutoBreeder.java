package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoBreeder extends Module {
    private Item[] sheepCowBreedingItems = { Items.WHEAT };
    private Item[] pigBreedingItems = { Items.CARROT, Items.POTATO, Items.BEETROOT  };
    private Item[] chickenBreedingItems = { Items.WHEAT_SEEDS, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS, Items.BEETROOT_SEEDS };

    public AutoBreeder() {
        super(Category.Misc, "auto-breeder", "Automatically breeds animals.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof AnimalEntity) || !((AnimalEntity) entity).canEat()) continue;

            Item[] breedingItems = null;
            if (entity instanceof SheepEntity || entity instanceof CowEntity) breedingItems = sheepCowBreedingItems;
            else if (entity instanceof PigEntity) breedingItems = pigBreedingItems;
            else if (entity instanceof ChickenEntity) breedingItems = chickenBreedingItems;

            if (breedingItems == null) continue;

            boolean findBreedingItem = true;
            boolean offHand = false;
            if (checkHand(Hand.MAIN_HAND, breedingItems)) findBreedingItem = false;
            else if (checkHand(Hand.OFF_HAND, breedingItems)) {
                findBreedingItem = false;
                offHand = true;
            }

            boolean foundBreedingItem = !findBreedingItem;
            if (findBreedingItem) {
                for (int i = 0; i < 9; i++) {
                    Item item = mc.player.inventory.getInvStack(i).getItem();
                    for (Item breedingItem : breedingItems) {
                        if (item == breedingItem) {
                            mc.player.inventory.selectedSlot = i;
                            foundBreedingItem = true;
                            break;
                        }
                    }
                    if (foundBreedingItem) break;
                }
            }

            if (foundBreedingItem) {
                mc.interactionManager.interactEntity(mc.player, entity, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
                return;
            }
        }
    });

    private boolean checkHand(Hand hand, Item[] items) {
        Item handItem = hand == Hand.MAIN_HAND ? mc.player.inventory.getMainHandStack().getItem() : mc.player.inventory.offHand.get(0).getItem();
        for (Item item : items) {
            if (handItem == item) return true;
        }
        return false;
    }
}
