package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public class ShieldBreaker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyAxe = sgGeneral.add(new BoolSetting.Builder()
        .name("only-axe")
        .description("Only swing when an axe is in your hotbar, since only axes disable shields.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> crosshairOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("crosshair-only")
        .description("Only target the player you are looking at. Off targets the nearest blocking player and rotates to them.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Maximum distance to a blocking player.")
        .defaultValue(3.5)
        .min(1)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between shield-break attempts.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 40)
        .build()
    );

    private int timer;

    public ShieldBreaker() {
        super(Categories.Donut, "shield-breaker", "Swaps to an axe and hits players who are blocking.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (timer > 0) {
            timer--;
            return;
        }

        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        Player target = findTarget();
        if (target == null) return;

        FindItemResult axe = InvUtils.findInHotbar(stack -> stack.is(ItemTags.AXES));
        if (!axe.found() && onlyAxe.get()) return;

        if (crosshairOnly.get()) {
            hit(target, axe);
        } else {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> hit(target, axe));
        }

        timer = delay.get();
    }

    private Player findTarget() {
        double maxSq = range.get() * range.get();

        if (crosshairOnly.get()) {
            if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player player
                && player.isBlocking() && mc.player.distanceToSqr(player) <= maxSq) {
                return player;
            }
            return null;
        }

        Player nearest = null;
        double nearestSq = maxSq;
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isBlocking()) continue;

            double distSq = mc.player.distanceToSqr(player);
            if (distSq <= nearestSq) {
                nearest = player;
                nearestSq = distSq;
            }
        }
        return nearest;
    }

    private void hit(Player target, FindItemResult axe) {
        if (axe.found()) InvUtils.swap(axe.slot(), true);

        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);

        if (axe.found()) InvUtils.swapBack();
    }
}
