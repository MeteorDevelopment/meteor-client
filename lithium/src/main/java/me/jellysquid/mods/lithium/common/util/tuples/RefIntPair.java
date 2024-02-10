package me.jellysquid.mods.lithium.common.util.tuples;

import java.util.Objects;

public record RefIntPair<A>(A left, int right) {

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (RefIntPair<?>) obj;
        return this.left == that.left &&
                this.right == that.right;
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(this.left), right);
    }

    @Override
    public String toString() {
        return "RefIntPair[" +
                "left=" + left + ", " +
                "right=" + right + ']';
    }

}
