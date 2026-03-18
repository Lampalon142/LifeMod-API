package fr.lampalon.lifemod.common.replay;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Utility class for interpolating movements between replay frames.
 */
public class Interpolator {

    /**
     * Linearly interpolates between two locations based on a progress factor.
     * @param start The starting location.
     * @param end The target location.
     * @param factor Progress from 0.0 to 1.0.
     * @return The interpolated location.
     */
    public static Location interpolateLocation(Location start, Location end, double factor) {
        if (start == null || end == null) return start;
        
        Vector startVec = start.toVector();
        Vector endVec = end.toVector();
        
        Vector interpolatedVec = startVec.clone().add(endVec.subtract(startVec).multiply(factor));
        
        float yaw = (float) (start.getYaw() + (end.getYaw() - start.getYaw()) * factor);
        float pitch = (float) (start.getPitch() + (end.getPitch() - start.getPitch()) * factor);
        
        return new Location(start.getWorld(), interpolatedVec.getX(), interpolatedVec.getY(), interpolatedVec.getZ(), yaw, pitch);
    }
}
