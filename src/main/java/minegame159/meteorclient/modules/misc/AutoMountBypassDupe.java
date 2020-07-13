package minegame159.meteorclient.modules.misc;

//Created by squidoodly 12/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class AutoMountBypassDupe extends ToggleModule {
    public AutoMountBypassDupe(){super(Category.Misc, "auto-mount-bypass-dupe", "Does the mount bypass dupe for you");}

    private boolean noCancel = false;
    private AbstractDonkeyEntity entity;
    private boolean sneak = false;

    private final List<Integer> slotsToMove = new ArrayList<>();
    private final List<Integer> slotsToThrow = new ArrayList<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> shulkersOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("shulker-only")
            .description("Only moves shulker boxes into the inventory")
            .defaultValue(false)
            .build());

    @EventHandler
    private Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if(noCancel)return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket
                && ((PlayerInteractEntityC2SPacket) event.packet).getType() == PlayerInteractEntityC2SPacket.InteractionType.INTERACT_AT
                && (((PlayerInteractEntityC2SPacket) event.packet).getEntity(mc.world)) instanceof AbstractDonkeyEntity) {
            event.cancel();
        }
    });

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        int slots = getInvSize(mc.player.getVehicle());

        for (Entity e: mc.world.getEntities()) {
            if (e.getPos().distanceTo(mc.player.getPos()) < 5
                    && e instanceof AbstractDonkeyEntity && ((AbstractDonkeyEntity) e).isTame()) {
                entity = (AbstractDonkeyEntity) e;
            }
        }
        if(entity == null) return;

        if(sneak) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SNEAKING));
            sneak = false;
            return;
        }

        if(slots == -1){
            if(entity.hasChest() || mc.player.getMainHandStack().getItem() == Items.CHEST){
                mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND));
            }else{
                int slot = InvUtils.findItemWithCount(Items.CHEST).slot;
                if(slot != -1 && slot < 9){
                    mc.player.inventory.selectedSlot  = slot;
                }else{
                    Utils.sendMessage("#redCannot find chest in hotbar. Disabling!");
                    this.toggle();
                }
            }
        }else if(slots == 0){
            if(isDupeTime()){
                if(!slotsToThrow.isEmpty()){
                    for (int i: slotsToThrow) {
                        InvUtils.clickSlot(i, 1, SlotActionType.THROW);
                    }
                    slotsToThrow.clear();
                }else{
                    for (int i = 2; i < getDupeSize(); i++) {
                        slotsToThrow.add(i);
                    }
                }
            }else{
                mc.player.closeContainer();
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SNEAKING));
                sneak = true;
            }
        }else if(!(mc.currentScreen instanceof HorseScreen)){
            mc.player.openRidingInventory();
        }else if(slots > 0 ){
            if (slotsToMove.isEmpty()) {
                boolean empty = true;
                for (int i = 2; i <= slots; i++) {
                    if (!(mc.player.container.getStacks().get(i).isEmpty())) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    for (int i = slots + 2; i < mc.player.container.getStacks().size(); i++) {
                        if (!(mc.player.container.getStacks().get(i).isEmpty())) {
                            if (mc.player.container.getSlot(i).getStack().getItem() == Items.CHEST) continue;
                            if (!(mc.player.container.getSlot(i).getStack().getItem() instanceof BlockItem
                                    && ((BlockItem) mc.player.container.getSlot(i).getStack().getItem()).getBlock() instanceof ShulkerBoxBlock)
                                    && shulkersOnly.get()) continue;
                            slotsToMove.add(i);

                            if (slotsToMove.size() >= slots) break;
                        }
                    }
                }else{
                    noCancel = true;
                    mc.player.networkHandler.sendPacket(new PlayerInteractEntityC2SPacket(entity, Hand.MAIN_HAND, entity.getPos().add(entity.getWidth() / 2, entity.getHeight() / 2, entity.getWidth() / 2)));
                    noCancel = false;
                    return;
                }
            }

            if (!slotsToMove.isEmpty()) {
                for (int i: slotsToMove) mc.interactionManager.method_2906(mc.player.container.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                slotsToMove.clear();

            }
        }
    });

    private int getInvSize(Entity e){
        if (!(e instanceof AbstractDonkeyEntity)) return -1;

        if (!((AbstractDonkeyEntity)e).hasChest()) return 0;

        if (e instanceof LlamaEntity) {
            return 3 * ((LlamaEntity) e).method_6702();
        }

        return 15;
    }

    private boolean isDupeTime() {
        if (mc.player.getVehicle() != entity || entity.hasChest() || mc.player.container.getStacks().size() == 46) {
            return false;
        }

        if (mc.player.container.getStacks().size() > 38) {
            for (int i = 2; i < getDupeSize() + 1; i++) {
                if (mc.player.container.getSlot(i).hasStack()) {
                    return true;
                }
            }
        }

        return false;
    }

    private int getDupeSize() {
        if (mc.player.getVehicle() != entity || entity.hasChest() || mc.player.container.getStacks().size() == 46) {
            return 0;
        }

        return mc.player.container.getStacks().size() - 38;
    }
}
