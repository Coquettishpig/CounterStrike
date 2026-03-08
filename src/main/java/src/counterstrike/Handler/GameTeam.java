package src.counterstrike.Handler;

import org.bukkit.entity.Player;

import java.util.List;

public class GameTeam {
    private Role role;
    private List<Player> players;

    public GameTeam(final List<Player> players) {
        this.players = null;
        this.players = players;
    }

    public void setRole(final Role role) {
        this.role = role;
    }

    public Role getRole() {
        return this.role;
    }

    public Player getPlayer(final int i) {
        return this.players.get(i);
    }

    public void addPlayer(final Player p) {
        this.players.add(p);
    }

    public void removePlayer(final Player p) {
        this.players.remove(p);
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    public int size() {
        return this.players.size();
    }

    public static Role getEnum(final String name) {
        for (final Role type : Role.values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public enum Role {
        TERRORIST,
        COUNTERTERRORIST;
    }
}
