package fr.sixpixels.gps;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GPSListener implements Listener {
    public GPS plugin;
    public GPSListener(GPS plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        //if (e.hasChangedBlock() || e.hasChangedOrientation()) {
            Player p = e.getPlayer();
            LocationFinder finder = this.plugin.finders.get(p.getUniqueId());
            if (finder != null) {
                finder.updateBossbar(p);

            }

        //}
    }
    @EventHandler
    public void onPlayerDead(PlayerDeathEvent e) {
        removeGps(e.getPlayer());

    }

    private void removeGps(Player p) {
        LocationFinder finder = this.plugin.finders.get(p.getUniqueId());
        if (finder != null) {
            if (finder.npc != null) {
                if (finder.npc.isSpawned()) {
                    finder.npc.despawn();
                }

                finder.npc.destroy();
            }

            this.plugin.finders.remove(p.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent e) {
        removeGps(e.getPlayer());

    }
    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        removeGps(e.getPlayer());
    }
}
