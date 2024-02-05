package fr.sixpixels.gps;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.UUID;

public class LocationFinder {
    public Player player;

    private GPS plugin;
    public Location destination;

    public Location start;

    public String destinationName;

    public BossBar bar;

    public BukkitTask actionBarTask;

    public LocationFinder(Player player, Location loc, String name, GPS plugin) {
        this.plugin = plugin;
        this.player = player;
        this.destination = loc;
        this.destinationName = name;
        this.start = player.getLocation();
    }

    public void addBossbar() {
        Component title = this.title();
        this.bar = BossBar.bossBar(title, this.progress(), BossBar.Color.RED, BossBar.Overlay.PROGRESS);

        player.showBossBar(bar);
    }

    public void updateBossbar(Player player) {
        this.player = player;
        // This should be our bossbar
        bar.name(this.title());
        bar.progress(this.progress());
    }

    public Component title() {
        Location ploc = player.getLocation();
        // direction the player is pointing towards
        Vector direction = ploc.getDirection();
        player.sendMessage("dir " + direction);
        // Get direction from player location to destination
        Vector heading = this.destination.toVector().subtract(ploc.toVector());
        player.sendMessage("heading " + heading);
        String posChar = "⬤";
        if (heading.getBlockY() > 0) {
            posChar = "↑";
        }
        if (heading.getBlockY() < 0) {
            posChar = "↓";
        }

        heading.setY(0);
        direction.setY(0);




//        dot = x1*x2 + y1*y2      # Dot product between [x1, y1] and [x2, y2]
//        det = x1*y2 - y1*x2      # Determinant
//        angle = atan2(det, dot)  # atan2(y, x) or atan2(sin, cos)

        Vector a = this.destination.toVector().subtract(ploc.toVector()).normalize();
        Vector b = ploc.getDirection();
        double dot = a.dot(b);
        double det = a.getX() * b.getZ() - a.getZ() * b.getX();
        double angle = Math.atan2(det, dot);
        angle = Math.toDegrees(-angle);
        //double angle = heading.angle(direction) * 180 / Math.PI;
        int charpos = (int) Math.round(angle / 4.0 + 45.0/2.0);
        player.sendMessage("Angle " + angle);
        player.sendMessage("charpos " + charpos);
        if (charpos > 45) {
            posChar = "→";
            charpos=45;
        }
        if (charpos < 0) {
            posChar = "←";
            charpos = 0;
        }


        Component t = Component.text(new String(new char[charpos]).replace("\0", "—"), TextColor.color(0xCCCCCC));
        t = t.append(Component.text(posChar).style(Style.style(TextColor.color(0xFF2233), TextDecoration.BOLD)));
        t= t.append(Component.text(new String(new char[45-charpos]).replace("\0", "—"), TextColor.color(0xCCCCCC)));

        player.sendMessage(t);
        return t;
    }

    public float progress() {
        double orig = this.start.distance(this.destination);
        if ((int)Math.round(orig) == 0) {
            return 0.0f;
        }
        double dist = player.getLocation().distance(this.destination);
        if (dist < 1) {
            this.actionBarTask.cancel();
            this.plugin.arrived(player);

        }
        float p = (float)dist / (float)orig;
        player.sendMessage("progress is " + p);
        if (p < 0.1f) {
            return 0.0f;
        }

        if (p > 1.0f) {
            this.start = this.player.getLocation();
            return 1.0f;
        }
        return p;

    }
}