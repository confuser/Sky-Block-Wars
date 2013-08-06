package me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen;

class BlockLocation {

  public String world;
	public int x;
	public int y;
	public int z;
	public int block;
	
	public BlockLocation(String world, int x, int y, int z, int block)
	{
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.block = block;
	}
	
	public String getWorld()
	{
		return world;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public int getBlock()
	{
		return block;
	}
}
