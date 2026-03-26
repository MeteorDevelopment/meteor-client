#!/usr/bin/env python3

import argparse
import datetime as dt
import json
import queue
import shutil
import subprocess
import sys
import threading
import time
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parent.parent
MC_DIR = Path.home() / "Library" / "Application Support" / "minecraft"
BUILD_JAR = REPO_ROOT / "build" / "libs" / "meteor-client-26.1-local.jar"
INSTALLED_JAR = MC_DIR / "mods" / "meteor-client-26.1-local.jar"
LAUNCHER_LOG = MC_DIR / "launcher_log.txt"
ACCOUNTS_JSON = MC_DIR / "launcher_accounts.json"
CLIENT_ID_FILE = MC_DIR / "clientId_v2.txt"
VANILLA_VERSION_JSON = MC_DIR / "versions" / "26.1" / "26.1.json"
FABRIC_VERSION_JSON = MC_DIR / "versions" / "fabric-loader-0.18.4-26.1" / "fabric-loader-0.18.4-26.1.json"
LATEST_LOG = MC_DIR / "logs" / "latest.log"
CRASH_REPORTS_DIR = MC_DIR / "crash-reports"
SOAK_LOG_DIR = REPO_ROOT / "soak-logs" / "26_1"
FATAL_LOG_PATTERNS = (
    "Caught error loading resourcepacks",
    "MixinApplyError",
    "InvalidInjectionException",
    "Critical injection failure",
    "BootstrapMethodError",
    "Exception in thread \"main\"",
    "Exception in thread \"Render thread\"",
)
FATAL_CONTAINS_ALL = (
    ("Mixin transformation of ", " failed"),
    ("Game exited with code",),
)
NON_FATAL_LOG_PATTERNS = (
    "Failed to fetch user properties",
    "Failed to fetch Realms feature flags",
    "Couldn't connect to realms",
    "Could not authorize you against Realms server",
    "baritone.command.defaults.ComeCommand",
    "The requested compatibility level JAVA_25 is higher",
    "Detected unexpected shutdown during last game startup",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build, install, and launch the local Minecraft 26.1 Fabric instance.")
    parser.add_argument("--no-build", action="store_true", help="Skip the Gradle build step.")
    parser.add_argument("--no-install", action="store_true", help="Skip copying the built jar into the live mods folder.")
    parser.add_argument("--startup-timeout", type=int, default=35, help="Seconds to wait for startup before considering the launch stable.")
    parser.add_argument("--leave-running", action="store_true", help="Leave the game process running if it survives startup.")
    parser.add_argument("--tail-lines", type=int, default=120, help="How many lines of latest.log to print after a failed run.")
    parser.add_argument("--soak", action="store_true", help="Run repeated startup smoke tests, increasing the timeout after each clean run.")
    parser.add_argument("--timeout-step", type=int, default=30, help="Seconds to add to the soak timeout after each clean run.")
    parser.add_argument("--max-timeout", type=int, default=0, help="Optional cap for soak timeout growth. 0 means no cap.")
    parser.add_argument("--sleep-between-runs", type=int, default=3, help="Seconds to wait between soak iterations.")
    parser.add_argument("--audit-first", action="store_true", help="Run the static mixin audit once before the soak loop starts.")
    parser.add_argument("--watch-live", action="store_true", help="Do not launch the game; watch latest.log and crash reports for the next fatal error while you reproduce it manually.")
    parser.add_argument("--watch-timeout", type=int, default=180, help="Seconds to wait in --watch-live mode before giving up.")
    return parser.parse_args()


def require_file(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(f"Required file not found: {path}")


def require_dir(path: Path) -> None:
    if not path.is_dir():
        raise FileNotFoundError(f"Required directory not found: {path}")


def read_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def build_repo() -> None:
    cmd = ["/bin/zsh", "-lc", "GRADLE_USER_HOME=$PWD/.gradle-home ./gradlew build"]
    print(f"[run_live_26_1] Building repo in {REPO_ROOT}", flush=True)
    subprocess.run(cmd, cwd=REPO_ROOT, check=True)


def install_jar() -> None:
    require_file(BUILD_JAR)
    INSTALLED_JAR.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(BUILD_JAR, INSTALLED_JAR)
    print(f"[run_live_26_1] Installed {BUILD_JAR.name} to {INSTALLED_JAR}", flush=True)


def replace_launcher_placeholders(value: str) -> str:
    return value.replace("<WORKDIR>/minecraft", str(MC_DIR)).replace("$HOME", str(Path.home()))


def extract_last_launch_config() -> tuple[str, list[str]]:
    require_file(LAUNCHER_LOG)
    lines = LAUNCHER_LOG.read_text(encoding="utf-8", errors="replace").splitlines()

    start_idx = -1
    java_path = None
    for idx, line in enumerate(lines):
        marker = "Starting game in folder $HOME/Library/Application Support/minecraft using java executable "
        if marker in line:
            start_idx = idx
            java_path = replace_launcher_placeholders(line.split(marker, 1)[1].strip())

    if start_idx == -1 or not java_path:
        raise RuntimeError("Could not find the last Java launch block in launcher_log.txt.")

    window_start = max(0, start_idx - 250)
    java_args = []
    for line in lines[window_start:start_idx]:
        marker = "Java argument:"
        if marker in line:
            java_args.append(replace_launcher_placeholders(line.split(marker, 1)[1]))

    if not java_args:
        raise RuntimeError("Could not extract JVM arguments from the last launch block.")

    log4j_arg = f"-Dlog4j.configurationFile={MC_DIR / 'assets' / 'log_configs' / 'client-1.21.2.xml'}"
    if not any(arg.startswith("-Dlog4j.configurationFile=") for arg in java_args):
        java_args.insert(0, log4j_arg)

    return java_path, java_args


def read_active_account() -> tuple[str, str]:
    require_file(ACCOUNTS_JSON)
    data = read_json(ACCOUNTS_JSON)
    active_id = data.get("activeAccountLocalId")
    accounts = data.get("accounts", {})
    if not active_id or active_id not in accounts:
        raise RuntimeError("Could not find the active launcher account.")

    account = accounts[active_id]
    profile = account.get("minecraftProfile", {})
    username = profile.get("name") or account.get("username") or "Player"
    uuid = profile.get("id") or "00000000000000000000000000000000"
    return username, uuid


def read_client_id() -> str:
    if CLIENT_ID_FILE.is_file():
        value = CLIENT_ID_FILE.read_text(encoding="utf-8", errors="replace").strip()
        if value:
            return value
    return "0"


def build_game_args() -> list[str]:
    require_file(VANILLA_VERSION_JSON)
    require_file(FABRIC_VERSION_JSON)

    vanilla = read_json(VANILLA_VERSION_JSON)
    username, uuid = read_active_account()
    client_id = read_client_id()

    return [
        "--username",
        username,
        "--version",
        "fabric-loader-0.18.4-26.1",
        "--gameDir",
        str(MC_DIR),
        "--assetsDir",
        str(MC_DIR / "assets"),
        "--assetIndex",
        str(vanilla["assetIndex"]["id"]),
        "--uuid",
        uuid,
        "--accessToken",
        "0",
        "--clientId",
        client_id,
        "--xuid",
        "0",
        "--versionType",
        vanilla.get("type", "release"),
    ]


def read_main_class() -> str:
    fabric = read_json(FABRIC_VERSION_JSON)
    main_class = fabric.get("mainClass")
    if not main_class:
        raise RuntimeError("Fabric version json did not contain a mainClass.")
    return main_class


def newest_crash_report(before_mtime: float | None) -> Path | None:
    if not CRASH_REPORTS_DIR.is_dir():
        return None

    newest = None
    newest_mtime = -1.0
    for path in CRASH_REPORTS_DIR.glob("crash-*-client.txt"):
        try:
            mtime = path.stat().st_mtime
        except FileNotFoundError:
            continue
        if before_mtime is not None and mtime <= before_mtime:
            continue
        if mtime > newest_mtime:
            newest = path
            newest_mtime = mtime
    return newest


def tail_text(path: Path, num_lines: int) -> str:
    try:
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    except FileNotFoundError:
        return ""
    return "\n".join(lines[-num_lines:])


def read_lines_since(path: Path, start_offset: int) -> tuple[list[str], int]:
    try:
        with path.open("r", encoding="utf-8", errors="replace") as f:
            f.seek(start_offset)
            data = f.read()
            offset = f.tell()
    except FileNotFoundError:
        return [], start_offset
    return data.splitlines(), offset


def is_fatal_line(line: str) -> bool:
    if any(pattern in line for pattern in NON_FATAL_LOG_PATTERNS):
        return False
    if any(pattern in line for pattern in FATAL_LOG_PATTERNS):
        return True
    return any(all(pattern in line for pattern in patterns) for patterns in FATAL_CONTAINS_ALL)


def find_fatal_in_text(lines: list[str]) -> str | None:
    for line in lines:
        if is_fatal_line(line):
            return line.strip()
    return None


def run_audit() -> list[str]:
    cmd = [sys.executable, str(REPO_ROOT / "scripts" / "audit_mixins.py")]
    try:
        proc = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True, timeout=45)
    except subprocess.TimeoutExpired:
        return ["audit-timeout"]
    lines = proc.stdout.splitlines()
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or "audit_mixins.py failed")
    return [line for line in lines if line.strip()]


