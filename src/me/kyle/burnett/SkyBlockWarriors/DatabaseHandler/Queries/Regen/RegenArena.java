package me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries.Regen;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.block.Block;

import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.SQLSelection;

public class RegenArena {

    public static void checkTable() throws SQLException, ClassNotFoundException {

        Connection con = null;

        con = SQLSelection.getConnection();

        con.createStatement().execute("CREATE TABLE IF NOT EXISTS regen(world VARCHAR(255), x INTEGER, y INTEGER, z INTEGER, block INTEGER, type BLOB, arena INTEGER, placed BOOLEAN);");
    }

    public static void addBlockPlaced(String world, int x, int y, int z, byte[] type, int arena) throws SQLException, ClassNotFoundException {

        Connection con = null;

        con = SQLSelection.getConnection();

        con.createStatement().execute("INSERT INTO regen (world, x, y, z, block, arena, placed) VALUES('" + world + "', " + x + "," + y + ", " + z + ", " + type + ", " + arena + ", " + true + ");");
    }

    public static void addBlockBroken(String world, int x, int y, int z, byte[] type, int arena) throws SQLException, ClassNotFoundException {

        Connection con = null;

        con = SQLSelection.getConnection();

        con.createStatement().execute("INSERT INTO regen (world, x, y, z, block, arena, placed) VALUES('" + world + "', " + x + "," + y + ", " + z + ", " + type + ", " + arena + ", " + false + ");");
    }

    public static void removeBlockPlaced(int arena, Block b) throws SQLException, ClassNotFoundException {

        Connection con = null;

        con = SQLSelection.getConnection();

        con.createStatement().execute("DELETE FROM regen WHERE arena = " + arena + ";");
    }

    public static void removeBlockBroken(int arena, Block b) throws SQLException, ClassNotFoundException {

        Connection con = null;

        con = SQLSelection.getConnection();

        con.createStatement().execute("DELETE FROM regen WHERE arena = " + arena + ";");
    }

    @SuppressWarnings("null")
    public static List<BlockLocation> getBlocksPlaced(int arena) throws SQLException, ClassNotFoundException {

        List<BlockLocation> blocks = null;

        ResultSet rs = null;

        Connection con = null;

        con = SQLSelection.getConnection();

        rs = con.createStatement().executeQuery("SELECT world, x, y, z, block, type, placed FROM regen WHERE arena = " + arena + ";");

        while (rs.next()) {
            blocks.add(new BlockLocation(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getByte(6)));
        }
        return blocks;
    }

    @SuppressWarnings("null")
    public static List<BlockLocation> getBlocksBroken(int arena) throws SQLException, ClassNotFoundException {

        List<BlockLocation> blocks = null;

        ResultSet rs = null;

        Connection con = null;

        con = SQLSelection.getConnection();

        rs = con.createStatement().executeQuery("SELECT world, x, y, z, block, type, placed FROM regen WHERE arena = " + arena + ";");

        while (rs.next()) {
            blocks.add(new BlockLocation(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getByte(6)));
        }
        return blocks;
    }
}
