package minegame159.meteorclient.modules.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.entity.player.AttackEntityEvent;
import minegame159.meteorclient.events.entity.player.StartBreakingBlockEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;

public class AntiBreak extends Module{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> breakDurability = sgGeneral.add(new IntSetting.Builder()
        .name("anti-break-durability")
        .description("The durability to stop using a tool.")
        .defaultValue(10)
        .max(50)
        .min(2)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> toolSave = sgGeneral.add(new BoolSetting.Builder()
        .name("tool-save")
        .description("Whether or not to save a tool from breaking.")
        .defaultValue(true)
        .build()
    );

    public AntiBreak() {
        super(Categories.Player, "anti-break", "Prevents tools and items from breaking.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (toolSave.get()) {
            BlockState blockState = mc.world.getBlockState(event.blockPos);
            
            if (blockState.getHardness(mc.world, event.blockPos) < 0 || blockState.isAir()) return;
            
            ItemStack currentStack = mc.player.inventory.getStack(mc.player.inventory.selectedSlot);
    
            if (currentStack.getItem() instanceof ToolItem && shouldStopUsing(currentStack)) {
                mc.options.keyAttack.setPressed(false);
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onAttack(AttackEntityEvent event) {
        if (toolSave.get()) {
            ItemStack currentStack = mc.player.inventory.getStack(mc.player.inventory.selectedSlot);
        
            if (currentStack.getItem() instanceof ToolItem && shouldStopUsing(currentStack)) {
                mc.options.keyAttack.setPressed(false);
                event.setCancelled(true);
            }
        }
    }

    private boolean shouldStopUsing(ItemStack itemStack) {
        return itemStack.getMaxDamage() - itemStack.getDamage() < breakDurability.get();
    }
}
