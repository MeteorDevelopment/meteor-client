package me.jellysquid.mods.lithium.common.util.math;

import net.minecraft.util.math.MathHelper;

/**
 * A replacement for the sine angle lookup table used in {@link MathHelper}, both reducing the size of LUT and improving
 * the access patterns for common paired sin/cos operations.
 *
 *  sin(-x) = -sin(x)
 *    ... to eliminate negative angles from the LUT.
 *
 *  sin(x) = sin(pi/2 - x)
 *    ... to eliminate supplementary angles from the LUT.
 *
 * Using these identities allows us to reduce the LUT from 64K entries (256 KB) to just 16K entries (64 KB), enabling
 * it to better fit into the CPU's caches at the expense of some cycles on the fast path. The implementation has been
 * tightly optimized to avoid branching where possible and to use very quick integer operations.
 *
 * Generally speaking, reducing the size of a lookup table is always a good optimization, but since we need to spend
 * extra CPU cycles trying to maintain parity with vanilla, there is the potential risk that this implementation ends
 * up being slower than vanilla when the lookup table is able to be kept in cache memory.
 *
 * Unlike other "fast math" implementations, the values returned by this class are *bit-for-bit identical* with those
 * from {@link MathHelper}. Validation is performed during runtime to ensure that the table is correct.
 *
 * @author coderbot16   Author of the original (and very clever) implementation in Rust:
 *  https://gitlab.com/coderbot16/i73/-/tree/master/i73-trig/src
 * @author jellysquid3  Additional optimizations, port to Java
 */
public class CompactSineLUT {
    private static final int[] SINE_TABLE_INT = new int[16384 + 1];
    private static final float SINE_TABLE_MIDPOINT;

    static {
        // Copy the sine table, covering to raw int bits
        for (int i = 0; i < SINE_TABLE_INT.length; i++) {
            SINE_TABLE_INT[i] = Float.floatToRawIntBits(MathHelper.SINE_TABLE[i]);
        }

        SINE_TABLE_MIDPOINT = MathHelper.SINE_TABLE[MathHelper.SINE_TABLE.length / 2];

        // Test that the lookup table is correct during runtime
        for (int i = 0; i < MathHelper.SINE_TABLE.length; i++) {
            float expected = MathHelper.SINE_TABLE[i];
            float value = lookup(i);

            if (expected != value) {
                throw new IllegalArgumentException(String.format("LUT error at index %d (expected: %s, found: %s)", i, expected, value));
            }
        }
    }

    // [VanillaCopy] MathHelper#sin(float)
    public static float sin(float f) {
        return lookup((int) (f * 10430.378f) & 0xFFFF);
    }

    // [VanillaCopy] MathHelper#cos(float)
    public static float cos(float f) {
        return lookup((int) (f * 10430.378f + 16384.0f) & 0xFFFF);
    }

    private static float lookup(int index) {
        // A special case... Is there some way to eliminate this?
        if (index == 32768) {
            return SINE_TABLE_MIDPOINT;
        }

        // Trigonometric identity: sin(-x) = -sin(x)
        // Given a domain of 0 <= x <= 2*pi, just negate the value if x > pi.
        // This allows the sin table size to be halved.
        int neg = (index & 0x8000) << 16;

        // All bits set if (pi/2 <= x), none set otherwise
        // Extracts the 15th bit from 'half'
        int mask = (index << 17) >> 31;

        // Trigonometric identity: sin(x) = sin(pi/2 - x)
        int pos = (0x8001 & mask) + (index ^ mask);

        // Wrap the position in the table. Moving this down to immediately before the array access
        // seems to help the Hotspot compiler optimize the bit math better.
        pos &= 0x7fff;

        // Fetch the corresponding value from the LUT and invert the sign bit as needed
        // This directly manipulate the sign bit on the float bits to simplify logic
        return Float.intBitsToFloat(SINE_TABLE_INT[pos] ^ neg);
    }
}
