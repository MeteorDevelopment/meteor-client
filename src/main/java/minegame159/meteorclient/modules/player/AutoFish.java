package minegame159.meteorclient.modules.player;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.item.FishingRodItem;

public class AutoFish extends Module {
    private static BoolSetting autoCast = new BoolSetting("auto-cast", "Automatically casts when activated.", true);
    private static IntSetting ticksCatch = new IntSetting("ticks-catch", "Ticks to wait before catching the fish.", 6, 0, null);
    private static IntSetting ticksThrow = new IntSetting("ticks-throw", "Ticks to wait before throwing the bobber.", 14, 0, null);

    private boolean ticksEnabled;
    private int ticksToRightClick;
    private int ticksData;

    public AutoFish() {
        super(Category.Player, "auto-fish", "Automatically fishes.", autoCast, ticksCatch, ticksThrow);
    }

    @Override
    public void onActivate() {
        ticksEnabled = false;
        if (autoCast.value && mc.player.getMainHandStack().getItem() instanceof FishingRodItem) Utils.rightClick();
    }

    @SubscribeEvent
    private void onPlaySoundPacket(PlaySoundPacketEvent e) {
        if (e.packet.getSound().getId().getPath().equals("entity.fishing_bobber.splash")) {
            ticksEnabled = true;
            ticksToRightClick = ticksCatch.value;
            ticksData = 0;
        }
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (ticksEnabled && ticksToRightClick <= 0) {
            if (ticksData == 0) {
                Utils.rightClick();
                ticksToRightClick = ticksThrow.value;
                ticksData = 1;
            }
            else if (ticksData == 1) {
                Utils.rightClick();
                ticksEnabled = false;
            }
        }

        ticksToRightClick--;
    }

    @SubscribeEvent
    public void onInput(KeyEvent event) {
        if (mc.options.keyUse.isPressed()) ticksEnabled = false;
    }
}
