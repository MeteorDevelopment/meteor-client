/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.notebot.decoder.*;
import meteordevelopment.meteorclient.utils.notebot.NotebotUtils;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import meteordevelopment.meteorclient.utils.notebot.song.Song;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Notebot extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgNoteMap = settings.createGroup("Note Map", false);
    private final SettingGroup sgRender = settings.createGroup("Render", true);

    public final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("The delay when loading a song.")
        .defaultValue(1)
        .sliderRange(1, 20)
        .min(1)
        .build()
    );

    public final Setting<Integer> concurrentTuneBlocks = sgGeneral.add(new IntSetting.Builder()
        .name("concurrent-tune-blocks")
        .description("How many noteblocks can be tuned at the same time. On Paper it is recommended to set it to 1 to avoid bugs.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    public final Setting<NotebotUtils.NotebotMode> mode = sgGeneral.add(new EnumSetting.Builder<NotebotUtils.NotebotMode>()
        .name("mode")
        .description("Select mode of notebot")
        .defaultValue(NotebotUtils.NotebotMode.ExactInstruments)
        .build()
    );

    public final Setting<Boolean> polyphonic = sgGeneral.add(new BoolSetting.Builder()
        .name("polyphonic")
        .description("Whether or not to allow multiple notes to be played at the same time")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> autoRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-rotate")
        .description("Should client look at note block when it wants to hit it")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> autoPlay = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-play")
        .description("Auto plays random songs")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> roundOutOfRange = sgGeneral.add(new BoolSetting.Builder()
        .name("round-out-of-range")
        .description("Rounds out of range notes")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> swingArm = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-arm")
        .description("Should swing arm on hit")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> checkNoteblocksAgainDelay = sgGeneral.add(new IntSetting.Builder()
        .name("check-noteblocks-again-delay")
        .description("How much delay should be between end of tuning and checking again")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    public final Setting<Boolean> renderText = sgRender.add(new BoolSetting.Builder()
        .name("render-text")
        .description("Whether or not to render the text above noteblocks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> renderBoxes = sgRender.add(new BoolSetting.Builder()
        .name("render-boxes")
        .description("Whether or not to render the outline around the noteblocks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public final Setting<SettingColor> untunedSideColor = sgRender.add(new ColorSetting.Builder()
        .name("untuned-side-color")
        .description("The color of the sides of the untuned blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    public final Setting<SettingColor> untunedLineColor = sgRender.add(new ColorSetting.Builder()
        .name("untuned-line-color")
        .description("The color of the lines of the untuned blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    public final Setting<SettingColor> tunedSideColor = sgRender.add(new ColorSetting.Builder()
        .name("tuned-side-color")
        .description("The color of the sides of the tuned blocks being rendered.")
        .defaultValue(new SettingColor(0, 204, 0, 10))
        .build()
    );

    public final Setting<SettingColor> tunedLineColor = sgRender.add(new ColorSetting.Builder()
        .name("tuned-line-color")
        .description("The color of the lines of the tuned blocks being rendered.")
        .defaultValue(new SettingColor(0, 204, 0, 255))
        .build()
    );

    public final Setting<SettingColor> tuneHitSideColor = sgRender.add(new ColorSetting.Builder()
        .name("hit-side-color")
        .description("The color of the sides being rendered on noteblock tune hit.")
        .defaultValue(new SettingColor(255, 153, 0, 10))
        .build()
    );

    private final Setting<SettingColor> tuneHitLineColor = sgRender.add(new ColorSetting.Builder()
        .name("hit-line-color")
        .description("The color of the lines being rendered on noteblock tune hit.")
        .defaultValue(new SettingColor(255, 153, 0, 255))
        .build()
    );

    public final Setting<SettingColor> scannedNoteblockSideColor = sgRender.add(new ColorSetting.Builder()
        .name("scanned-noteblock-side-color")
        .description("The color of the sides of the scanned noteblocks being rendered.")
        .defaultValue(new SettingColor(255, 255, 0, 30))
        .build()
    );

    private final Setting<SettingColor> scannedNoteblockLineColor = sgRender.add(new ColorSetting.Builder()
        .name("scanned-noteblock-line-color")
        .description("The color of the lines of the scanned noteblocks being rendered.")
        .defaultValue(new SettingColor(255, 255, 0, 255))
        .build()
    );

    public final Setting<Double> noteTextScale = sgRender.add(new DoubleSetting.Builder()
        .name("note-text-scale")
        .description("The scale.")
        .defaultValue(1.5)
        .min(0)
        .build()
    );

    public final Setting<Boolean> showScannedNoteblocks = sgRender.add(new BoolSetting.Builder()
        .name("show-scanned-noteblocks")
        .description("Show scanned Noteblocks")
        .defaultValue(false)
        .build()
    );

    private CompletableFuture<Song> loadingSongFuture = null;

    private Song song; // Loaded song
    private final Map<Note, BlockPos> noteBlockPositions = new HashMap<>(); // Currently used noteblocks by the song
    private final Multimap<Note, BlockPos> scannedNoteblocks = MultimapBuilder.linkedHashKeys().arrayListValues().build(); // Found noteblocks
    private final List<BlockPos> clickedBlocks = new ArrayList<>();
    private Stage stage = Stage.None;
    private boolean isPlaying = false;
    private int currentTick = 0;
    private int ticks = 0;
    private WLabel status;

    private boolean anyNoteblockTuned = false;
    private final Map<BlockPos, Integer> tuneHits = new HashMap<>(); // noteblock -> target hits number

    private int waitTicks = -1;


    public Notebot() {
        super(Categories.Misc, "notebot", "Plays noteblock nicely");

        for (Instrument inst : Instrument.values()) {
            NotebotUtils.OptionalInstrument optionalInstrument = NotebotUtils.OptionalInstrument.fromMinecraftInstrument(inst);
            if (optionalInstrument != null) {
                sgNoteMap.add(new EnumSetting.Builder<NotebotUtils.OptionalInstrument>()
                    .name(beautifyText(inst.name()))
                    .defaultValue(optionalInstrument)
                    .visible(() -> mode.get() == NotebotUtils.NotebotMode.ExactInstruments)
                    .build()
                );
            }
        }
    }

    @Override
    public String getInfoString() {
        return stage.toString();
    }

    @Override
    public void onActivate() {
        ticks = 0;
        resetVariables();
    }

    private void resetVariables() {
        if (loadingSongFuture != null) {
            loadingSongFuture.cancel(true);
            loadingSongFuture = null;
        }
        clickedBlocks.clear();
        tuneHits.clear();
        anyNoteblockTuned = false;
        currentTick = 0;
        isPlaying = false;
        stage = Stage.None;
        song = null;
        noteBlockPositions.clear();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderBoxes.get()) return;

        if (stage != Stage.SetUp && stage != Stage.Tune && stage != Stage.WaitingToCheckNoteblocks && !isPlaying) return;

        if (showScannedNoteblocks.get()) {
            for (BlockPos blockPos : scannedNoteblocks.values()) {
                double x1 = blockPos.getX();
                double y1 = blockPos.getY();
                double z1 = blockPos.getZ();
                double x2 = blockPos.getX() + 1;
                double y2 = blockPos.getY() + 1;
                double z2 = blockPos.getZ() + 1;

                event.renderer.box(x1, y1, z1, x2, y2, z2, scannedNoteblockSideColor.get(), scannedNoteblockLineColor.get(), shapeMode.get(), 0);
            }
        } else {
            for (var entry : noteBlockPositions.entrySet()) {
                Note note = entry.getKey();
                BlockPos blockPos = entry.getValue();

                BlockState state = mc.world.getBlockState(blockPos);
                if (state.getBlock() != Blocks.NOTE_BLOCK) continue;

                int level = state.get(NoteBlock.NOTE);

                double x1 = blockPos.getX();
                double y1 = blockPos.getY();
                double z1 = blockPos.getZ();
                double x2 = blockPos.getX() + 1;
                double y2 = blockPos.getY() + 1;
                double z2 = blockPos.getZ() + 1;

                // Render boxes around noteblocks in use

                Color sideColor;
                Color lineColor;
                if (clickedBlocks.contains(blockPos)) {
                    sideColor = tuneHitSideColor.get();
                    lineColor = tuneHitLineColor.get();
                } else {
                    if (note.getNoteLevel() == level) {
                        sideColor = tunedSideColor.get();
                        lineColor = tunedLineColor.get();
                    } else {
                        sideColor = untunedSideColor.get();
                        lineColor = untunedLineColor.get();
                    }
                }

                event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode.get(), 0);
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!renderText.get()) return;

        if (stage != Stage.SetUp && stage != Stage.Tune && stage != Stage.WaitingToCheckNoteblocks && !isPlaying) return;

        Vec3 pos = new Vec3();

        for (BlockPos blockPos : noteBlockPositions.values()) {
            BlockState state = mc.world.getBlockState(blockPos);
            if (state.getBlock() != Blocks.NOTE_BLOCK) continue;

            double x = blockPos.getX() + 0.5;
            double y = blockPos.getY() + 1;
            double z = blockPos.getZ() + 0.5;

            pos.set(x, y, z);

            // Render level text logic

            String levelText = String.valueOf(state.get(NoteBlock.NOTE));
            String tuneHitsText = null;
            if (tuneHits.containsKey(blockPos)) {
                tuneHitsText = " -" + tuneHits.get(blockPos);
            }

            if (!NametagUtils.to2D(pos, noteTextScale.get(), true)) {
                continue;
            }

            TextRenderer text = TextRenderer.get();

            NametagUtils.begin(pos);
            text.beginBig();

            double xScreen = text.getWidth(levelText) / 2.0;
            if (tuneHitsText != null) {
                xScreen += text.getWidth(tuneHitsText) / 2.0;
            }

            double hX = text.render(levelText, -xScreen, 0, Color.GREEN);
            if (tuneHitsText != null) {
                text.render(tuneHitsText, hX, 0, Color.RED);
            }
            text.end();

            NametagUtils.end();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        ticks++;
        clickedBlocks.clear();

        if (stage == Stage.WaitingToCheckNoteblocks) {
            waitTicks--;
            if (waitTicks == 0) {
                waitTicks = -1;
                info("Checking noteblocks again...");

                setupTuneHitsMap();
                stage = Stage.Tune;
            }
        }
        else if (stage == Stage.SetUp) {
            scanForNoteblocks();
            if (scannedNoteblocks.isEmpty()) {
                error("Can't find any nearby noteblock!");
                forceStop();
                return;
            }

            setupNoteblocksMap();
            if (noteBlockPositions.isEmpty()) {
                error("Can't find any valid noteblock to play song.");
                forceStop();
                return;
            }
            setupTuneHitsMap();
            stage = Stage.Tune;
        }
        else if (stage == Stage.Tune) {
            tune();
        }
        else if (stage == Stage.Preview || stage == Stage.Playing) {
            if (!isPlaying) return;

            if (mc.player == null || currentTick > song.getLastTick()) {
                stop();
                return;
            }

            if (song.getNotesMap().containsKey(currentTick)) {
                if (stage == Stage.Preview) onTickPreview();
                else onTickPlay();
            }

            currentTick++;

            updateStatus();
        }
    }

    private void setupNoteblocksMap() {
        noteBlockPositions.clear();

        List<Note> uniqueNotesToUse = new ArrayList<>(song.getRequirements());
        Map<Instrument, List<BlockPos>> incorrectNoteBlocks = new HashMap<>();

        // Check if there are already tuned noteblocks
        for (var entry : scannedNoteblocks.asMap().entrySet()) {
            Note note = entry.getKey();
            List<BlockPos> noteblocks = new ArrayList<>(entry.getValue());

            if (uniqueNotesToUse.contains(note)) {
                noteBlockPositions.put(note, noteblocks.remove(0));
                uniqueNotesToUse.remove(note);
            }

            if (!noteblocks.isEmpty()) {
                // Add excess for mapping process note -> block pos

                if (!incorrectNoteBlocks.containsKey(note.getInstrument())) {
                    incorrectNoteBlocks.put(note.getInstrument(), new ArrayList<>());
                }

                incorrectNoteBlocks.get(note.getInstrument()).addAll(noteblocks);
            }
        }

        // Map note -> block pos
        for (var entry : incorrectNoteBlocks.entrySet()) {
            List<BlockPos> positions = entry.getValue();

            if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
                Instrument inst = entry.getKey();

                List<Note> foundNotes = uniqueNotesToUse.stream()
                    .filter(note -> note.getInstrument() == inst)
                    .collect(Collectors.toList());

                if (foundNotes.isEmpty()) continue;

                for (BlockPos pos : positions) {
                    if (foundNotes.isEmpty()) break;

                    Note note = foundNotes.remove(0);
                    noteBlockPositions.put(note, pos);

                    uniqueNotesToUse.remove(note);
                }
            } else {
                for (BlockPos pos : positions) {
                    if (uniqueNotesToUse.isEmpty()) break;

                    Note note = uniqueNotesToUse.remove(0);
                    noteBlockPositions.put(note, pos);
                }
            }
        }

        if (!uniqueNotesToUse.isEmpty()) {
            for (Note note : uniqueNotesToUse) {
                warning("Missing note: "+note.getInstrument()+", "+note.getNoteLevel());
            }
            warning(uniqueNotesToUse.size()+" missing notes!");
        }
    }

    private void setupTuneHitsMap() {
        tuneHits.clear();

        for (var entry : noteBlockPositions.entrySet()) {
            int targetLevel = entry.getKey().getNoteLevel();
            BlockPos blockPos = entry.getValue();

            BlockState blockState = mc.world.getBlockState(blockPos);
            int currentLevel = blockState.get(NoteBlock.NOTE);

            if (targetLevel != currentLevel) {
                tuneHits.put(blockPos, calcNumberOfHits(currentLevel, targetLevel));
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();

        // Open Song GUI
        WButton openSongGUI = table.add(theme.button("Open Song GUI")).expandX().minWidth(100).widget();
        openSongGUI.action = () -> mc.setScreen(theme.notebotSongs());

        table.row();

        // Align Center
        WButton alignCenter = table.add(theme.button("Align Center")).expandX().minWidth(100).widget();
        alignCenter.action = () -> {
            if (mc.player == null) return;
            mc.player.setPosition(Vec3d.ofBottomCenter(mc.player.getBlockPos()));
        };

        table.row();

        // Label
        status = table.add(theme.label(getStatus())).expandCellX().widget();

        // Pause
        WButton pause = table.add(theme.button(isPlaying ? "Pause" : "Resume")).right().widget();
        pause.action = () -> {
            pause();
            pause.set(isPlaying ? "Pause" : "Resume");
            updateStatus();
        };

        // Stop
        WButton stop = table.add(theme.button("Stop")).right().widget();
        stop.action = this::forceStop;

        return table;
    }


    public String getStatus() {
        if (!this.isActive()) return "Module disabled.";
        if (song == null) return "No song loaded.";
        if (isPlaying) return String.format("Playing song. %d/%d", currentTick, song.getLastTick());
        if (stage == Stage.Playing || stage == Stage.Preview) return "Ready to play.";
        if (stage == Stage.SetUp || stage == Stage.Tune || stage == Stage.WaitingToCheckNoteblocks) return "Setting up the noteblocks.";
        else return String.format("Stage: %s.", stage.toString());
    }

    public void play() {
        if (mc.player == null) return;
        if (mc.player.getAbilities().creativeMode && stage != Stage.Preview) {
            error("You need to be in survival mode.");
        } else if (stage == Stage.Preview || stage == Stage.Playing) {
            isPlaying = true;
            info("Playing.");
        } else {
            error("No song loaded.");
        }
    }

    public void pause() {
        if (!isActive()) toggle();
        if (isPlaying) {
            info("Pausing.");
            isPlaying = false;
        } else {
            info("Resuming.");
            isPlaying = true;
        }
    }

    public void forceStop() {
        info("Stopping.");
        if (stage == Stage.SetUp || stage == Stage.Tune || stage == Stage.WaitingToCheckNoteblocks || stage == Stage.LoadingSong) {
            resetVariables();
        } else {
            isPlaying = false;
            currentTick = 0;
        }
        updateStatus();
        disable();
    }

    public void stop() {
        if (autoPlay.get() && stage != Stage.Preview) {
            playRandomSong();
        } else {
            forceStop();
        }
    }

    public void playRandomSong() {
        File[] files = MeteorClient.FOLDER.toPath().resolve("notebot").toFile().listFiles();
        if (files == null) return;

        File randomSong = files[ThreadLocalRandom.current().nextInt(files.length)];
        if (SongDecoders.hasDecoder(randomSong)) {
            loadSong(randomSong);
        } else {
            playRandomSong();
        }
    }

    public void disable() {
        resetVariables();
        if (!isActive()) toggle();
    }

    public void loadSong(File file) {
        if (!isActive()) toggle();
        if (!loadFileToMap(file, () -> stage = Stage.SetUp)) {
            if (autoPlay.get()) {
                playRandomSong();
            }
        }
        updateStatus();
    }

    public void previewSong(File file) {
        if (!isActive()) toggle();
        loadFileToMap(file, () -> {
            stage = Stage.Preview;
            play();
        });
        updateStatus();
    }

    private boolean loadFileToMap(File file, Runnable callback) {
        if (!file.exists() || !file.isFile()) {
            error("File not found");
            return false;
        }

        if (!SongDecoders.hasDecoder(file)) {
            error("File is in wrong format. Decoder not found.");
            return false;
        }
        resetVariables();
        info("Loading song \"%s\".", FilenameUtils.getBaseName(file.getName()));

        loadingSongFuture = CompletableFuture.supplyAsync(() -> SongDecoders.parse(file));
        loadingSongFuture.completeOnTimeout(null, 10, TimeUnit.SECONDS);

        stage = Stage.LoadingSong;
        long time1 = System.currentTimeMillis();
        loadingSongFuture.thenAccept(song -> {
            if (song != null) {
                this.song = song;
                long time2 = System.currentTimeMillis();
                long diff = time2 - time1;

                info("Song '"+FilenameUtils.getBaseName(file.getName())+"' has been loaded to the memory! Took "+diff+"ms");
                callback.run();
            } else {
                error("Could not load song '"+FilenameUtils.getBaseName(file.getName())+"'");
                if (autoPlay.get()) {
                    playRandomSong();
                }
            }
        });
        return true;
    }

    private void scanForNoteblocks() {
        if (mc.interactionManager == null || mc.world == null || mc.player == null) return;
        scannedNoteblocks.clear();
        int min = (int) (-mc.interactionManager.getReachDistance()) - 2;
        int max = (int) mc.interactionManager.getReachDistance() + 2;

        // Scan for noteblocks horizontally
        // 6^3 kek
        for (int y = min; y < max; y++) {
            for (int x = min; x < max; x++) {
                for (int z = min; z < max; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y + 1, z);

                    BlockState blockState = mc.world.getBlockState(pos);
                    if (blockState.getBlock() != Blocks.NOTE_BLOCK) continue;

                    // Copied from ServerPlayNetworkHandler#onPlayerInteractBlock
                    Vec3d vec3d2 = Vec3d.ofCenter(pos);
                    double sqDist = mc.player.getEyePos().squaredDistanceTo(vec3d2);
                    if (sqDist > ServerPlayNetworkHandler.MAX_BREAK_SQUARED_DISTANCE) continue;

                    if (!isValidScanSpot(pos)) continue;

                    Note note = NotebotUtils.getNoteFromNoteBlock(blockState, mode.get());
                    scannedNoteblocks.put(note, pos);
                }
            }

        }
    }

    private void onTickPreview() {
        for (Note note : song.getNotesMap().get(currentTick)) {
            if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
                mc.player.playSound(note.getInstrument().getSound(), 2f, (float) Math.pow(2.0D, (note.getNoteLevel() - 12) / 12.0D));
            } else {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 2f, (float) Math.pow(2.0D, (note.getNoteLevel() - 12) / 12.0D));
            }
        }
    }

    private void tune() {
        if (tuneHits.isEmpty()) {
            if (anyNoteblockTuned) {
                anyNoteblockTuned = false;
                waitTicks = checkNoteblocksAgainDelay.get();
                stage = Stage.WaitingToCheckNoteblocks;

                info("Delaying check for noteblocks");
            } else {
                stage = Stage.Playing;
                info("Loading done.");
                play();
            }
            return;
        }

        if (ticks < tickDelay.get()) {
            return;
        }

        tuneBlocks();
        ticks = 0;
    }

    private void tuneBlocks() {
        if (mc.world == null || mc.player == null) {
            disable();
        }

        if (swingArm.get()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        int iterations = 0;
        var iterator = tuneHits.entrySet().iterator();

        // Concurrent tuning :o
        while (iterator.hasNext()){
            var entry = iterator.next();
            BlockPos pos = entry.getKey();
            int hitsNumber = entry.getValue();

            if (autoRotate.get()) {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> tuneNoteblockWithPackets(pos));
            } else {
                this.tuneNoteblockWithPackets(pos);
            }

            clickedBlocks.add(pos);

            hitsNumber--;
            entry.setValue(hitsNumber);

            if (hitsNumber == 0) {
                iterator.remove();
            }

            iterations++;

            if (iterations == concurrentTuneBlocks.get()) return;
        }
    }

    private void tuneNoteblockWithPackets(BlockPos pos) {
        // We don't need to raycast here. Server handles this packet fine
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, false), 0));

        anyNoteblockTuned = true;
    }

    public void updateStatus() {
        if (status != null) status.set(getStatus());
    }

    private static int calcNumberOfHits(int from, int to) {
        if (from > to) {
            return (25 - from) + to;
        } else {
            return to - from;
        }
    }

    private void onTickPlay() {
        Collection<Note> notes = song.getNotesMap().get(this.currentTick);
        if (!notes.isEmpty()) {
            if (autoRotate.get()) {
                Optional<Note> firstNote = notes.stream().findFirst();
                if (firstNote.isPresent()) {
                    BlockPos firstPos = noteBlockPositions.get(firstNote.get());

                    if (firstPos != null) {
                        Rotations.rotate(Rotations.getYaw(firstPos), Rotations.getPitch(firstPos));
                    }
                }
            }

            if (swingArm.get()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }

            for (Note note : notes) {
                BlockPos pos = noteBlockPositions.get(note);
                if (pos == null) {
                    return;
                }

                if (polyphonic.get()) {
                    playRotate(pos);
                } else {
                    this.playRotate(pos);
                }
            }
        }
    }

    private void playRotate(BlockPos pos) {
        if (mc.interactionManager == null) return;
        try {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN, 0));
        } catch (NullPointerException ignored) {
        }
    }

    private boolean isValidScanSpot(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) return false;
        return mc.world.getBlockState(pos.up()).isAir();
    }

    @Nullable
    public Instrument getMappedInstrument(Instrument inst) {
        if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
            return ((NotebotUtils.OptionalInstrument) sgNoteMap.getByIndex(inst.ordinal()).get()).toMinecraftInstrument();
        } else {
            return inst;
        }
    }

    private String beautifyText(String text) {
        text = text.toLowerCase(Locale.ROOT);

        String[] arr = text.split("_");
        StringBuilder sb = new StringBuilder();

        for (String s : arr) {
            sb.append(Character.toUpperCase(s.charAt(0)))
                .append(s.substring(1));
        }
        return sb.toString().trim();
    }

    private enum Stage {
        None,
        LoadingSong,
        SetUp,
        Tune,
        WaitingToCheckNoteblocks,
        Playing,
        Preview
    }
}
