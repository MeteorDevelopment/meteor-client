package meteordevelopment.meteorclient.utils.misc;

import com.mojang.blaze3d.platform.NativeImage;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Reports whatever is playing on the machine (Spotify, a browser tab, VLC, ...) by
 * reading the Windows system media session, and can play/pause or skip it.
 *
 * The session API is WinRT, which PowerShell cannot use here: the thumbnail stream
 * comes back as an unprojected __ComObject with no usable members. So a small C#
 * helper is compiled once with the csc.exe that ships with .NET Framework and then
 * run as a long-lived process that streams records out and takes commands in.
 */
public class MediaInfo {
    public static final Identifier ART_ID = MeteorClient.identifier("media_art");

    private static final Pattern SEP = Pattern.compile("");

    /** Bump when SOURCE changes so a stale compiled helper is replaced. */
    private static final String VERSION = "v2";

    private static volatile String title = "";
    private static volatile String artist = "";
    private static volatile boolean playing = false;
    private static volatile boolean available = false;
    private static volatile int duration = 0;

    private static volatile int polledPosition = 0;
    private static volatile long polledAt = 0;

    private static volatile int artFlag = 0;
    private static int loadedArtFlag = -1;

    private static volatile NativeImage pendingArt = null;
    private static volatile boolean artReady = false;
    private static DynamicTexture artTexture = null;

    private static Thread worker = null;
    private static volatile Process process = null;
    private static volatile BufferedWriter commands = null;
    private static Path artPath = null;
    private static boolean unsupported = false;

    public static String getTitle() { return title; }
    public static String getArtist() { return artist; }
    public static boolean isPlaying() { return playing; }
    public static boolean isAvailable() { return available; }
    public static int getDuration() { return duration; }
    public static boolean hasArt() { return artReady; }

    public static int getPosition() {
        if (!available) return 0;
        int pos = polledPosition;
        if (playing) pos += (int) ((System.currentTimeMillis() - polledAt) / 1000);
        if (duration > 0) pos = Math.min(pos, duration);
        return Math.max(0, pos);
    }

    public static void start() {
        if (worker != null || unsupported) return;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            unsupported = true;
            return;
        }

