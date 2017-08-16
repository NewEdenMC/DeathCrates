package co.neweden.DeathCrates;

import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;

public class Crate
{
    private Player owner;
    private PlayerCrateData pcd;
    private Shulker shulker;
    public Crate(Player _owner, PlayerCrateData _pcd, Shulker _shulker)
    {
        owner = _owner;
        pcd = _pcd;
        shulker = _shulker;
    }

    public Player getOwner() { return owner; }
    public PlayerCrateData getData() { return pcd; }
    public Shulker getShulker() { return shulker; }
}
