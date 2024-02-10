package fr.sixpixels.gps;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.ScoreboardTrait;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class LocationFinder {
    public Player player;

    private final GPS plugin;
    public Location destination;

    public Location start;

    public String destinationName;

    public BossBar bar;

    public List<Location> npcPath;

    public BukkitTask actionBarTask;

    public NPC npc;

    public LocationFinder(Player player, Location loc, String name, GPS plugin) {
        this.plugin = plugin;
        this.player = player;
        this.destination = loc;
        this.destinationName = name;
        this.npcPath = new ArrayList<>();
        this.start = player.getLocation();
        if (CitizensAPI.hasImplementation()) {

            Bukkit.getLogger().log(Level.FINEST, "[GPS] adding helper NPC");
            this.npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.BEE, "GPS");

            npc.getOrAddTrait(LookClose.class).setRange(10);
            npc.getOrAddTrait(LookClose.class).setDisableWhileNavigating(true);
            npc.getOrAddTrait(LookClose.class).lookClose(true);
            npc.getOrAddTrait(ScoreboardTrait.class).setColor(ChatColor.GOLD);

            this.npc.spawn(player.getLocation());

            Bukkit.getLogger().info("[GPS] npc spawned " + this.npc.isSpawned());
            Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
                if (this.npc != null && this.npc.isSpawned()) {
                    this.npc.getEntity().setGlowing(true);

                    Location ll = player.getLocation().toCenterLocation();
                    ll.add(0, -1, 0);
                    this.npcPath = Pathfinding.findPath(ll, this.destination);
                    Bukkit.getLogger().log(Level.FINEST, "[GPS] generated path with " + this.npcPath.size() + " locations");

                    if (!this.npcPath.isEmpty()) {
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                            Bukkit.getLogger().log(Level.FINEST, "[GPS] Setting NPC target to " + this.npcPath.get(0));

                            this.npc.getNavigator().setTarget(this.npcPath.get(0));
                            this.npcPath.remove(0);
                        }, 2L);


                    } else {
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                            this.npc.despawn();
                            this.npc.destroy();
                        }, 2L);
                    }
                }
            }, 2L);
        }


        this.bar = BossBar.bossBar(Component.text(""), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    }

    private void forwardNPC() {
        if (npc != null && npc.isSpawned() && !npc.getNavigator().isNavigating() && this.npc.getEntity().getLocation().distanceSquared(player.getLocation()) < 80) {
            if (!this.npcPath.isEmpty()) {
                this.npc.getNavigator().setTarget(this.npcPath.get(0));
                Bukkit.getLogger().log(Level.FINEST, "distance left " + this.npcPath.get(0).distance(this.destination));

                this.npcPath.remove(0);
                Bukkit.getLogger().log(Level.FINEST, "[GPS] " + this.npcPath.size() + " path locations remaining");
            } else {
                this.npc.getNavigator().setTarget(this.destination);
            }
        }
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

        Bukkit.getScheduler().runTaskLater(this.plugin, this::forwardNPC, 2L);

    }



    public Component title() {
        Location ploc = player.getLocation();
        // direction the player is pointing towards
        Vector direction = ploc.getDirection();
        // Get direction from player location to destination
        Vector heading = this.destination.toVector().subtract(ploc.toVector());

        String posChar = "⬤";
        if (heading.getBlockY() > 0) {
            posChar = "↑";
        }
        if (heading.getBlockY() < 0) {
            posChar = "↓";
        }

        heading.setY(0);
        direction.setY(0);

        Vector a = this.destination.toVector().subtract(ploc.toVector()).normalize();
        Vector b = ploc.getDirection();
        double dot = a.dot(b);
        double det = a.getX() * b.getZ() - a.getZ() * b.getX();
        double angle = Math.atan2(det, dot);
        angle = Math.toDegrees(-angle);
        //double angle = heading.angle(direction) * 180 / Math.PI;
        int charpos = (int) Math.round(angle / 4.0 + 45.0/2.0);
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
        return t;
    }

    public float progress() {
        double orig = this.start.distance(this.destination);
        if ((int)Math.round(orig) == 0) {
            return 0.0f;
        }
        double dist = player.getLocation().distance(this.destination);
        if (dist < 2) {
            if (this.actionBarTask != null) {
                this.actionBarTask.cancel();
            }
            if (this.npc != null) {
                if (this.npc.isSpawned()) {
                    this.npc.despawn();
                }

                this.npc.destroy();
            }

            player.hideBossBar(this.bar);
            this.plugin.arrived(player);

        }
        float p = (float)dist / (float)orig;
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