        worker = new Thread(MediaInfo::run, "Ember-MediaInfo");
        worker.setDaemon(true);
        worker.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Process p = process;
            if (p != null) p.destroyForcibly();
        }));
    }

    /** Sends "toggle", "next" or "prev" to whichever app owns the media session. */
    public static void sendCommand(String command) {
        BufferedWriter writer = commands;
        if (writer == null) return;

        try {
            synchronized (writer) {
                writer.write(command);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException ignored) {
        }
    }

    /** Must be called from the render thread. Uploads newly fetched art. */
    public static void uploadPendingArt() {
        NativeImage img = pendingArt;
        if (img == null) return;
        pendingArt = null;

        DynamicTexture old = artTexture;
        artTexture = new DynamicTexture(() -> "ember-media-art", img);
        mc.getTextureManager().register(ART_ID, artTexture);
        if (old != null) old.close();

        artReady = true;
    }

    private static void run() {
        Path exe;
        try {
            exe = ensureExe();
        } catch (Throwable e) {
            MeteorClient.LOG.warn("Media widget unavailable: could not build helper ({})", e.toString());
            unsupported = true;
            return;
        }

        while (true) {
            try {
                pump(exe);
            } catch (Throwable ignored) {
            }

            available = false;
            playing = false;

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private static void pump(Path exe) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(exe.toString(), artPath.toString());
        pb.redirectErrorStream(false);

        Process proc = pb.start();
        process = proc;
        commands = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8));

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                handleLine(line);
            }
        } finally {
            commands = null;
            proc.destroyForcibly();
            process = null;
        }
    }

    private static void handleLine(String line) {
        String[] parts = SEP.split(line, -1);
        if (parts.length < 6) return;

        String t = parts[0].trim();

        title = t;
        artist = parts[1].trim();
        polledPosition = parseInt(parts[2]);
        duration = parseInt(parts[3]);
        polledAt = System.currentTimeMillis();
        playing = parts[4].equalsIgnoreCase("Playing");
        available = !t.isEmpty();

        int flag = parseInt(parts[5]);
        if (flag != artFlag) {
            artFlag = flag;
            loadArt();
        }
    }

    private static void loadArt() {
        if (artFlag == loadedArtFlag) return;
        try {
            byte[] data = Files.readAllBytes(artPath);
            if (data.length < 16) return;

            pendingArt = NativeImage.read(data);
            loadedArtFlag = artFlag;
        } catch (Throwable ignored) {
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatTime(int seconds) {
        if (seconds < 0) seconds = 0;
        int m = seconds / 60;
        int s = seconds % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    // --- Helper build ---

    private static Path ensureExe() throws Exception {
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "ember-media");
        Files.createDirectories(dir);

        artPath = dir.resolve("art.img");
        Path exe = dir.resolve("EmberMedia-" + VERSION + ".exe");
        if (Files.exists(exe)) return exe;

        Path src = dir.resolve("EmberMedia-" + VERSION + ".cs");
        Files.writeString(src, SOURCE, StandardCharsets.UTF_8);

        Path csc = findCsc();
        Path winmd = Paths.get(System.getenv("WINDIR"), "System32", "WinMetadata");
        Path sysRuntime = findSystemRuntime();

        List<String> cmd = new ArrayList<>();
        cmd.add(csc.toString());
        cmd.add("/nologo");
        cmd.add("/target:exe");
        cmd.add("/platform:anycpu");
        cmd.add("/out:" + exe);
        cmd.add("/reference:" + sysRuntime);
        cmd.add("/reference:" + winmd.resolve("Windows.Media.winmd"));
        cmd.add("/reference:" + winmd.resolve("Windows.Storage.winmd"));
        cmd.add("/reference:" + winmd.resolve("Windows.Foundation.winmd"));
        cmd.add(src.toString());

        Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = proc.waitFor();

        if (code != 0 || !Files.exists(exe)) {
            throw new IllegalStateException("csc failed (" + code + "): " + output.trim());
        }

        return exe;
    }

    private static Path findCsc() {
        Path root = Paths.get(System.getenv("WINDIR"), "Microsoft.NET");
        for (String arch : new String[]{"Framework64", "Framework"}) {
            Path p = root.resolve(arch).resolve("v4.0.30319").resolve("csc.exe");
            if (Files.exists(p)) return p;
        }
        throw new IllegalStateException("csc.exe not found");
    }

    private static Path findSystemRuntime() throws Exception {
        Path gac = Paths.get(System.getenv("WINDIR"), "Microsoft.NET", "assembly", "GAC_MSIL", "System.Runtime");
        if (Files.isDirectory(gac)) {
            try (Stream<Path> versions = Files.list(gac)) {
                for (Path v : versions.toList()) {
                    Path dll = v.resolve("System.Runtime.dll");
                    if (Files.exists(dll)) return dll;
                }
            }
        }
        throw new IllegalStateException("System.Runtime.dll not found in GAC");
    }

    private static final String SOURCE = """
        using System;
        using System.IO;
        using System.Text;
        using System.Threading;
        using Windows.Foundation;
        using Windows.Media.Control;
        using Windows.Storage.Streams;

        public static class EmberMedia {
            const char SEP = (char)1;

            static string lastArtKey = "";
            static int artFlag = 0;

            // Waits without System.Runtime.WindowsRuntime, which is unusable here: it is
            // built against the unified Windows.winmd from the SDK, and only the split
            // winmds in System32\\WinMetadata exist on a stock machine.
            static T Wait<T>(IAsyncOperation<T> op) {
                var done = new ManualResetEventSlim(false);
                op.Completed = (o, s) => done.Set();
                done.Wait(10000);
                return op.GetResults();
            }

            public static int Main(string[] args) {
                Console.OutputEncoding = Encoding.UTF8;
                string artPath = args.Length > 0 ? args[0] : null;

                GlobalSystemMediaTransportControlsSessionManager mgr;
                try {
                    mgr = Wait(GlobalSystemMediaTransportControlsSessionManager.RequestAsync());
                } catch (Exception) {
                    return 1;
                }

                // Commands arrive one per line on stdin: toggle, next, prev.
                var input = new Thread(() => {
                    string line;
                    while ((line = Console.In.ReadLine()) != null) {
                        try {
                            var session = mgr.GetCurrentSession();
                            if (session == null) continue;

                            switch (line.Trim()) {
                                case "toggle": Wait(session.TryTogglePlayPauseAsync()); break;
                                case "next": Wait(session.TrySkipNextAsync()); break;
                                case "prev": Wait(session.TrySkipPreviousAsync()); break;
                            }
                        } catch (Exception) {
                        }
                    }
                });
                input.IsBackground = true;
                input.Start();

                while (true) {
                    try {
                        Emit(mgr, artPath);
                    } catch (Exception) {
                    }
                    Thread.Sleep(500);
                }
            }

            static void Emit(GlobalSystemMediaTransportControlsSessionManager mgr, string artPath) {
                var s = mgr.GetCurrentSession();
                if (s == null) {
                    Write("", "", 0, 0, "None");
                    return;
                }

                var p = Wait(s.TryGetMediaPropertiesAsync());
                var tl = s.GetTimelineProperties();
                var pb = s.GetPlaybackInfo();

                string status = pb.PlaybackStatus.ToString();
                double pos = tl.Position.TotalSeconds;
                double dur = tl.EndTime.TotalSeconds;

                // Position is a snapshot taken at LastUpdatedTime, not a live value;
                // without this the bar sits still and only jumps when you pause.
                try {
                    var lu = tl.LastUpdatedTime;
                    if (status == "Playing" && lu.Year > 2000) {
                        double age = (DateTimeOffset.UtcNow - lu).TotalSeconds;
                        if (age > 0 && age < 3600) pos += age;
                    }
                } catch (Exception) { }

                if (dur > 0 && pos > dur) pos = dur;
                if (pos < 0) pos = 0;

                string title = p.Title ?? "";
                string artist = p.Artist ?? "";

                string key = title + "###" + artist;
                if (key != lastArtKey) {
                    lastArtKey = key;
                    if (TryWriteArt(p, artPath)) artFlag++;
                }

                Write(title, artist, (int)pos, (int)dur, status);
            }

            static bool TryWriteArt(GlobalSystemMediaTransportControlsSessionMediaProperties p, string artPath) {
                if (string.IsNullOrEmpty(artPath) || p.Thumbnail == null) return false;
                try {
                    var stream = Wait(p.Thumbnail.OpenReadAsync());
                    uint size = (uint)stream.Size;
                    if (size == 0) return false;

                    var reader = new DataReader(stream.GetInputStreamAt(0));
                    Wait(reader.LoadAsync(size));

                    var bytes = new byte[size];
                    reader.ReadBytes(bytes);

                    string tmp = artPath + ".tmp";
                    File.WriteAllBytes(tmp, bytes);
                    if (File.Exists(artPath)) File.Delete(artPath);
                    File.Move(tmp, artPath);
                    return true;
                } catch (Exception) {
                    return false;
                }
            }

            static void Write(string title, string artist, int pos, int dur, string status) {
                var sb = new StringBuilder();
                sb.Append(Clean(title)).Append(SEP);
                sb.Append(Clean(artist)).Append(SEP);
                sb.Append(pos).Append(SEP);
                sb.Append(dur).Append(SEP);
                sb.Append(status).Append(SEP);
                sb.Append(artFlag);

                Console.Out.WriteLine(sb.ToString());
                Console.Out.Flush();
            }

            static string Clean(string s) {
                if (s == null) return "";
                var sb = new StringBuilder(s.Length);
                foreach (char c in s) {
                    if (c < 0x20) continue;
                    if (c >= 0x200B && c <= 0x200F) continue;
                    if (c == 0xFEFF) continue;
                    sb.Append(c);
                }
                return sb.ToString().Trim();
            }
        }
        """;
}
