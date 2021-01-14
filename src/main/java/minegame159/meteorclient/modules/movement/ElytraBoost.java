package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.player.InteractItemEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.FireworkItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.List;

public class ElytraBoost extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> dontConsumeFirework = sgGeneral.add(new BoolSetting.Builder()
            .name("dont-consume-firework")
            .description("Doesn't consume the firework when using it.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> fireworkLevel = sgGeneral.add(new IntSetting.Builder()
            .name("firework-level")
            .description("The duration.")
            .defaultValue(0)
            .min(0)
            .max(255)
            .build()
    );

    private final Setting<Integer> keybind = sgGeneral.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("Keybind to boost.")
            .action(this::boost)
            .build()
    );

    private final List<FireworkRocketEntity> fireworks = new ArrayList<>();

    public ElytraBoost() {
        super(Category.Movement, "elytra-boost", "Boosts you as if you used a firework.");
    }

    @Override
    public void onDeactivate() {
        fireworks.clear();
    }

    @EventHandler
    private final Listener<InteractItemEvent> onInteractItem = new Listener<>(event -> {
        ItemStack itemStack = mc.player.getStackInHand(event.hand);

        if (itemStack.getItem() instanceof FireworkItem && dontConsumeFirework.get()) {
            event.toReturn = ActionResult.PASS;

            boost();
        }
    });

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        fireworks.removeIf(fireworkRocketEntity -> fireworkRocketEntity.removed);
    });

    private void boost() {
        if (mc.player.isFallFlying() && mc.currentScreen == null) {
            ItemStack itemStack = Items.FIREWORK_ROCKET.getDefaultStack();
            itemStack.getOrCreateSubTag("Fireworks").putByte("Flight", fireworkLevel.get().byteValue());

            FireworkRocketEntity entity = new FireworkRocketEntity(mc.world, itemStack, mc.player);
            fireworks.add(entity);
            mc.world.addEntity(entity.getEntityId(), entity);
        }
    }

    public boolean isFirework(FireworkRocketEntity firework) {
        return isActive() && fireworks.contains(firework);
    }
}
