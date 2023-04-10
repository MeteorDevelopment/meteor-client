package meteordevelopment.meteorclient.systems.modules.ttw;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.StringHelper;

import java.util.ArrayList;

public class PayAll extends Module {
    public PayAll() {
        super(Categories.TTW, "Pay-All", "Pays All Players In Server");
    }

    int currentDelay = 0;
    ArrayList<String> players = new ArrayList<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("Defines the command to use.")
        .defaultValue("/pay {player} {amount}")
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Defines the delay between sending commands (in Ticks!).")
        .defaultValue(20)
        .sliderRange(0, 200)
        .min(0)
        .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("Defines the amount to pay to every player.")
        .defaultValue(1000)
        .sliderRange(1, 200000)
        .min(1)
        .build()
    );
    private final Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Disables spam when you leave a server.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables spam when you are disconnected from a server.")
        .defaultValue(true)
        .build()
    );

    @Override
    public void onActivate() {
        fetchPlayers();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }
    @EventHandler
    public void onTick(TickEvent.Pre event) {

        if(players.isEmpty()) fetchPlayers();

        if(currentDelay < delay.get()) {
            currentDelay++;
        } else {
            currentDelay = 0;
            String cmd = command.get().startsWith("/") ? command.get().replaceFirst("/", "") : command.get();
            mc.getNetworkHandler().sendCommand(cmd.replace("{amount}", String.valueOf(amount.get())).replace("{player}", players.get(0)));
            players.remove(0);
        }
    }

    public void fetchPlayers() {
        players.clear();
        String playerName = mc.getSession().getProfile().getName();

        for(PlayerListEntry info : mc.player.networkHandler.getPlayerList()) {
            String name = info.getProfile().getName();
            name = StringHelper.stripTextFormat(name);

            if(name.equalsIgnoreCase(playerName))
                continue;

            players.add(name);
        }
    }

}
