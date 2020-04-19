package minegame159.meteorclient.modules.combat;

//Created by squidoodly 15/04/2020
//Added by squidoodly 18/04/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.DamageCalcUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RayTraceContext;

public class SmartSurround extends ToggleModule {

    private MinecraftClient mc = MinecraftClient.getInstance();

    private int oldSlot;

    private int slot = -1;

    private int rPosX;

    private int rPosZ;

    private Entity crystal;

    private Setting<Boolean> onlyObsidian = addSetting(new BoolSetting.Builder()
            .name("only-obsidian")
            .description("Only uses Obsidian")
            .defaultValue(false)
            .build());

    private Setting<Double> minDamage = addSetting(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage before this activates.")
            .defaultValue(5.5)
            .build());

    public SmartSurround(){
        super(Category.Combat, "smart-surround", "Tries to save you from crystals automatically.");
    }

    @EventHandler
    private Listener<EntityAddedEvent> onSpawn = new Listener<>(event -> {
        crystal = event.entity;
        if(event.entity.getType() == EntityType.END_CRYSTAL){
            if(DamageCalcUtils.crystalDamage(mc.player, event.entity) > minDamage.get()){
                slot = findObiInHotbar();
                if(slot == -1){
                    Utils.sendMessage("#redNo Obi in hotbar. Disabling!");
                    return;
                }
                mc.player.inventory.selectedSlot = slot;
                rPosX = mc.player.getBlockPos().getX() - event.entity.getBlockPos().getX();
                rPosZ = mc.player.getBlockPos().getZ() - event.entity.getBlockPos().getZ();
            }
        }
    });

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if(slot == -1){
            return;
        }else {
            if ((rPosX >= 2) && (rPosZ == 0)) {
                placeObi(rPosX - 1, 0, crystal);
            } else if ((rPosX > 1) && (rPosZ > 1)) {
                placeObi(rPosX, rPosZ - 1, crystal);
                placeObi(rPosX - 1, rPosZ, crystal);
            } else if ((rPosX == 0) && (rPosZ >= 2)) {
                placeObi(0, rPosZ - 1, crystal);
            } else if ((rPosX < -1) && (rPosZ < -1)) {
                placeObi(rPosX, rPosZ + 1, crystal);
                placeObi(rPosX + 1, rPosZ, crystal);
            } else if ((rPosX == 0) && (rPosZ <= -2)) {
                placeObi(0, rPosZ + 1, crystal);
            } else if ((rPosX > 1) && (rPosZ < -1)) {
                placeObi(rPosX, rPosZ + 1, crystal);
                placeObi(rPosX - 1, rPosZ, crystal);
            } else if ((rPosX <= -2) && (rPosZ == 0)) {
                placeObi(rPosX + 1, 0, crystal);
            } else if ((rPosX < -1) && (rPosZ > 1)) {
                placeObi(rPosX, rPosZ - 1, crystal);
                placeObi(rPosX + 1, rPosZ, crystal);
            }
            if (mc.world.rayTrace(
                    new RayTraceContext(mc.player.getPos(), crystal.getPos(),
                            RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, mc.player)).getType()
                    != HitResult.Type.MISS) {
                slot = -1;
                mc.player.inventory.selectedSlot = oldSlot;
            }
        }
    });

    private void placeObi(int x, int z, Entity crystal){
        //Place block packet
        PlayerInteractBlockC2SPacket placePacket = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, crystal.getBlockPos().add(x, -1, z), false));
        mc.player.networkHandler.sendPacket(placePacket);
        //Swinging hand to show that it is being placed
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int findObiInHotbar(){
        oldSlot = mc.player.inventory.selectedSlot;
        int newSlot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getInvStack(i).getItem();
            if (item == Items.OBSIDIAN) {
                newSlot = i;
                mc.player.inventory.selectedSlot = newSlot;
                break;
            }
        }
        return newSlot;
    }
}
