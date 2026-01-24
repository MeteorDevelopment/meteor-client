package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class FastPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("放置冷却刻 (Ticks)。设置为 4 时将完全使用原版逻辑。")
        .defaultValue(2)
        .min(0)
        .max(4)
        .sliderRange(0, 4)
        .build()
    );

    public FastPlace() {
        super(Categories.Combat, "fast-place", "更智能的快速放置，支持副手。");
    }

    // 改为 Pre 事件，在客户端处理输入前运行，效果更稳定
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 如果设置为 4 (原版)，直接 return，保证完全的原版行为，解决“设置为4也由于代码干扰导致异常”的问题
        if (delay.get() == 4) return;

        // 只有按下按键时才触发
        if (!mc.options.useKey.isPressed()) return;

        // 检查主手或副手是否满足快速放置条件
        boolean mainHandBlock = shouldFastPlace(mc.player.getMainHandStack());
        boolean offHandBlock = shouldFastPlace(mc.player.getOffHandStack());

        if (!mainHandBlock && !offHandBlock) return;

        // 获取当前冷却
        int currentCooldown = ((MinecraftClientAccessor) mc).meteor$getItemUseCooldown();

        // 只有当冷却确实大于设定值时才修改
        // 使用 Pre 事件修改后，当刻的输入处理就会读取到新的冷却值
        if (currentCooldown > delay.get()) {
            ((MinecraftClientAccessor) mc).meteor$setItemUseCooldown(delay.get());
        }
    }

    private boolean shouldFastPlace(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();

        // 仅对方块生效，防止误触其他物品（如药水、食物等）
        if (!(item instanceof BlockItem)) return false;
        
        // 排除蜘蛛网等特殊物品
        if (item == Items.COBWEB) return false;

        return true;
    }
}