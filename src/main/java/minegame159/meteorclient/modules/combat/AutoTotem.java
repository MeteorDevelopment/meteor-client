package minegame159.meteorclient.modules.combat;

//Updated by squidoodly 24/04/2020
//Updated by squidoodly 19/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.movement.NoFall;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.DamageCalcUtils;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EnderCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

public class AutoTotem extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
            .name("smart")
            .description("Only switches to totem when in danger of dying")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> inventorySwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("inventory")
            .description("Switches totems while you are in your inventory")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("The health smart totem activates")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .build()
    );
    
    private int totemCount;
    private String totemCountString = "0";

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean locked = false;

    public AutoTotem() {
        super(Category.Combat, "auto-totem", "Automatically equips totems.");
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen instanceof ContainerScreen<?>  && (!(mc.currentScreen instanceof InventoryScreen) || !inventorySwitch.get())) return;

        int preTotemCount = totemCount;
        InvUtils.FindItemResult result = InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING);

        if (result.found() && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && !smart.get()) {
            locked = true;
            if(mc.player.inventory.getCursorStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
            }
            InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
        }else if(result.found() && !(mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) && smart.get() &&
                ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) < health.get() || ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) - getHealthReduction()) < health.get())){
            locked = true;
            if(mc.player.inventory.getCursorStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
            }
            InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
        }
        if(smart.get() && ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) > health.get()
                && (((mc.player.getHealth() + mc.player.getAbsorptionAmount()) - getHealthReduction()) > health.get()))){
            locked = false;
        }

        if (result.count != preTotemCount) totemCountString = Integer.toString(result.count);
    });

    @Override
    public String getInfoString() {
        return totemCountString;
    }

    private double getHealthReduction(){
        double damageTaken = 0;
        for(Entity entity : mc.world.getEntities()){
            if(entity instanceof EnderCrystalEntity && damageTaken < DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.blastProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.getDamageMultiplied(DamageCalcUtils.crystalDamage(mc.player, entity.getPos())))))){
                damageTaken = DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.blastProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.getDamageMultiplied(DamageCalcUtils.crystalDamage(mc.player, entity.getPos())))));
            }else if(entity instanceof PlayerEntity && damageTaken < DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.normalProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.getSwordDamage((PlayerEntity) entity))))){
                if(!FriendManager.INSTANCE.isTrusted((PlayerEntity) entity) && mc.player.getPos().distanceTo(entity.getPos()) < 5){
                    if(((PlayerEntity) entity).getActiveItem().getItem() instanceof SwordItem){
                        damageTaken = DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.normalProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.getSwordDamage((PlayerEntity) entity))));
                    }
                }
            }
        }
        if(!ModuleManager.INSTANCE.get(NoFall.class).isActive() && mc.player.fallDistance > 3){
            double damage =mc.player.fallDistance * 0.5;
            if(damage > damageTaken){
                damageTaken = damage;
            }
        }
        if (mc.world.dimension.getType() != DimensionType.OVERWORLD) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                if (blockEntity instanceof BedBlockEntity && damageTaken < DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.blastProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.getDamageMultiplied(DamageCalcUtils.bedDamage(mc.player, new Vec3d(blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ()))))))) {
                    damageTaken = DamageCalcUtils.resistanceReduction(mc.player, DamageCalcUtils.blastProtReduction(mc.player, DamageCalcUtils.armourCalc(mc.player, DamageCalcUtils.getDamageMultiplied(DamageCalcUtils.bedDamage(mc.player, new Vec3d(blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ()))))));
                }
            }
        }
        return damageTaken;
    }

    public boolean getLocked(){
        return locked;
    }

}
