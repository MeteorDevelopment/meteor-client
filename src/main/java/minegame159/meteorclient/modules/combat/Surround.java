package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class Surround extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Works only when you standing on ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyWhenSneaking = sgGeneral.add(new BoolSetting.Builder()
            .name("only-when-sneaking")
            .description("Places blocks only when sneaking.")
            .defaultValue(false)
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

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
            .name("instant")
            .description("Places all blocks in one tick.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnJump = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-jump")
            .description("Automatically disables when you jump.")
            .defaultValue(true)
            .build()
    );

    private int prevSlot;
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private boolean return_;

    public Surround() {
        super(Category.Combat, "surround", "Surrounds you with obsidian (or other blocks) to take less damage.");
    }

    @Override
    public void onActivate() {
        if (center.get()) {
            double x = MathHelper.floor(mc.player.getX()) + 0.5;
            double z = MathHelper.floor(mc.player.getZ()) + 0.5;
            mc.player.updatePosition(x, mc.player.getY(), z);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
        }
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (disableOnJump.get() && mc.options.keyJump.isPressed()) {
            toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (onlyWhenSneaking.get() && !mc.options.keySneak.isPressed()) return;

        // Place
        return_ = false;

        boolean p1 = tryPlace(0, -1, 0);
        if (return_) return;
        boolean p2 = tryPlace(1, 0, 0);
        if (return_) return;
        boolean p3 = tryPlace(-1, 0, 0);
        if (return_) return;
        boolean p4 = tryPlace(0, 0, 1);
        if (return_) return;
        boolean p5 = tryPlace(0, 0, -1);
        if (return_) return;

        // Auto turn off
        if (turnOff.get() && p1 && p2 && p3 && p4 && p5) toggle();
    });

    private boolean tryPlace(int x, int y, int z) {
        boolean p = place(x, 0, z);
        if (return_) return p;
        if (p) return true;

        for (int i = 0; i < 5; i++) {
            int x2 = x;
            int y2 = y;
            int z2 = z;

            switch (i) {
                case 0: y2--; break;
                case 1: x2++; break;
                case 2: x2--; break;
                case 3: z2++; break;
                case 4: z2--; break;
            }

            p = place(x2, y2, z2);
            if (return_) return p;
            if (p) {
                place(x, y, z);
                return true;
            }
        }

        return false;
    }

    private boolean place(int x, int y, int z) {
        setBlockPos(x, y, z);

        boolean wasObby = !instant.get() && mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN;

        if (findSlot()) {
            Utils.place(Blocks.OBSIDIAN.getDefaultState(), blockPos, true, false, true);
            resetSlot();

            if (!instant.get()) {
                boolean isObby = mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN;
                if (!wasObby && isObby) return_ = true;
            }
        }

        return !mc.world.getBlockState(blockPos).getMaterial().isReplaceable();
    }

    private void setBlockPos(int x, int y, int z) {
        blockPos.set(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
    }

    private boolean findSlot() {
        prevSlot = mc.player.inventory.selectedSlot;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (!(item instanceof BlockItem)) continue;

            if (item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN) {
                mc.player.inventory.selectedSlot = i;
                return true;
            }
        }

        return false;
    }

    private void resetSlot() {
        mc.player.inventory.selectedSlot = prevSlot;
    }
}
