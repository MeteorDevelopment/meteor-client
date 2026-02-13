package meteordevelopment.meteorclient.systems.modules.world;

import java.util.List;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.renderer.ShapeMode;

import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;

public class MinerPlacer extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSettings = settings.createGroup("Settings");
    private final SettingGroup sgScript = settings.createGroup("Script");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<BlockPos> zero = sgGeneral.add(new BlockPosSetting.Builder()
        .name("zero-pos")
        .description("Mining block position")
        .build()
    );

    private final Setting<Boolean> attackBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("attacking-block")
        .description("Attack blocks in pos.")
        .defaultValue(false)
        .build()
    );
  
    private final Setting<Boolean> miningBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("breaking-block")
        .description("Break blocks in pos.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> interactingBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("interacting-block")
        .description("Intreact blocks in pos.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Rotate> rotate = sgSettings.add(new EnumSetting.Builder<Rotate>()
        .name("rotate")
        .description("Switch rotates mode.")
        .defaultValue(Rotate.None)
        .build()
    );
    
    private final Setting<CardinalDirections> cardinaldirection = sgSettings.add(new EnumSetting.Builder<CardinalDirections>()
        .name("place-pirection")
        .description("Direction to use.")
        .defaultValue(CardinalDirections.Down)
        .build()
    );

    private final Setting<UseHand> breakHand = sgSettings.add(new EnumSetting.Builder<UseHand>()
        .name("break-hand")
        .description("Hand to break.")
        .defaultValue(UseHand.Main)
        .build()
    );
    
    private final Setting<UseHand> interactHand = sgSettings.add(new EnumSetting.Builder<UseHand>()
        .name("interact-hand")
        .description("Hand to interact.")
        .defaultValue(UseHand.Main)
        .build()
    );

    private final Setting<Boolean> insideBlock = sgSettings.add(new BoolSetting.Builder()
        .name("inside-block")
        .description("Inside block value.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Boolean> pre = sgSettings.add(new BoolSetting.Builder()
        .name("pre")
        .description("Load script before tick.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> post = sgSettings.add(new BoolSetting.Builder()
        .name("post")
        .description("Load script after tick.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> run = sgScript.add(new BoolSetting.Builder()
        .name("run")
        .description("Fire script execution.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<List<String>> script = sgScript.add(new StringListSetting.Builder()
        .name("script")
        .description("Minerplacer action commands.")
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the obsidian will be placed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> placingswing = sgRender.add(new BoolSetting.Builder()
        .name("placing-swing")
        .description("Doing placing swing.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> breakingswing = sgRender.add(new BoolSetting.Builder()
        .name("breaking-swing")
        .description("Doing breaking swing.")
        .defaultValue(false)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(0, 0, 0, 0))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(0, 0, 0, 0))
        .build()
    );

    public int a,b,x,y,z;
    public BlockPos pos;
    
    public MinerPlacer()
    {
        super(Categories.World, "MinerPlacer", "Break or Place in specific coordinate.");
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event)
    {
        if (pre.get())
            main();
    }
        
    @EventHandler
    private void onTickPre(TickEvent.Post event) 
    {
        if (post.get())
            main();
    }

    @EventHandler
    private void onRender(Render3DEvent event)
    {
        pos = new BlockPos(x,y,z);
        if(render.get())
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    public void main()
    {   
        pos = new BlockPos(x,y,z);

        switch(rotate.get())
        {
            case None -> {work();}
            case Client -> {clientAngle(); work();}
            case Packet -> {Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> {work();});}    
        }

        try
        {
            if (run.get()) execute(script.get().get(a).charAt(b));
            if (b != script.get().get(a).length()-1) b++;
        }
        catch(Exception e)
        {}
    }

    private void work()
    {
        if(attackBlock.get())
          attackingBlock();
      
        if(miningBlock.get())
          breakingBlock();
                    
        if(interactingBlock.get())
          interactingBlock();
    }
  
    public void attackingBlock()
    {
        mc.interactionManager.attackBlock(pos, getDirection(blockPos));
        mc.player.swingHand(usedBreakHand());
    }

    public void interactingBlock()
    {
         mc.interactionManager.interactBlock(mc.player, usedInteractHand(), new BlockHitResult(pos.toCenterPos(), direction(pos), pos, insideBlock.get()));
         mc.player.swingHand(usedInteractHand());
    }

    public void breakingBlock()
    {
        mc.interactionManager.updateBlockBreakingProgress(pos, direction(pos));
        mc.player.swingHand(usedBreakHand());
    }

    public Hand usedInteractHand()
    {
        switch(interactHand.get())
        {
            case Main -> {return Hand.MAIN_HAND;}
            case Off -> {return Hand.OFF_HAND;}
        }
        return null;
    }

    public Hand usedBreakHand()
    {
        switch(interactHand.get())
        {
            case Main -> {return Hand.MAIN_HAND;}
            case Off -> {return Hand.OFF_HAND;}
        }
        return null;
    }

    private void clientAngle()
    {
        mc.player.setYaw((float)Rotations.getYaw(pos)); 
        mc.player.setPitch((float)Rotations.getPitch(pos));
    }
    
    private void execute(char b)
    { 
        switch (b)
        {
            case 'X': x++; break;
            case 'Y': y++; break;
            case 'Z': z++; break;
            case 'x': x--; break;
            case 'y': y--; break;
            case 'z': z--; break;
            case '&': next();
            case '%': zeroing();
            case ';': restart();
            case '?': return;
            default: break;
        }
    }

    public Direction direction(BlockPos pos)
    {
        switch (cardinaldirection.get())
        {
            case Auto ->{return BlockUtils.getDirection(pos);}
            case Up -> {return Direction.UP;}
            case Down -> {return Direction.DOWN;}
            case North -> {return Direction.NORTH;}
            case South -> {return Direction.SOUTH;}
            case East -> {return Direction.EAST;}
            case West -> {return Direction.WEST;}     
        }
        return null;
    }
    public void next()
    {
        a++; b=0;   
    }
    public void restart()
    {
        a=0; b=0;
    }
    public void zeroing()
    {
        x=zero.get().getX();
        y=zero.get().getY();
        z=zero.get().getZ();
    }

    public WWidget getWidget(GuiTheme theme)
    {
        WVerticalList main = theme.verticalList();
        WVerticalList pm = theme.verticalList();
        WVerticalList nm = theme.verticalList();
        WVerticalList set = theme.verticalList();

        main.add(pm).expandX().widget();
        main.add(nm).expandX().widget();
        main.add(set).expandX().widget();
        
        WHorizontalList a = pm.add(theme.horizontalList()).expandX().widget();
        WHorizontalList b = nm.add(theme.horizontalList()).expandX().widget();
        WHorizontalList c = set.add(theme.horizontalList()).expandX().widget();
        
        WButton ix = a.add(theme.button("x++")).expandX().widget(); ix.action = () -> x++;
        WButton iy = a.add(theme.button("y++")).expandX().widget(); iy.action = () -> y++;
        WButton iz = a.add(theme.button("z++")).expandX().widget(); iz.action = () -> z++;
        WButton dx = b.add(theme.button("x--")).expandX().widget(); dx.action = () -> x--;
        WButton dy = b.add(theme.button("y--")).expandX().widget(); dy.action = () -> y--;
        WButton dz = b.add(theme.button("z--")).expandX().widget(); dz.action = () -> z--;
        WButton sx = c.add(theme.button("Set_X")).expandX().widget(); sx.action = () -> {x=zero.get().getX();};
        WButton sy = c.add(theme.button("Set_Y")).expandX().widget(); sy.action = () -> {y=zero.get().getY();};
        WButton sz = c.add(theme.button("Set_Z")).expandX().widget(); sz.action = () -> {z=zero.get().getZ();};
        WButton ab = set.add(theme.button("Attack_Block")).expandX().widget(); ab.action = () -> {attackingBlock();};
        WButton bb = set.add(theme.button("Break_Block")).expandX().widget(); ab.action = () -> {interactingBlock();};
        WButton ib = set.add(theme.button("Interact_Block")).expandX().widget(); ab.action = () -> {interactingBlock();};
        WButton rs = set.add(theme.button("Restart_Script")).expandX().widget(); rs.action = () -> {restart();};

        return main;
    }

    public enum Rotate
    {
        None,
        Client,
        Packet
    }
    
    public enum UseHand
    {
        Main,
        Off
    }
    
    public enum CardinalDirections
    {
        Auto,
        Up,
        Down,
        North,
        South,
        East,
        West
    }
}