def append_summary(summary_path: Path, message: str) -> None:
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    with summary_path.open("a", encoding="utf-8") as f:
        f.write(message + "\n")


class OutputPump(threading.Thread):
    def __init__(self, stream, log_path: Path):
        super().__init__(daemon=True)
        self.stream = stream
        self.log_path = log_path
        self.queue: queue.Queue[str | None] = queue.Queue()

    def run(self) -> None:
        with self.log_path.open("w", encoding="utf-8") as log_file:
            for line in self.stream:
                log_file.write(line)
                log_file.flush()
                self.queue.put(line)
        self.queue.put(None)


def launch_game_soak(startup_timeout: int, tail_lines: int, run_log: Path) -> tuple[bool, str | None, Path]:
    java_path, java_args = extract_last_launch_config()
    main_class = read_main_class()
    game_args = build_game_args()

    require_file(Path(java_path))

    cmd = [java_path, *java_args, main_class, *game_args]
    print(f"[run_live_26_1] Launching soak run with timeout {startup_timeout}s", flush=True)

    proc = subprocess.Popen(
        cmd,
        cwd=MC_DIR,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )

    if proc.stdout is None:
        raise RuntimeError("Failed to capture game output.")

    pump = OutputPump(proc.stdout, run_log)
    pump.start()

    start = time.monotonic()
    fatal_line = None

    while True:
        if proc.poll() is not None:
            break

        if time.monotonic() - start >= startup_timeout:
            proc.terminate()
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait(timeout=10)
            pump.join(timeout=5)
            return True, None, run_log

        try:
            line = pump.queue.get(timeout=0.25)
        except queue.Empty:
            continue

        if line is None:
            continue

        if is_fatal_line(line):
            fatal_line = line.strip()
            proc.terminate()
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait(timeout=10)
            break

    pump.join(timeout=5)

    if fatal_line:
        return False, fatal_line, run_log

    return False, f"process exited before surviving {startup_timeout}s", run_log


