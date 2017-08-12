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
    HashMap<Block, Crate> serverCrates = new HashMap<Block, Crate>();

    //TODO: add configs
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getLogger().log(Level.INFO, "Enabled DeathCrates!");
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
        {
            public void run()
            {
                if (serverCrates.isEmpty()) return;
                int cc = 0;
                /*
                Iterator crateIterator = serverCrates.entrySet().iterator();
                while (crateIterator.hasNext()) {
                    Map.Entry entry = (Map.Entry)crateIterator.next();
                    Crate crate = (Crate)entry.getValue();
                    if (crate.getData().getExpiryTime() < System.currentTimeMillis() && crate.getData().getExpiryTime() != 0)
                    {
                        if (serverCrates.containsKey((Player)entry.getKey()))
                        {
                            crate.getWorld().getBlockAt(crate.getCrateLocation()).setType(crate.getData().getPreviousMaterial());
                            serverCrates.remove(entry.getKey());
                            ++cc;
                        }
                    } else if (crate.getData().getInventory().getContents().length < 1 && serverCrates.containsKey((Player)entry.getKey()))
                    {
                        crate.getWorld().getBlockAt(crate.getCrateLocation()).setType(crate.getData().getPreviousMaterial());
                        serverCrates.remove((Player)entry.getKey());
                        ++cc;
                    } else continue;

                }*/

                for (Map.Entry<Block, Crate> entry: serverCrates.entrySet()) {
                    Crate crate = entry.getValue();
                    if (crate.getData().getExpiryTime() < System.currentTimeMillis() && crate.getData().getExpiryTime() != 0) {
                        deleteCrate(entry.getValue(), entry.getKey());
                        cc++;
                        return;
                    }
                }

                //Bukkit.getLogger().log(Level.INFO, "Cleared [" + cc + "] Crates at [" + System.currentTimeMillis() + "]");

            }
        }, this.getConfig().getInt("deathcrates.crate-clear-delay-ms", 10000) * 20 / 1000, 200);
    }
    public void onDisable() {
        this.getLogger().log(Level.INFO, "Clearing all server crates..");
        int x = 0;
        /*Iterator crateIterator = serverCrates.entrySet().iterator();
        while (crateIterator.hasNext()) {
            Map.Entry entry = (Map.Entry)crateIterator.next();
            Crate crate = (Crate)entry.getValue();
            crate.getWorld().getBlockAt(crate.getCrateLocation()).setType(crate.getData().getPreviousMaterial());
            serverCrates.remove(entry.getKey());
            x++;

        }*/

        for (Map.Entry<Block, Crate> entry: serverCrates.entrySet()) {
            /*Crate crate = entry.getValue();
            crate.getWorld().getBlockAt(crate.getCrateLocation()).setType(crate.getData().getPreviousMaterial());
            crate.getHologramEntity().remove();
            serverCrates.remove(entry.getKey());*/
            deleteCrate(entry.getValue(), entry.getKey());
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
        List<ItemStack> drops = new ArrayList<ItemStack>();
        //for (ItemStack stack: deathEvent.getDrops()) { drops.add(stack); }
        drops.addAll(deathEvent.getDrops());
        deathEvent.getDrops().clear();
        if (deathEvent.getEntity().getLocation().getBlock().getType().equals(Material.AIR) ||
                deathEvent.getEntity().getLocation().getBlock().getType().equals(Material.WATER)) {
            spawnCrate(deathEvent.getEntity().getLocation(), drops, deathEvent.getEntity().getWorld(), deathEvent.getDroppedExp(), deathEvent.getEntity(),
                    deathEvent.getEntity().getWorld().getBlockAt(deathEvent.getEntity().getLocation()).getType(), deadPlayer.getLocation());
        } else {
            for (Block block : Utils.getNearbyBlocks(deathEvent.getEntity().getLocation(), 5)) {
                if (block.getType().equals(Material.AIR) || block.getType().equals(Material.WATER)) {
                    spawnCrate(block.getLocation(), drops, block.getWorld(), deathEvent.getDroppedExp(), deathEvent.getEntity(), block.getType(),
                            deadPlayer.getLocation());
                    return;
                }
            }
        }

    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent respawnEvent) {
        Crate crate = onRespawn(respawnEvent.getPlayer());
        if (crate == null) {return;}
        crate.getData().setExpiryTime(
                System.currentTimeMillis() + ((long)(this.getConfig().getDouble("deathcrates.despawn-time-min") * 60 * 1000))
        );
        serverCrates.replace(crate.getWorld().getBlockAt(crate.getCrateLocation()   ), crate);
        if (!crate.getData().getHasSentRespawnMessage()) {
            crate.getOwner().sendMessage(this.getConfig().getString("deathcrates.respawn-message"));
            crate.getData().setHasSentRespawnMessage(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent interactEvent) {
        if (interactEvent.getClickedBlock() == null) return;
        if (!interactEvent.getClickedBlock().getType().equals(Material.CHEST)) return;
        Crate crate = getCrate(interactEvent.getClickedBlock());
        //crate = the crate at that block, crate2 = crate at that block while owner opened it
        //if these two don't match, then someone else opened the crate, if they do match, then it's the crate owner who opened them
        //TODO: instead of using Player objects, use UUIDs incase the player logs out and logs back in! (DONE)
        //if crate is null, then the clicked block is DEFINITELY not a crate (should return)
        //if crate2 is null, then either there is no crate there, or the player opening it isn't the owner of the crate
        //if both crate and crate2 are null, return (block isn't a crate)
        //if crate2 is null and there is a crate there (do this by doing getCrate(Block) if the condition is true) means that another player opened the crate

        /*if (serverCrates.containsKey(interactEvent.getPlayer())) crate = serverCrates.get(interactEvent.getPlayer());
        Iterator crateIterator = serverCrates.entrySet().iterator();
        while (crateIterator.hasNext()) {
            if (crate != null || !interactEvent.getPlayer().hasPermission("staff")) return;
            Map.Entry entry = (Map.Entry)crateIterator.next();
            Crate x = (Crate)entry.getValue();
            if (x.getWorld().getBlockAt(x.getCrateLocation()) != interactEvent.getClickedBlock()) return;
            crate = x;
        }*/
        //if (serverCrates.containsKey((Player)interactEvent.getPlayer())) crate = serverCrates.get(interactEvent.getPlayer());
        if (crate == null) return;
        if (crate.getOwner().getUniqueId() != interactEvent.getPlayer().getUniqueId() &&
                !interactEvent.getPlayer().hasPermission("deathcrates.override"))
        {
            interactEvent.getPlayer().sendMessage(this.getConfig().getString("deathcrates.insufficient-permissions-message"));
            return;
        }
        if (!interactEvent.getClickedBlock().equals(crate.getWorld().getBlockAt(crate.getCrateLocation()))) return;
        interactEvent.getPlayer().openInventory(crate.getData().getInventory());
        if (crate.getOwner().getUniqueId() == interactEvent.getPlayer().getUniqueId()) { // owner opened the crate
            interactEvent.getPlayer().giveExp(crate.getData().getNewExp());
            crate.getData().setNewExp(0);
        }
        serverCrates.replace(crate.getWorld().getBlockAt(crate.getCrateLocation()), crate);
        interactEvent.setCancelled(true); // to prevent the chest inventory from opening, only the crate inventory opens
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEntityInteract(PlayerInteractEntityEvent entityInteractEvent) {
        if (!entityInteractEvent.getRightClicked().getType().equals(EntityType.SHULKER)) return;
        Crate crate = getCrate(entityInteractEvent.getRightClicked().getWorld().getBlockAt(entityInteractEvent.getRightClicked().getLocation()));

        if (crate == null) return;
        if (crate.getOwner().getUniqueId() != entityInteractEvent.getPlayer().getUniqueId() &&
                !entityInteractEvent.getPlayer().hasPermission("deathcrates.override"))
        {
            entityInteractEvent.getPlayer().sendMessage(this.getConfig().getString("deathcrates.insufficient-permissions-message"));
            return;
        }
        if (!entityInteractEvent.getRightClicked().getWorld().getBlockAt(entityInteractEvent.getRightClicked().getLocation())
                .equals(crate.getWorld().getBlockAt(crate.getCrateLocation()))) return;
        entityInteractEvent.getPlayer().openInventory(crate.getData().getInventory());
        if (crate.getOwner().getUniqueId() == entityInteractEvent.getPlayer().getUniqueId()) { // owner opened the crate
            entityInteractEvent.getPlayer().giveExp(crate.getData().getNewExp());
            crate.getData().setNewExp(0);
        }
        serverCrates.replace(crate.getWorld().getBlockAt(crate.getCrateLocation()), crate);
        entityInteractEvent.setCancelled(true); // to prevent the chest inventory from opening, only the crate inventory opens

    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent inventoryCloseEvent) {
        /*Crate playerCrate = getCrate((Player)inventoryCloseEvent.getPlayer());
        if (playerCrate == null) return;
        int x = 0;
        for (Map.Entry<Block, Crate> entry : serverCrates.entrySet()) {
            Crate crate = entry.getValue();
            for (ItemStack i : crate.getData().getInventory().getContents()) {
                if (i == null) continue;
                if (!i.getType().equals(Material.AIR)) x++;
            }

            if (inventoryCloseEvent.getInventory().equals(crate.getData().getInventory()) && x == 0) {
                deleteCrate(entry.getValue(), entry.getKey());
                return;
            }
            x = 0;
        }*/


        /*Crate crate = getCrate(((Chest)inventoryCloseEvent.getInventory().getHolder()).getBlock());
        if (crate == null) return;
        int x = 0;
        for (ItemStack i : crate.getData().getInventory().getContents()) {
            if (i == null) continue;
            if (!i.getType().equals(Material.AIR)) x++;
        }

        if (x > 0) return;
        deleteCrate(crate, crate.getWorld().getBlockAt(crate.getCrateLocation()));
        */
        if (inventoryCloseEvent.getInventory().getSize() != 5 * 9) return;
        Optional<Map.Entry<Block, Crate>> opt = serverCrates.entrySet().stream()
                .filter(e -> e.getValue().getData().getInventory().equals(inventoryCloseEvent.getInventory()))
                .findFirst();
        if (!opt.isPresent()) return;
        Crate crate = opt.get().getValue();

        int x = 0;
        for (ItemStack i : crate.getData().getInventory().getContents()) {
            if (i == null) continue;
            if (!i.getType().equals(Material.AIR)) x++;
        }

        if (x > 0) return;
        deleteCrate(crate, crate.getCrateLocation().getBlock());
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent breakEvent) {
        if (!breakEvent.getBlock().getType().equals(Material.CHEST)
                || breakEvent.isCancelled()) { return; }
        Crate crate = serverCrates.get(breakEvent.getBlock());
        if (crate == null) return;
        if (crate.getOwner().getUniqueId() != breakEvent.getPlayer().getUniqueId()
                && !breakEvent.getPlayer().hasPermission("deathcrates.override")) return;
        crate.getOwner().giveExp(crate.getData().getNewExp());
        for (HumanEntity humanEntity: crate.getData().getInventory().getViewers()) {
            Player player = (Player)humanEntity;
            player.closeInventory();
        }
        for (ItemStack item: crate.getData().getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType().equals(Material.AIR)) continue;
            if (crate.getData().getInventory().getViewers().size() > 0) {
                for (HumanEntity humanEntity: crate.getData().getInventory().getViewers()) {
                    Player player = (Player)humanEntity;
                    player.closeInventory();
                }
            }
            crate.getWorld().dropItemNaturally(crate.getCrateLocation(), item);
            crate.getData().getInventory().remove(item);
        }
        deleteCrate(crate, crate.getWorld().getBlockAt(crate.getCrateLocation()));

    }


    public void processEvent_SingleBlock(Cancellable event, Block block, Player player) {
        Crate crate = serverCrates.get(block);
        if (crate == null) return;
        if (player != null)
            player.sendMessage(this.getConfig().getString("deathcrates.action-not-allowed-message"));
        event.setCancelled(true);
    }

    public void processEvent_MultipleBlocks(Cancellable event, List<Block> blocks) {
        /*for (Map.Entry<Block, Crate> entry: serverCrates.entrySet()) {
            Crate crate = entry.getValue();
            if (blocks.contains(crate.getWorld().getBlockAt(crate.getCrateLocation()))) {
                event.setCancelled(true);
                return;
            }
        }*/
        for (Block block: blocks) {
            Crate crate = serverCrates.get(block);
            if (crate == null) continue;
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent blockFromToEvent) {
        for (Map.Entry<Block, Crate> entry: serverCrates.entrySet()) {
            Crate crate = entry.getValue();
            if (blockFromToEvent.getBlock().equals(crate.getWorld().getBlockAt(crate.getCrateLocation()))
                    || blockFromToEvent.getToBlock().equals(crate.getWorld().getBlockAt(crate.getCrateLocation()))) {
                blockFromToEvent.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent explodeEvent) {
        processEvent_MultipleBlocks(explodeEvent, explodeEvent.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent explodeEvent) {
        processEvent_MultipleBlocks(explodeEvent, explodeEvent.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtendEvent(BlockPistonExtendEvent extendEvent) {
        processEvent_MultipleBlocks(extendEvent, extendEvent.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetractEvent(BlockPistonRetractEvent retractEvent) {
        processEvent_MultipleBlocks(retractEvent, retractEvent.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(BlockBurnEvent burnEvent) {
        processEvent_SingleBlock(burnEvent, burnEvent.getBlock(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockIgnite(BlockIgniteEvent igniteEvent) {
        processEvent_SingleBlock(igniteEvent, igniteEvent.getBlock(), igniteEvent.getPlayer());
    }


    public void spawnCrate(Location blockLocation, List<ItemStack> drops, World currentWorld, int newXP, Player player, Material previousBlock,
                           Location playerDeathLocation)
    {
        if (drops.size() < 1) return;
        String message = this.getConfig().getString("deathcrates.crate-name", player.getDisplayName() + "'s Death Crate");
        //String crateName = message.replaceAll("(.*)NAME", player.getDisplayName());
        String crateName = message.replace("NAME", player.getDisplayName());
        Inventory inventory = Bukkit.createInventory(null, 5 * 9, crateName);
        for (ItemStack its: drops) {
            inventory.addItem(its);
        }
        PlayerCrateData pcd = new PlayerCrateData(inventory, player, previousBlock, newXP, 0L, playerDeathLocation);
        //--Location entityLocation = new Location(currentWorld, blockLocation.getBlockX() + 0.015625, blockLocation.getBlockY() - 0.015625,
        //--blockLocation.getBlockZ() - 0.015625);
        //LivingEntity holoEntity = (LivingEntity)currentWorld.spawnEntity(entityLocation, EntityType.SHULKER_BULLET);
        LivingEntity holoEntity = (LivingEntity)currentWorld.spawnEntity(blockLocation, EntityType.SHULKER );
        holoEntity.setCustomName(crateName);
        holoEntity.setSilent(true);
        holoEntity.setInvulnerable(true);
        holoEntity.setCustomNameVisible(true);
        holoEntity.setAI(false);
        holoEntity.setCanPickupItems(false);
        holoEntity.setGlowing(false);
        holoEntity.setCollidable(false);
        //holoEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int)(pcd.getExpiryTime() - System.currentTimeMillis()), 1));
        Crate crate = new Crate(player, pcd, currentWorld, blockLocation, holoEntity);
        serverCrates.put(currentWorld.getBlockAt(crate.getCrateLocation()), crate);
        currentWorld.getBlockAt(crate.getCrateLocation()).setType(Material.CHEST);
        Inventory inv = Bukkit.createInventory(

                (InventoryHolder)crate.getWorld().getBlockAt(crate.getCrateLocation()),
                crate.getData().getInventory().getSize(),
                crate.getData().getInventory().getTitle()
        );
        for (ItemStack itemStack: crate.getData().getInventory().getStorageContents()) {
            inv.addItem(itemStack);
        }
        crate.getData().setInventory(inv);
        serverCrates.replace(currentWorld.getBlockAt(crate.getCrateLocation()), crate);
    }

    public void deleteCrate(Crate crate, Block block) {
        serverCrates.remove(crate.getWorld().getBlockAt(crate.getCrateLocation()));
        crate.getWorld().getBlockAt(crate.getCrateLocation()).setType(crate.getData().getPreviousMaterial());
        crate.getHologramEntity().remove();
    }

    /*public Crate getCrate(Player player, Block block) {
        for (Map.Entry<Block, Crate> entry: serverCrates.entrySet()) {
            Crate cratex = entry.getValue();
            if (cratex.getOwner().getUniqueId().equals(player.getUniqueId()) && cratex.getWorld().getBlockAt(cratex.getCrateLocation()).equals(block))
                return cratex;
        }
        return null;
    }

    public Crate getCrate(Player player) {
        for (Map.Entry<Block, Crate> entry: serverCrates.entrySet()) {
            Crate cratex = entry.getValue();
            if (cratex.getOwner().getUniqueId().equals(player.getUniqueId()))
                return cratex;
        }
        return null;
    } */

    public Crate getCrate(Block block) {
        for (Map.Entry<Block, Crate> entry: serverCrates.entrySet()) {
            Crate cratex = entry.getValue();
            Block blockx = entry.getKey();
            if (blockx.equals(block))
                return cratex;
        }
        return null;
    }

    private Crate onRespawn(Player player) {
        for (Map.Entry<Block, Crate> entry: serverCrates.entrySet()) {
            Crate x = entry.getValue();
            if (x.getOwner().getUniqueId().equals(player.getUniqueId()) && x.getData().getExpiryTime() == 0) {
                return x;
            }
        }
        return null;
    }

}
