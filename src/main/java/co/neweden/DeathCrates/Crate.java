package co.neweden.DeathCrates;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;

import java.util.UUID;

public class Crate
{
    private Player owner;
    private PlayerCrateData pcd;
    private UUID shulkerUuid;
    public Crate(Player _owner, PlayerCrateData _pcd, Shulker _shulker)
    {
        owner = _owner;
        pcd = _pcd;
        shulkerUuid = _shulker.getUniqueId();
    }

    public Player getOwner() { return owner; }
    public PlayerCrateData getData() { return pcd; }
    public Shulker getShulker() {
        Entity entityFromServer = Bukkit.getEntity(shulkerUuid);
        if (entityFromServer == null || !(entityFromServer instanceof Shulker))
            return null;
        else
            return (Shulker) entityFromServer;
    }

}