def soak_loop(args: argparse.Namespace) -> int:
    SOAK_LOG_DIR.mkdir(parents=True, exist_ok=True)
    summary_path = SOAK_LOG_DIR / "summary.log"

    if args.audit_first:
        findings = run_audit()
        audit_path = SOAK_LOG_DIR / "audit.txt"
        audit_path.write_text("\n".join(findings) + ("\n" if findings else ""), encoding="utf-8")
        append_summary(
            summary_path,
            f"{dt.datetime.now(dt.UTC).isoformat()} audit findings={len(findings)} path={audit_path}",
        )
        print(f"[run_live_26_1] Audit recorded {len(findings)} finding(s) in {audit_path}", flush=True)

    timeout = args.startup_timeout
    iteration = 1

    while True:
        timestamp = dt.datetime.now(dt.UTC).strftime("%Y%m%dT%H%M%SZ")
        run_log = SOAK_LOG_DIR / f"run-{iteration:04d}-{timestamp}-{timeout}s.log"
        print(f"[run_live_26_1] Soak iteration {iteration} starting with timeout {timeout}s", flush=True)

        ok, reason, log_path = launch_game_soak(timeout, args.tail_lines, run_log)
        now = dt.datetime.now(dt.UTC).isoformat()

        if ok:
            append_summary(summary_path, f"{now} ok iteration={iteration} timeout={timeout} log={log_path}")
            print(f"[run_live_26_1] Iteration {iteration} survived {timeout}s", flush=True)

            next_timeout = timeout + args.timeout_step
            if args.max_timeout > 0:
                next_timeout = min(next_timeout, args.max_timeout)

            if next_timeout != timeout:
                print(f"[run_live_26_1] Increasing timeout to {next_timeout}s", flush=True)
            timeout = next_timeout
            iteration += 1
            time.sleep(max(0, args.sleep_between_runs))
            continue

        append_summary(summary_path, f"{now} fail iteration={iteration} timeout={timeout} reason={reason} log={log_path}")
        print(f"[run_live_26_1] Iteration {iteration} failed: {reason}", flush=True)
        tail = tail_text(log_path, args.tail_lines)
        if tail:
            print("[run_live_26_1] ----- soak log tail -----", flush=True)
            print(tail, flush=True)
            print("[run_live_26_1] -------------------------", flush=True)
        print(f"[run_live_26_1] Full run log: {log_path}", flush=True)
        print(f"[run_live_26_1] Summary log: {summary_path}", flush=True)
        return 1


