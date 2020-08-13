package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

public class Surround extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Works only when you standing on ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyObsidian = sgGeneral.add(new BoolSetting.Builder()
            .name("only-obsidian")
            .description("Only uses obsidian.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off")
            .description("Turns off when placed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
            .name("center")
            .description("Moves you to the center of the block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnJump = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-jump")
            .description("Automatically disables when you jump.")
            .defaultValue(true)
            .build()
    );

    public Surround() {
        super(Category.Combat, "surround", "Surrounds you with obsidian (or other blocks) to take less damage.");
    }

    @Override
    public void onActivate() {
        if (center.get()) {
            double x = MathHelper.floor(mc.player.x) + 0.5;
            double z = MathHelper.floor(mc.player.z) + 0.5;
            mc.player.updatePosition(x, mc.player.y, z);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(mc.player.x, mc.player.y, mc.player.z, mc.player.onGround));
        }
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (onlyOnGround.get() && !mc.player.onGround) return;

        if (disableOnJump.get() && mc.options.keyJump.isPressed()) {
            toggle();
            return;
        }

        int slot;
        if (mc.player.getMainHandStack().getItem() == Items.OBSIDIAN) slot = mc.player.inventory.selectedSlot;
        else slot = findSlot();

        boolean allPlaced = true;

        if (slot != -1) {
            int preSelectedSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = slot;
            BlockState blockState = ((BlockItem) mc.player.inventory.getMainHandStack().getItem()).getBlock().getDefaultState();

            Utils.place(blockState, mc.player.getBlockPos().add(1, 0, 0));
            if (mc.world.getBlockState(mc.player.getBlockPos().add(1, 0, 0)).getMaterial().isReplaceable()) allPlaced = false;
            if (mc.player.getMainHandStack().getItem() != Items.OBSIDIAN) slot = findSlot();
            if (slot == -1) {
                mc.player.inventory.selectedSlot = preSelectedSlot;
                if (turnOff.get() && allPlaced) toggle();
                return;
            }

            Utils.place(blockState, mc.player.getBlockPos().add(-1, 0, 0));
            if (mc.world.getBlockState(mc.player.getBlockPos().add(-1, 0, 0)).getMaterial().isReplaceable()) allPlaced = false;
            if (mc.player.getMainHandStack().getItem() != Items.OBSIDIAN) slot = findSlot();
            if (slot == -1) {
                mc.player.inventory.selectedSlot = preSelectedSlot;
                if (turnOff.get() && allPlaced) toggle();
                return;
            }

            Utils.place(blockState, mc.player.getBlockPos().add(0, 0, 1));
            if (mc.world.getBlockState(mc.player.getBlockPos().add(0, 0, 1)).getMaterial().isReplaceable()) allPlaced = false;
            if (mc.player.getMainHandStack().getItem() != Items.OBSIDIAN) slot = findSlot();
            if (slot == -1) {
                mc.player.inventory.selectedSlot = preSelectedSlot;
                if (turnOff.get() && allPlaced) toggle();
                return;
            }

            Utils.place(blockState, mc.player.getBlockPos().add(0, 0, -1));
            if (mc.world.getBlockState(mc.player.getBlockPos().add(0, 0, -1)).getMaterial().isReplaceable()) allPlaced = false;

            mc.player.inventory.selectedSlot = preSelectedSlot;

            if (turnOff.get() && allPlaced) toggle();
        }
    });

    private int findSlot() {
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getInvStack(i).getItem();

            if (!(item instanceof BlockItem)) continue;

            if (item == Items.OBSIDIAN) {
                return i;
            }

            if (!onlyObsidian.get()) {
                if (((BlockItem) item).getBlock().getDefaultState().isSimpleFullBlock(mc.world, null)) slot = i;
            }
        }

        return slot;
    }
}
