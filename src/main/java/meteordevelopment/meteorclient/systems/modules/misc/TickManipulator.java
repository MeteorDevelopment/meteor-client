package meteordevelopment.meteorclient.systems.modules.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class TickManipulator extends Module
{
    private final SettingGroup sgClient = settings.createGroup("Client");
    private final SettingGroup sgServer = settings.createGroup("Server");

	public final Setting clientdelay = sgClient.add(new IntSetting.Builder().name("client-delay").description("update client timer.").defaultValue(0).build());
	public final Setting serverdelay = sgServer.add(new IntSetting.Builder().name("server-delay").description("update server timer.").defaultValue(0).build());

	public final Setting clientbool = sgClient.add(new BoolSetting.Builder().name("client-bool").description("update client boolean.").defaultValue(false).build());
	public final Setting serverbool = sgServer.add(new BoolSetting.Builder().name("server-bool").description("update server boolean.").defaultValue(false).build());


	public int clientTimer;
    public int serverTimer;

    public TickManipulator()
    {
        super(Categories.Misc, "tick-manipulator", "Manipulates world ticks");
    }

	public boolean clientTime()
    {
        // wait for timer
		if(clientTimer > 0 && Boolean.TRUE.equals(clientbool.get()))
		{
			clientTimer--;
			return true;
		}
		clientTimer = (Integer)clientdelay.get();
        return false;
    }

    public boolean serverTime()
    {
        // wait for timer
		if(serverTimer > 0 && Boolean.TRUE.equals(serverbool.get()))
		{
			serverTimer--;
			return true;
		}
		serverTimer = (Integer)serverdelay.get();
        return false;
    }
}
