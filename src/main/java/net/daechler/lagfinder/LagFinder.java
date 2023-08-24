package net.daechler.lagfinder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class LagFinder extends JavaPlugin {

    private final int ENTITY_THRESHOLD = 50;
    private final int REDSTONE_THRESHOLD = 20;
    private final int ITEM_DROP_THRESHOLD = 100;
    private final int MOB_THRESHOLD = 30;
    private final int VEHICLE_THRESHOLD = 10;
    private final int SPECIAL_ENTITY_THRESHOLD = 50;
    private final int TILE_ENTITY_THRESHOLD = 40;
    private final int TNT_THRESHOLD = 10;
    private final int PORTAL_THRESHOLD = 5;
    private final int RADIUS = 10;

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + getName() + " has been enabled!");

        // Schedule a task to periodically check for lag sources asynchronously
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::checkForLagSources, 0L, 20L * 60); // Check every minute
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + getName() + " has been disabled!");
    }

    private void checkForLagSources() {
        // Get a copy of the online players to avoid concurrent modification issues
        Player[] onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[0]);

        for (Player player : onlinePlayers) {
            if (player.hasPermission("lagfinder.admin")) {
                Location loc = findLagSource(player.getLocation());
                if (loc != null) {
                    // Switch back to the main thread to send messages to the player
                    Bukkit.getScheduler().runTask(this, () -> {
                        player.sendMessage(ChatColor.YELLOW + "Potential lag source detected at: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                        player.sendMessage(ChatColor.YELLOW + "Type /tp " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " to teleport to the location.");
                    });
                }
            }
        }
    }

    private Location findLagSource(Location location) {
        List<Entity> nearbyEntities = location.getWorld().getEntities();
        int entityCount = 0;
        int redstoneCount = 0;
        int itemDropCount = 0;
        int mobCount = 0;
        int vehicleCount = 0;
        int specialEntityCount = 0;
        int tileEntityCount = 0;
        int tntCount = 0;
        int portalCount = 0;

        for (Entity entity : nearbyEntities) {
            if (entity.getLocation().distance(location) <= RADIUS) {
                entityCount++;
                if (entity instanceof Item) {
                    itemDropCount++;
                } else if (entity instanceof Monster) {
                    mobCount++;
                } else if (entity instanceof Boat || entity instanceof Minecart) {
                    vehicleCount++;
                } else if (entity instanceof Snowball || entity instanceof ExperienceOrb || entity instanceof Arrow || entity instanceof Trident || entity instanceof Fireball || entity instanceof Egg) {
                    specialEntityCount++;
                } else if (entity instanceof TNTPrimed) {
                    tntCount++;
                }
            }
        }

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    Location checkLoc = location.clone().add(x, y, z);
                    Material blockType = checkLoc.getBlock().getType();
                    BlockState state = checkLoc.getBlock().getState();
                    if (state instanceof org.bukkit.block.TileState) {
                        tileEntityCount++;
                    }
                    switch (blockType) {
                        case REDSTONE_WIRE:
                        case REDSTONE_TORCH:
                        case REDSTONE_BLOCK:
                        case REDSTONE_LAMP:
                        case REPEATER:
                        case COMPARATOR:
                        case PISTON:
                        case STICKY_PISTON:
                        case OBSERVER:
                        case DROPPER:
                        case DISPENSER:
                            redstoneCount++;
                            break;
                        case NETHER_PORTAL:
                            portalCount++;
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        if (entityCount >= ENTITY_THRESHOLD || redstoneCount >= REDSTONE_THRESHOLD || itemDropCount >= ITEM_DROP_THRESHOLD || mobCount >= MOB_THRESHOLD || vehicleCount >= VEHICLE_THRESHOLD || specialEntityCount >= SPECIAL_ENTITY_THRESHOLD || tileEntityCount >= TILE_ENTITY_THRESHOLD || tntCount >= TNT_THRESHOLD || portalCount >= PORTAL_THRESHOLD) {
            return location;
        }

        return null;
    }
}
