package me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.Queries;

import java.sql.ResultSet;
import java.sql.SQLException;
import me.kyle.burnett.SkyBlockWarriors.DatabaseHandler.SQLSelection;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class PlayerWins implements Listener {

    public static void setPlayerWins(Player p, int value) throws SQLException, ClassNotFoundException {
        int wins = 0;
        ResultSet rs = SQLSelection.getStatement().executeQuery("SELECT wins FROM sbw WHERE username='" + p.getName() + "';");
        if (rs.next()) {
            wins = rs.getInt(1) + value;
            SQLSelection.getStatement().execute("UPDATE sbw SET wins=" + wins + " WHERE username='" + p.getName() + "';");
        }

    }
}
