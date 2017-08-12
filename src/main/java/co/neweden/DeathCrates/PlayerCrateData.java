package co.neweden.DeathCrates;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PlayerCrateData
{
    private Location playerDeathLocation;
    private Inventory inventory;
    private int originalExp;
    private int newExp;
    private Player player;
    private long expiryTime;
    private Material previousBlock;
    private boolean hasSentRespawnMessage;
    public PlayerCrateData(Inventory _inventory, Player _player, Material _previousBlock, int _newExp, long _expiryTime, Location _playerDeathLocation)
    {
        inventory = _inventory;
        player = _player;
        expiryTime = _expiryTime;
        previousBlock = _previousBlock;
        newExp = _newExp;
        playerDeathLocation = _playerDeathLocation;
        hasSentRespawnMessage = false;
    }
    public boolean getHasSentRespawnMessage() { return hasSentRespawnMessage; }
    public void setHasSentRespawnMessage(boolean newBool) { hasSentRespawnMessage = newBool; }
    public Inventory getInventory() { return inventory; }
    public Location getPlayerDeathLocation() { return playerDeathLocation; }
    public Player getPlayer() { return player; }
    public int getNewExp() { return newExp; }
    public long getExpiryTime() { return expiryTime; }
    public Material getPreviousMaterial() { return previousBlock; }
    public void setExpiryTime(long newExpiryTime)
    {
        expiryTime = newExpiryTime;
    }
    public void setNewExp(int newerExp)
    {
        newExp = newerExp;
    }
    public void setInventory(Inventory newInventory) {
        for (HumanEntity humanEntity: inventory.getViewers()) {
            Player p = (Player)humanEntity;
            p.closeInventory();
        }
        inventory = newInventory;
    }

}