def launch_game(startup_timeout: int, leave_running: bool, tail_lines: int) -> int:
    java_path, java_args = extract_last_launch_config()
    main_class = read_main_class()
    game_args = build_game_args()

    require_file(Path(java_path))

    latest_log_mtime = LATEST_LOG.stat().st_mtime if LATEST_LOG.exists() else None
    newest_crash_mtime = None
    if CRASH_REPORTS_DIR.is_dir():
        newest = newest_crash_report(None)
        newest_crash_mtime = newest.stat().st_mtime if newest else None

    cmd = [java_path, *java_args, main_class, *game_args]
    print(f"[run_live_26_1] Launching {main_class}", flush=True)
    print(f"[run_live_26_1] Java: {java_path}", flush=True)

    proc = subprocess.Popen(cmd, cwd=MC_DIR)
    timed_out = False

    try:
        returncode = proc.wait(timeout=startup_timeout)
    except subprocess.TimeoutExpired:
        timed_out = True
        returncode = None

    if timed_out:
        if leave_running:
            print(f"[run_live_26_1] Startup survived {startup_timeout}s. Leaving PID {proc.pid} running.", flush=True)
            return 0

        print(f"[run_live_26_1] Startup survived {startup_timeout}s. Stopping PID {proc.pid}.", flush=True)
        proc.terminate()
        try:
            proc.wait(timeout=10)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait(timeout=10)
        return 0

    print(f"[run_live_26_1] Game exited with code {returncode}", flush=True)

    fresh_log = LATEST_LOG.exists() and (
        latest_log_mtime is None or LATEST_LOG.stat().st_mtime > latest_log_mtime
    )
    fresh_crash = newest_crash_report(newest_crash_mtime)

    if fresh_log:
        print(f"[run_live_26_1] Fresh latest.log: {LATEST_LOG}", flush=True)
        tail = tail_text(LATEST_LOG, tail_lines)
        if tail:
            print("[run_live_26_1] ----- latest.log tail -----", flush=True)
            print(tail, flush=True)
            print("[run_live_26_1] --------------------------", flush=True)

    if fresh_crash:
        print(f"[run_live_26_1] Fresh crash report: {fresh_crash}", flush=True)
        tail = tail_text(fresh_crash, min(80, tail_lines))
        if tail:
            print("[run_live_26_1] --- crash report tail ---", flush=True)
            print(tail, flush=True)
            print("[run_live_26_1] ------------------------", flush=True)

    return returncode if returncode is not None else 0


def watch_live(tail_lines: int, watch_timeout: int) -> int:
    print(f"[run_live_26_1] Watching {LATEST_LOG} for up to {watch_timeout}s", flush=True)
    print("[run_live_26_1] Reproduce the server/Realm join now in your normal launcher.", flush=True)

    log_offset = 0
    if LATEST_LOG.exists():
        log_offset = LATEST_LOG.stat().st_size

    newest = newest_crash_report(None)
    newest_crash_mtime = newest.stat().st_mtime if newest else None

    start = time.monotonic()
    while time.monotonic() - start < watch_timeout:
        lines, log_offset = read_lines_since(LATEST_LOG, log_offset)
        fatal = find_fatal_in_text(lines)
        if fatal:
            print(f"[run_live_26_1] Detected fatal log line: {fatal}", flush=True)
            tail = tail_text(LATEST_LOG, tail_lines)
            if tail:
                print("[run_live_26_1] ----- latest.log tail -----", flush=True)
                print(tail, flush=True)
                print("[run_live_26_1] --------------------------", flush=True)
            return 1

        fresh_crash = newest_crash_report(newest_crash_mtime)
        if fresh_crash:
            print(f"[run_live_26_1] Detected fresh crash report: {fresh_crash}", flush=True)
            tail = tail_text(fresh_crash, min(80, tail_lines))
            if tail:
                print("[run_live_26_1] --- crash report tail ---", flush=True)
                print(tail, flush=True)
                print("[run_live_26_1] ------------------------", flush=True)
            return 1

        time.sleep(0.5)

    print("[run_live_26_1] Watch timed out without seeing a fatal error.", flush=True)
    return 0


def main() -> int:
    args = parse_args()

    require_dir(MC_DIR)
    require_file(VANILLA_VERSION_JSON)
    require_file(FABRIC_VERSION_JSON)

    if not args.watch_live and not args.no_build:
        build_repo()

    if not args.watch_live and not args.no_install:
        install_jar()

    if args.soak:
        return soak_loop(args)

    if args.watch_live:
        return watch_live(args.tail_lines, args.watch_timeout)

    return launch_game(args.startup_timeout, args.leave_running, args.tail_lines)


if __name__ == "__main__":
    raise SystemExit(main())
