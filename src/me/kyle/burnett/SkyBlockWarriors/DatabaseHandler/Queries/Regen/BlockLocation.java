package me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen;

public class BlockLocation {

    private String world;
    private int x;
    private int y;
    private int z;
    private int typeID;
    private byte data;

    public BlockLocation(String world, int x, int y, int z, int typeID, byte data) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.typeID = typeID;
        this.data = data;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getBlock() {
        return typeID;
    }

    public byte getData(){
        return data;
    }
}
