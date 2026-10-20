/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.compat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version-agnostic representation of a Minecraft version.
 *
 * <p>The rest of the codebase should never compare raw version strings. Instead it uses
 * {@link #isAtLeast(McVersion)}, {@link #isAtMost(McVersion)} or
 * {@link #isBetween(McVersion, McVersion)} so that every version-dependent decision lives
 * behind the {@link ICompat} adapter.</p>
 */
public final class McVersion implements Comparable<McVersion> {
    private static final Pattern RELEASE = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");
    private static final Pattern SNAPSHOT = Pattern.compile("(\\d+)w(\\d+)");

    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;

    public McVersion(int major, int minor, int patch, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = raw;
    }

    /**
     * Parses a version string such as {@code 26.2}, {@code 26.2.1} or the snapshot form
     * {@code 26w30a}. Unknown strings fall back to {@code 0.0.0} so that callers never
     * have to deal with a {@code null} version.
     */
    public static McVersion parse(String raw) {
        String string = raw == null ? "" : raw.trim();

        Matcher snapshot = SNAPSHOT.matcher(string);
        if (snapshot.find()) {
            return new McVersion(Integer.parseInt(snapshot.group(1)), Integer.parseInt(snapshot.group(2)), 0, string);
        }

        Matcher release = RELEASE.matcher(string);
        if (release.find()) {
            int major = Integer.parseInt(release.group(1));
            int minor = release.group(2) != null ? Integer.parseInt(release.group(2)) : 0;
            int patch = release.group(3) != null ? Integer.parseInt(release.group(3)) : 0;

            return new McVersion(major, minor, patch, string);
        }

        return new McVersion(0, 0, 0, string);
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public String raw() {
        return raw;
    }

    public boolean isAtLeast(McVersion other) {
        return compareTo(other) >= 0;
    }

    public boolean isAtMost(McVersion other) {
        return compareTo(other) <= 0;
    }

    public boolean isBetween(McVersion min, McVersion max) {
        return isAtLeast(min) && isAtMost(max);
    }

    @Override
    public int compareTo(McVersion other) {
        int result = Integer.compare(major, other.major);
        if (result != 0) return result;

        result = Integer.compare(minor, other.minor);
        if (result != 0) return result;

        return Integer.compare(patch, other.patch);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof McVersion other)) return false;

        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        return result;
    }

    @Override
    public String toString() {
        return raw;
    }
}
