package co.neweden.DeathCrates;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by ofcith at 22-07-2017 08:55 :D
 */
public class Main extends JavaPlugin implements Listener
{
    HashMap<Shulker, Crate> serverCrates = new HashMap<>();
    HashMap<Player, Crate> lastSpawned = new HashMap<>();

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        this.getLogger().log(Level.INFO, "Enabled DeathCrates!");
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run()
            {
                if (serverCrates.isEmpty()) return;
                for (Crate crate : serverCrates.values()) {
                    if (crate.getData().getExpiryTime() < System.currentTimeMillis() && crate.getData().getExpiryTime() != 0) {
                        deleteCrate(crate);
                        return;
                    }
                }
            }
        }, this.getConfig().getInt("deathcrates.crate-clear-delay-ms", 10000) * 20 / 1000, 200);
    }
    public void onDisable() {
        this.getLogger().log(Level.INFO, "Clearing all server crates..");
        int x = 0;
        for (Crate crate : serverCrates.values()) {
            deleteCrate(crate);
            x++;
        }
        this.getLogger().log(Level.INFO,  "[" + x + "] Crates cleared and plugin disabled!");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent deathEvent) {
        if (deathEvent.getDrops().size() < 1 || !deathEvent.getEntity().hasPermission("deathcrates.spawnable")) return;
        Player deadPlayer = deathEvent.getEntity();
        EntityDamageEvent.DamageCause cause = deadPlayer.getLastDamageCause().getCause();

        if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.VOID)
            return;

        List<ItemStack> drops = new ArrayList<>(deathEvent.getDrops());
        deathEvent.getDrops().clear();

        Location cLocation = null;
        if (deathEvent.getEntity().getLocation().getBlock().getType().equals(Material.AIR) ||
                deathEvent.getEntity().getLocation().getBlock().getType().equals(Material.WATER)) {
            cLocation = deathEvent.getEntity().getLocation();
        } else {
            for (Block block : Utils.getNearbyBlocks(deathEvent.getEntity().getLocation(), 5)) {
                if (block.getType().equals(Material.AIR) || block.getType().equals(Material.WATER)) {
                    cLocation = block.getLocation();
                    break;
                }
            }
        }

        if (cLocation != null)
            spawnCrate(cLocation, drops, deathEvent.getDroppedExp(), deathEvent.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent respawnEvent) {
        Crate crate = onRespawn(respawnEvent.getPlayer());
        if (crate == null) return;
        crate.getData().setExpiryTime(
                System.currentTimeMillis() + (getConfig().getLong("deathcrates.despawn-time-min") * 60 * 1000)
        );
        if (!crate.getData().getHasSentRespawnMessage()) {
           if (crate.getData().getHasSpawned())
           {
               crate.getOwner().sendMessage(this.getConfig().getString("deathcrates.respawn-message"));
               crate.getData().setHasSentRespawnMessage(true);
           }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEntityInteract(PlayerInteractEntityEvent entityInteractEvent) {
        Crate crate = getCrate(entityInteractEvent.getRightClicked());
        if (crate == null) return;

        if (!crate.getOwner().equals(entityInteractEvent.getPlayer()) &&
                !entityInteractEvent.getPlayer().hasPermission("deathcrates.override"))
        {
            entityInteractEvent.getPlayer().sendMessage(this.getConfig().getString("deathcrates.insufficient-permissions-message"));
            return;
        }

        entityInteractEvent.getPlayer().openInventory(crate.getData().getInventory());
        if (crate.getOwner().equals(entityInteractEvent.getPlayer())) { // owner opened the crate
            entityInteractEvent.getPlayer().giveExp(crate.getData().getNewExp());
            crate.getData().setNewExp(0);
        }
        entityInteractEvent.setCancelled(true); // to prevent the chest inventory from opening, only the crate inventory opens
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent inventoryCloseEvent) {
        if (inventoryCloseEvent.getInventory().getSize() != 5 * 9) return;
        Optional<Crate> opt = serverCrates.values().stream()
                .filter(e -> e.getData().getInventory().equals(inventoryCloseEvent.getInventory()))
                .findFirst();
        if (!opt.isPresent()) return;
        Crate crate = opt.get();

        int x = 0;
        for (ItemStack i : crate.getData().getInventory().getContents()) {
            if (i == null) continue;
            if (!i.getType().equals(Material.AIR)) x++;
        }

        if (x > 0) return;
        deleteCrate(crate);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitySpawn(EntitySpawnEvent spawnEvent) {
        if (serverCrates.isEmpty()) return;
        Optional<Crate> opt = serverCrates.values().stream()
                .filter(c -> c.getShulker().getLocation().equals(spawnEvent.getLocation())
                        && spawnEvent.getEntityType().equals(EntityType.SHULKER))
                .findFirst();
        if (!opt.isPresent()) return;
        Crate crate = opt.get();

        if (spawnEvent.isCancelled()) {
            crate.getData().setHasSpawned(false);
            for (ItemStack i: crate.getData().getInventory().getContents()) {
                if (i == null) continue;
                if (i.getType().equals(Material.AIR)) continue;
                crate.getShulker().getWorld().dropItemNaturally(crate.getShulker().getLocation(), i);
                crate.getData().getInventory().remove(i);
            }
            crate.getOwner().sendMessage(this.getConfig().getString("deathcrates.crate-unspawnable-message"));
            deleteCrate(crate);
        }
    }

    public void spawnCrate(Location location, List<ItemStack> drops, int newXP, Player player) {
        if (drops.size() < 1) return;
        String message = this.getConfig().getString("deathcrates.crate-name", player.getDisplayName() + "'s Death Crate");
        String crateName = message.replace("NAME", player.getDisplayName());
        Inventory inventory = Bukkit.createInventory(player, 5 * 9, crateName);
        for (ItemStack its: drops) {
            inventory.addItem(its);
        }
        PlayerCrateData pcd = new PlayerCrateData(inventory, newXP, 0L, player.getLocation());

        Shulker shulker = (Shulker) location.getWorld().spawnEntity(location, EntityType.SHULKER);
        shulker.setCustomName(crateName);
        shulker.setSilent(true);
        shulker.setInvulnerable(true);
        shulker.setCustomNameVisible(true);
        shulker.setAI(false);
        shulker.setCanPickupItems(false);
        shulker.setCollidable(false);
        shulker.setGlowing(true);

        Crate crate = new Crate(player, pcd, shulker);
        serverCrates.put(crate.getShulker(), crate);
        lastSpawned.put(player, crate);
    }

    public void deleteCrate(Crate crate) {
        serverCrates.remove(crate.getShulker());
        crate.getShulker().remove();
    }

    public Crate getCrate(Entity entity) {
        if (!(entity instanceof Shulker)) return null;
        return serverCrates.get(entity);
    }

    private Crate onRespawn(Player player) {
        Crate crate = lastSpawned.get(player);
        if (crate.getData().getExpiryTime() != 0) return null;
        lastSpawned.remove(player);
        return crate;
    }

}
