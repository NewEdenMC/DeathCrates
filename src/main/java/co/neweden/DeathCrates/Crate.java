package co.neweden.DeathCrates;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;

import java.util.Optional;
import java.util.UUID;

public class Crate
{
    private Player owner;
    private PlayerCrateData pcd;
    private UUID shulkerUuid;
    private Location shulkerLocation;
    private boolean isDeleted;
    public Crate(Player _owner, PlayerCrateData _pcd, Shulker _shulker)
    {
        owner = _owner;
        pcd = _pcd;
        shulkerUuid = _shulker.getUniqueId();
        shulkerLocation = _shulker.getLocation();

        isDeleted = false;
    }

    public Crate(Player _owner, PlayerCrateData _pcd, Shulker _shulker, Location _loc)
    {
        owner = _owner;
        pcd = _pcd;
        shulkerUuid = _shulker.getUniqueId();
        shulkerLocation = _shulker.getLocation();

        isDeleted = false;
    }

    public Crate(Player _owner, PlayerCrateData _pcd, UUID _shulkerUuid)
    {
        owner = _owner;
        pcd = _pcd;
        shulkerUuid = _shulkerUuid;

        isDeleted = false;

        // get from server
        Entity e = Bukkit.getEntity(shulkerUuid);
        if (e == null || !(e instanceof Shulker)) shulkerLocation = null; else shulkerLocation = e.getLocation();
    }

    public Crate(Player _owner, PlayerCrateData _pcd, UUID _shulkerUuid, Location _loc)
    {
        owner = _owner;
        pcd = _pcd;
        shulkerUuid = _shulkerUuid;
        shulkerLocation = _loc;
        isDeleted = false;
    }

    public Player getOwner() { return owner; }
    public PlayerCrateData getData() { return pcd; }
    public Location getShulkerLocation() { return shulkerLocation; }
    public Shulker getShulker() {
        Entity entityFromServer = Bukkit.getEntity(shulkerUuid);
        if (entityFromServer == null || !(entityFromServer instanceof Shulker))
            return null;
        else
            return (Shulker) entityFromServer;
    }

    public UUID getShulkerUuid() { return shulkerUuid; }

    public boolean reloadShulkerChunk()
    {
        if (shulkerLocation == null) return false;
        Chunk chunk = shulkerLocation.getChunk();
        /* Performance improvement: check if it's already loaded, no need to load an already loaded one, right? */
        if (chunk.isLoaded()) return true;
        /* End. */
        boolean b = chunk.load(true); // idk what to call this
        return b;
    }

    public Shulker reloadAndGet()
    {
        if (shulkerLocation == null || shulkerUuid == null) return null;
        boolean b = reloadShulkerChunk();
        Entity e = Bukkit.getEntity(shulkerUuid);
        if (e == null || !(e instanceof Shulker)/* || !b*/) return null;
        return (Shulker)e;
    }

    public boolean isNonexistant()
    {
        Shulker s = reloadAndGet();
        return s == null;
    }

    public boolean deleteIfNonexistant(Main main)
    {
        if (!isNonexistant()) return false; // did not delete
        // it's nonexistant, deleting..

        if (shulkerUuid != null)
        {
            main.getServerCrates().remove(shulkerUuid);
            main.getShulker_bool().remove(shulkerUuid);
        }

        if (getShulkerLocation() != null)
        {
            Location l = getShulkerLocation();
            l.getWorld().spawnParticle(Particle.SMOKE_LARGE, l, 3);
        }

        return true;
    }

    public boolean isDeleted() { return isDeleted; }

}