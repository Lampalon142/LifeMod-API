package fr.lampalon.lifemod.common.anticheat.utils;

/**
 * Custom BoundingBox for precise collision analysis without Bukkit API.
 */
public final class BoundingBox {
    public double minX, minY, minZ;
    public double maxX, maxY, maxZ;

    public BoundingBox(double x, double y, double z) {
        // Default Minecraft Player BoundingBox: 0.6 x 1.8 x 0.6
        this.minX = x - 0.3;
        this.minY = y;
        this.minZ = z - 0.3;
        this.maxX = x + 0.3;
        this.maxY = y + 1.8;
        this.maxZ = z + 0.3;
    }

    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public BoundingBox expand(double x, double y, double z) {
        return new BoundingBox(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
    }

    public boolean intersects(BoundingBox other) {
        return (this.minX < other.maxX && this.maxX > other.minX) &&
               (this.minY < other.maxY && this.maxY > other.minY) &&
               (this.minZ < other.maxZ && this.maxZ > other.minZ);
    }
    
    public BoundingBox clone() {
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
