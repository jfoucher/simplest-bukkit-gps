package fr.sixpixels.gps;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

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
}
