package fr.redfaction.hooks;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

/**
 * Optional WorldGuard integration: prevents claiming inside protected regions
 * (e.g. a spawn protection region defined at the WorldGuard level).
 */
public class WorldGuardHook {

    /**
     * Returns true if the location falls inside an actual named WorldGuard region
     * (the global "__global__" region is ignored, since it always applies and would
     * otherwise block every claim on the map). Wilderness — even with PvP disabled —
     * is never considered protected.
     */
    public boolean isProtected(Location location) {
        try {
            RegionManager rm = WGBukkit.getRegionManager(location.getWorld());
            if (rm == null) return false;
            ApplicableRegionSet set = rm.getApplicableRegions(location);
            for (ProtectedRegion region : set.getRegions()) {
                if (!region.getId().equals(ProtectedRegion.GLOBAL_REGION)) {
                    return true; // inside a real protected region
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
