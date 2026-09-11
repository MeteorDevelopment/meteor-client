package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class RTPer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between RTP commands in seconds.")
        .defaultValue(30)
        .min(1)
        .build()
    );

    private int tickCounter = 0;

    public RTPer() {
        super(Categories.Donut, "rtper", "Automatically uses /rtp command.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        tickCounter++;
        if (tickCounter >= delay.get() * 20) {
            tickCounter = 0;
            mc.player.connection.sendCommand("rtp");
        }
    }
}
