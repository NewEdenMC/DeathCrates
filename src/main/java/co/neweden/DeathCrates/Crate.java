package co.neweden.DeathCrates;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Crate
{
    private Player owner;
    private PlayerCrateData pcd;
    private World world;
    private Location location;
    private Entity hologramEntity;
    public Crate(Player _owner, PlayerCrateData _pcd, World _world, Location _location, Entity _hologramEntity)
    {
        owner = _owner;
        pcd = _pcd;
        world = _world;
        location = _location;
        hologramEntity = _hologramEntity;
    }

    public Player getOwner() { return owner; }
    public PlayerCrateData getData() { return pcd; }
    public World getWorld() { return world; }
    public Location getCrateLocation() { return location; }
    public Entity getHologramEntity() { return hologramEntity; }
}
