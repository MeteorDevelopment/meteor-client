/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.screens.NotebotHelpScreen;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.containers.WTable;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.notebot.NBSDecoder;
import minegame159.meteorclient.utils.notebot.nbs.Layer;
import minegame159.meteorclient.utils.notebot.nbs.Note;
import minegame159.meteorclient.utils.notebot.nbs.Song;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.Rotations;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Notebot extends Module {

    public enum InstrumentType {
        Any,
        Harp,
        Bass,
        Bells,
        Flute,
        Chimes,
        Guitar,
        Xylophone,
        Iron_Xylophone,
        Cow_Bell,
        Didgeridoo,
        Bit,
        Banjo,
        Pling
    }

    private enum Stage {
        None,
        SetUp,
        Tune,
        Playing,
        Preview
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render",false);

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("tick-delay")
            .description("The delay when loading a song.")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<InstrumentType> instrument = sgGeneral.add(new EnumSetting.Builder<InstrumentType>()
        .name("instrument")
        .description("Select which instrument will be played")
        .defaultValue(InstrumentType.Any)
        .build()
    );

    private final Setting<Boolean> moveNotes = sgGeneral.add(new BoolSetting.Builder()
        .name("move-notes")
        .description("Move notes by one tick, if multiple notes supposed to play in one tick.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Whether or not to render the outline around the noteblocks.")
            .defaultValue(true)
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
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    private final List<BlockPos> possibleBlockPos = new ArrayList<>(Collections.emptyList());

    private Stage stage = Stage.None;
    private boolean isPlaying = false;
    private final HashMap<Integer,Integer> song = new HashMap<>();
    private final List<Integer> uniqueNotes = new ArrayList<>(Collections.emptyList());
    private final HashMap<Integer, BlockPos> blockPositions = new HashMap<>();
    private final List<BlockPos> scannedNoteblocks = new ArrayList<>();
    private int currentNote = 0;
    private int lastKey = -1;
    private int offset = 0;
    private int ticks = 0;
    private boolean noSongsFound = true;

    public Notebot() {
        super(Categories.Misc, "notebot","Plays noteblock nicely");
        for (int y = -5; y < 5; y++) {
            for (int x = -5; x < 5; x++) {
                if (y!=0||x!=0) {
                    BlockPos pos = new BlockPos(x, 0, y);
                    if (pos.getSquaredDistance(0, 0, 0, true) < (4.3*4.3)-0.5) {
                        possibleBlockPos.add(pos);
                    }
                }
            }
        }
        possibleBlockPos.sort((o1, o2) -> {
            double d1 = o1.getSquaredDistance(new Vec3i(0,0,0));
            double d2 = o2.getSquaredDistance(new Vec3i(0,0,0));
            return Double.compare(d1,d2);
        });
    }
    
    @Override
    public String getInfoString() {
        return stage.toString();
    }

    @Override
    public void onActivate() {
        ticks=0;
        resetVariables();
    }

    private void resetVariables() {
        currentNote=0;
        offset=0;
        lastKey=0;
        isPlaying=false;
        stage=Stage.None;
        song.clear();
        blockPositions.clear();
        uniqueNotes.clear();
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (!render.get()) return;
        if (stage!=Stage.SetUp && stage!=Stage.Tune) {
            if (!isPlaying) return;
        }
        blockPositions.values().forEach((blockPos) -> {

            double x1 = blockPos.getX();
            double y1 = blockPos.getY();
            double z1 = blockPos.getZ();
            double x2 = blockPos.getX() + 1;
            double y2 = blockPos.getY() + 1;
            double z2 = blockPos.getZ() + 1;

            Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        });
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        ticks++;
        switch (stage) {
            case Preview: {
                onTickPreview();
                break;
            }
            case SetUp: {
                onTickSetup();
                break;
            }
            case Tune: {
                onTickTune();
                break;
            }
            case Playing: {
                onTickPlay();
                break;
            }
        default:
            break;
        }
    }


    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WLabel status = table.add(theme.label(getStatus())).expandCellX().widget();
        WButton pause = table.add(theme.button(isPlaying?"Pause":"Resume")).right().widget();
        pause.action = () -> {
            Pause();
            pause.set(isPlaying?"Pause":"Resume");
            status.set(getStatus());
        };
        WButton stop = table.add(theme.button("Stop")).right().widget();
        stop.action = () -> {
            Stop();
            status.set(getStatus());
        };
        table.row();
        noSongsFound = true;
        try {
            Files.list(MeteorClient.FOLDER.toPath().resolve("notebot")).forEach(path -> {
                if (isValidFile(path)) {
                    noSongsFound = false;
                    table.add(theme.label(getFileLabel(path))).expandCellX();
                    WButton load = table.add(theme.button("Load")).right().widget();
                    load.action = () -> {
                        loadSong(path.toFile());
                        status.set(getStatus());
                    };
                    WButton preview = table.add(theme.button("Preview")).right().widget();
                    preview.action = () -> {
                        previewSong(path.toFile());
                        status.set(getStatus());
                    };
                    table.row();
                }
            });
        }  catch (IOException e) {
            table.add(theme.label("Missing \"notebot\" folder.")).expandCellX();
            table.row();
        }
        if (noSongsFound) {
            table.add(theme.label("No songs found.")).expandCellX();
            table.row();
            WButton help = table.add(theme.button("Help")).expandCellX().widget();
            help.action = () -> {
                mc.openScreen(new NotebotHelpScreen(theme));
            };
        }
        return table;
    }

    private String getStatus() {
        if (!this.isActive()) return "Module disabled.";
        if (song.isEmpty()) return "No song loaded.";
        if (isPlaying) return String.format("Playing song. %d/%d",currentNote,lastKey);
        if (stage == Stage.Playing || stage == Stage.Preview) return "Ready to play.";
        if (stage == Stage.SetUp || stage == Stage.Tune) return "Setting up the noteblocks.";
        else return String.format("Stage: %s.", stage.toString());
    }

    public void printStatus() {
        ChatUtils.moduleInfo(this,  getStatus());
    }

    private String getFileLabel(Path file) {
        return file
                .getFileName()
                .toString()
                .replace(".txt","")
                .replace(".nbs","");
    }

    private boolean isValidFile(Path file) {
        String extension = FilenameUtils.getExtension(file.toFile().getName());
        if (extension.equals("txt")) return true;
        else return extension.equals("nbs");
    }

    public void Play() {
        if (mc.player == null) return;
        if (mc.player.abilities.creativeMode && stage != Stage.Preview) {
            ChatUtils.moduleError(this, "You need to be in survival mode.");
        }
        else if (stage == Stage.Preview || stage == Stage.Playing) {
            isPlaying = true;
            ChatUtils.moduleInfo(this, "Playing.");
        } else {
            ChatUtils.moduleError(this, "No song loaded.");
        }
    }

    public void Pause() {
        if (!isActive()) toggle();
        if (isPlaying) {
            ChatUtils.moduleInfo(this, "Pausing.");
            isPlaying = false;
        } else {
            ChatUtils.moduleInfo(this, "Resuming.");
            isPlaying = true;
        }
    }

    public void Stop() {
        ChatUtils.moduleInfo(this, "Stopping.");
        if (stage == Stage.SetUp || stage == Stage.Tune) {
            resetVariables();
        } else {
            isPlaying = false;
            currentNote = 0;
        }
    }

    public void Disable() {
        resetVariables();
        ChatUtils.moduleInfo(this, "Stopping.");
        if (!isActive()) toggle();
    }

    public void loadSong(File file) {
        if (!isActive()) toggle();
        if (!loadFileToMap(file)) return;
        if (!setupBlocks()) return;
        ChatUtils.moduleInfo(this, "Loading song \"%s\".", getFileLabel(file.toPath()));
    }

    public void previewSong(File file) {
        if (!isActive()) toggle();
        if (loadFileToMap(file)) {
            ChatUtils.moduleInfo(this, "Song \"%s\" loaded.",getFileLabel(file.toPath()));
            stage = Stage.Preview;
            Play();
        }
    }

    private boolean loadFileToMap(File file) {
        if (!file.exists() || !file.isFile()) {
            ChatUtils.moduleError(this, "File not found");
            return false;
        }
        String extension = FilenameUtils.getExtension(file.getName());
        if (extension.equals("txt")) return loadTextFile(file);
        else if (extension.equals("nbs")) return loadNbsFile(file);
        return false;
    }

    private boolean loadTextFile(File file) {
        List<String> data;
        try {
            data = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            ChatUtils.moduleError(this, "Error while reading \"%s\"",file.getName());
            return false;
        }
        resetVariables();
        for (int i = 0; i < data.size(); i++) {
            String[] parts = data.get(i).split(":");
            if (parts.length<2) {
                ChatUtils.moduleWarning(this, "Malformed line %d", i);
                continue;
            }
            int key;
            int val;
            try {
                key = Integer.parseInt(parts[0]);
                val = Integer.parseInt(parts[1]);
                if (parts.length>2) {
                    int type = Integer.parseInt(parts[2]);
                    if (type == 1) continue; //basedrum
                    if (type == 2) continue; //snare
                    if (type == 3) continue; //hat
                    if (type == 11) continue; //cow_bell
                }
            } catch (NumberFormatException e) {
                ChatUtils.moduleWarning(this, "Invalid character at line %d", i);
                continue;
            }
            if (moveNotes.get() && song.containsKey(key)) {
                song.put(key+1, val);
                lastKey = key+1;
            } else {
                song.put(key,val);
                lastKey = key;
            }
            
        }
        return true;
    }

    
    private boolean loadNbsFile(File file) {
        Song nbsSong = NBSDecoder.parse(file);
        if (nbsSong == null) {
            ChatUtils.moduleError(this, "Couldn't parse the file. Only classic and opennbs v5 are supported");
            return false;
        }
        List<Layer> layers = new ArrayList<>(nbsSong.getLayerHashMap().values());
        resetVariables();
        for (Layer layer : layers) {
            for (int tick : layer.getHashMap().keySet()) {
                Note note = layer.getNote(tick);
                tick *= nbsSong.getDelay();
                if (note == null) continue;
                byte instrument = note.getInstrument();
                if (instrument == 2) continue;
                if (instrument == 3) continue;
                if (instrument == 4) continue;
                int n = Byte.toUnsignedInt(note.getKey());
                n -= 33; // amazing conversion
                if (n<0 || n>24) {
                    ChatUtils.moduleWarning(this, "Note at tick %d out of range.", tick);
                    continue;
                }
                if (moveNotes.get() && song.containsKey(tick)) {
                    song.put(tick+1, n);
                    lastKey = tick+1;
                } else {
                    song.put(tick,n);
                    lastKey = tick;
                }
                
            }
        }
        return true;
    }
    

    private void scanForNoteblocks() {
        if (mc.interactionManager==null || mc.world == null || mc.player == null) return;
        scannedNoteblocks.clear();
        int min = (int)(-mc.interactionManager.getReachDistance())-1;
        int max = (int)mc.interactionManager.getReachDistance()+1;
        // 5^3 kek
        for (int x = min; x < max; x++) {
            for (int y = min; y < max; y++) {
                for (int z = min; z < max; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x,y,z);
                    if (mc.world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) continue;
                    float reach = mc.interactionManager.getReachDistance();
                    reach = reach*reach; //^2
                    if (pos.getSquaredDistance(mc.player.getPos(),false) > reach) continue;
                    if (!isValidScanSpot(pos) || !isValidInstrument(pos)) continue;
                    scannedNoteblocks.add(pos);
                }
            }

        }
    }

    private boolean setupBlocks() {
        song.values().forEach((v) -> {
            if (!uniqueNotes.contains(v)) {
                uniqueNotes.add(v);
            }
        });
        scanForNoteblocks();
        if (uniqueNotes.size() > possibleBlockPos.size()+scannedNoteblocks.size()) {
            ChatUtils.moduleError(this, "Too many notes. %d is the maximum.", possibleBlockPos.size());
            return false;
        }
        currentNote = 0;
        offset = 0;
        stage = Stage.SetUp;
        return true;
    }

    private void onTickPreview() {
        if (isPlaying && mc.player != null) {
            if (currentNote >= lastKey) {
                Stop();
            }
            if (song.containsKey(currentNote)) {
                mc.player.playSound(getInstrumentSound(), 2f, (float) Math.pow(2.0D, (song.get(currentNote) - 12) / 12.0D));
            }
            currentNote++;
        }
    }

    private SoundEvent getInstrumentSound() {
        switch (instrument.get()) {
            case Bass:
                return SoundEvents.BLOCK_NOTE_BLOCK_BASS;
            case Bells:
                return SoundEvents.BLOCK_NOTE_BLOCK_BELL;
            case Flute:
                return SoundEvents.BLOCK_NOTE_BLOCK_FLUTE;
            case Chimes:
                return SoundEvents.BLOCK_NOTE_BLOCK_CHIME;
            case Guitar:
                return SoundEvents.BLOCK_NOTE_BLOCK_GUITAR;
            case Xylophone:
                return SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE;
            case Iron_Xylophone:
                return SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE;
            case Cow_Bell:
                return SoundEvents.BLOCK_NOTE_BLOCK_COW_BELL;
            case Didgeridoo:
                return SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO;
            case Bit:
                return SoundEvents.BLOCK_NOTE_BLOCK_BIT;
            case Banjo:
                return SoundEvents.BLOCK_NOTE_BLOCK_BANJO;
            case Pling:
                return SoundEvents.BLOCK_NOTE_BLOCK_PLING;
            default:
                return SoundEvents.BLOCK_NOTE_BLOCK_HARP;
        }
    }

    private void onTickSetup() {
        if (ticks<tickDelay.get()) return;
        ticks = 0;
        if (currentNote>=uniqueNotes.size()) {
            stage = Stage.Playing;
            ChatUtils.moduleInfo(this, "Loading done.");
            Play();
            return;
        }
        int index = currentNote+offset;
        BlockPos pos;
        if (index<scannedNoteblocks.size()) {
            pos = scannedNoteblocks.get(index);
            if (mc.world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) {
                offset++;
            } else {
                blockPositions.put(uniqueNotes.get(currentNote), pos);
                stage = Stage.Tune;
            }
            return;
        }
        int slot = InvUtils.findItemInHotbar(Items.NOTE_BLOCK);
        if (slot == -1) {
            ChatUtils.moduleError(this, "Not enough noteblocks");
            Disable();
            return;
        }
        index-=scannedNoteblocks.size();
        try {
            pos = mc.player.getBlockPos().add(possibleBlockPos.get(index));
        } catch (IndexOutOfBoundsException e) {
            ChatUtils.moduleError(this, "Not enough valid positions.");
            Disable();
            return;
        }
        if (!isValidEmptySpot(pos) || !isValidInstrument(pos)) {
            offset++;
            return;
        }
        if (!BlockUtils.place(pos, Hand.MAIN_HAND, slot, true, 100, true)) {
            offset++;
            return;
        } else {
            blockPositions.put(uniqueNotes.get(currentNote), pos);
            stage = Stage.Tune;
        }
    }


    private void onTickTune() {
        if (ticks<tickDelay.get()) return;
        ticks = 0;
        BlockPos pos = blockPositions.get(uniqueNotes.get(currentNote));
        if (pos == null) return;
        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, this::tuneRotate);
    }

    private void tuneRotate() {
        BlockPos pos = blockPositions.get(uniqueNotes.get(currentNote));
        if (pos == null) return;
        if (!tuneBlock(pos, uniqueNotes.get(currentNote))) {
            Disable();
        }
    }

    private boolean tuneBlock(BlockPos pos, int note) {
        if (mc.world == null || mc.player == null) return false;

        BlockState block = mc.world.getBlockState(pos);
        if (block.getBlock() != Blocks.NOTE_BLOCK) {
            offset++;
            stage = Stage.SetUp;
            return true;
        }

        if (block.get(NoteBlock.NOTE).equals(note)) {
            currentNote++;
            stage = Stage.SetUp;
            return true;
        }
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,  new BlockHitResult(
                mc.player.getPos(), rayTraceCheck(pos,true), pos, true)));
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private void onTickPlay() {
        if (!isPlaying) return;
        if (song == null) return;
        if (currentNote >= lastKey) {
            Stop();
            return;
        }
        if (song.containsKey(currentNote)) {
            int note = song.get(currentNote);
            BlockPos pos = blockPositions.get(note);
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, this::playRotate);
        }  else {
            currentNote++;
        }
    }

    private void playRotate() {
        if (mc.interactionManager == null) return;
        try {
            int note = song.get(currentNote);
            BlockPos pos = blockPositions.get(note);

            mc.interactionManager.attackBlock(pos,Direction.DOWN);
            currentNote++;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidEmptySpot(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.NOTE_BLOCK) return false;
        return true;
    }

    private boolean isValidScanSpot(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        return true;
    }

    private boolean isValidInstrument(BlockPos pos) {
        switch (instrument.get()) {
            case Any:
                return true;

            case Harp: {
                BlockState state = mc.world.getBlockState(pos);
                if (state.getBlock() == Blocks.NOTE_BLOCK) {
                    return (state.get(NoteBlock.INSTRUMENT) == Instrument.HARP);
                } else {
                    BlockState block = mc.world.getBlockState(pos.down());
                    if (block.getMaterial() == Material.WOOD) return false;
                    else if (block.getMaterial() == Material.AGGREGATE) return false;
                    else if (block.getMaterial() == Material.GLASS) return false;
                    else if (block.getMaterial() == Material.STONE) return false;
                    else if (block.getBlock() == Blocks.GOLD_BLOCK) return false;
                    else if (block.getBlock() == Blocks.CLAY) return false;
                    else if (block.getBlock() == Blocks.PACKED_ICE) return false;
                    else if (block.getMaterial() == Material.WOOL) return false;
                    else if (block.getBlock() == Blocks.BONE_BLOCK) return false;
                    else if (block.getBlock() == Blocks.IRON_BLOCK) return false;
                    else if (block.getBlock() == Blocks.SOUL_SAND) return false;
                    else if (block.getBlock() == Blocks.PUMPKIN) return false;
                    else if (block.getBlock() == Blocks.EMERALD_BLOCK) return false;
                    else if (block.getBlock() == Blocks.HAY_BLOCK) return false;
                    else if (block.getBlock() == Blocks.GLOWSTONE) return false;
                    else return true;
                }
            }
            case Banjo: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.HAY_BLOCK);
            }
            case Bass: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getMaterial() == Material.WOOD);
            }
            case Bells: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.GOLD_BLOCK);
            }
            case Bit: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.EMERALD_BLOCK);
            }
            case Chimes: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.PACKED_ICE);
            }
            case Cow_Bell: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.SOUL_SAND);
            }
            case Didgeridoo: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.PUMPKIN);
            }
            case Flute: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.CLAY);
            }
            case Guitar: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getMaterial() == Material.WOOL);
            }
            case Iron_Xylophone: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.IRON_BLOCK);
            }
            case Pling: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.GLOWSTONE);
            }
            case Xylophone: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.BONE_BLOCK);
            }
            default:
                return false;
        }

    }

    // Stolen from crystal aura :)
    private Direction rayTraceCheck(BlockPos pos, boolean forceReturn) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                    pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                    pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }
        if (forceReturn) { // When we're placing, we have to return a direction so we have a side to place against
            if ((double) pos.getY() > eyesPos.y) {
                return Direction.DOWN; // The player can never see the top of a block if they are under it
            }
            return Direction.UP;
        }
        return null;
    }
}
