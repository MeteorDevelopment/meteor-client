package minegame159.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

import java.util.Random;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmScatter extends Command {

    public SwarmScatter(){
        super("s","(highlight)scatter - Send them running.");
    }


    public void scatter() {
        scatter(100);
    }

    public void scatter(int radius){
        MinecraftClient mc = MinecraftClient.getInstance();
        if(mc.player != null) {
            Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
            Random random = new Random();
            double a = random.nextDouble() * 2 * Math.PI;
            double r = radius * Math.sqrt(random.nextDouble());
            double x = mc.player.getX() + r * Math.cos(a);
            double z = mc.player.getZ() + r * Math.sin(a);
            swarm.currentTaskSetting.set(Swarm.CurrentTask.BARITONE);
            swarm.resetTarget();
            if(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int)x,(int)z));
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("scatter").executes(context -> {
            Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
            if(swarm.isActive()){
                if(swarm.currentMode.get() == Swarm.Mode.QUEEN && swarm.server != null){
                    swarm.server.sendMessage(context.getInput());
                }
                else{
                    scatter();
                }
            }
            return SINGLE_SUCCESS;
        }).then(argument("radius", IntegerArgumentType.integer()).executes(context -> {
            Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
            if(swarm.isActive()){
                if(swarm.currentMode.get() == Swarm.Mode.QUEEN && swarm.server != null){
                    swarm.server.sendMessage(context.getInput());
                }
                else{
                    scatter(context.getArgument("radius",Integer.class));
                }
            }
            return SINGLE_SUCCESS;
        })));
    }
}
