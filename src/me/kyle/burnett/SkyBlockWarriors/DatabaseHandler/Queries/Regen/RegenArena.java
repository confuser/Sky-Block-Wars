package me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.SQLSelection;

public class RegenArena {

  public void checkTable() throws SQLException, ClassNotFoundException {
		 	
		Connection con = null;

	    con = SQLSelection.getConnection();

	    con.createStatement().execute("CREATE TABLE IF NOT EXISTS regen(world VARCHAR(255), x INTEGER, y INTEGER, z INTEGER, block INTEGER, arena VARCHAR(255));");
	}
	
	public void addBlock(String world, int x, int y, int z, int block, int arena) throws SQLException, ClassNotFoundException {
		
		Connection con = null;
		
		con = SQLSelection.getConnection();
		
		con.createStatement().execute("INSERT INTO regen (world, x, y, z, block, arena) VALUES('" + world + "', " + x + "," + y + ", " + z + ", " + block + ", " + arena + ");");
	}
	
	@SuppressWarnings("null")
	public BlockLocation[] getBlocks(int arena) throws SQLException, ClassNotFoundException {
		
		BlockLocation[] blocks = null;
		
		ResultSet rs = null;
		
		Connection con = null;
		
		int i = 1;
		
		con = SQLSelection.getConnection();
		
		rs = con.createStatement().executeQuery("SELECT world, x, y, z FROM regen WHERE arena = " + arena + ";");
		
		while(rs.next()) {
			
			blocks[i] = new BlockLocation(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5));
			
			i++;
		}
		return blocks;
	}
	
	public void removeBlock(int arena) throws SQLException, ClassNotFoundException {
		
		Connection con = null;
		
		con = SQLSelection.getConnection();
		
		con.createStatement().execute("DELETE FROM regen WHERE arena = " + arena + ";");
	}
}
