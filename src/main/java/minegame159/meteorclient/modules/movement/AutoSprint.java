package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class AutoSprint extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> permanent = sgGeneral.add(new BoolSetting.Builder()
            .name("permanent"
            ).description("Keeps you sprinting even when you aren't moving.")
            .defaultValue(true)
            .build()
    );
	
    public AutoSprint() {
        super(Category.Movement, "auto-sprint", "Automatically sprints.");
    }
    
    @Override
    public void onDeactivate() {
        mc.player.setSprinting(false);
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
    	if(mc.player.forwardSpeed > 0 && !permanent.get()) {
            mc.player.setSprinting(true);
    	} else if (permanent.get()) {
    		mc.player.setSprinting(true);
    	}
    });
}
