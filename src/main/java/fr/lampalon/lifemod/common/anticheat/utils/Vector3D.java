package fr.lampalon.lifemod.common.anticheat.utils;

/**
 * Thread-safe 3D Vector for AntiCheat calculations.
 */
public final class Vector3D {
    public double x, y, z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D clone() {
        return new Vector3D(x, y, z);
    }

    public double distanceSquared(Vector3D other) {
        return Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2);
    }

    public Vector3D add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3D multiply(double factor) {
        this.x *= factor;
        this.y *= factor;
        this.z *= factor;
        return this;
    }
}
