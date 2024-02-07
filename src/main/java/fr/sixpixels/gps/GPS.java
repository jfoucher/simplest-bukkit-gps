package fr.sixpixels.gps;

import com.samjakob.spigui.SpiGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

public final class GPS extends JavaPlugin {
    public static SpiGUI GUI;
    private YamlConfiguration language;
    public HashMap<UUID, LocationFinder> finders = new HashMap<>();


    @Override
    public void onEnable() {
        saveDefaultConfig();
        GUI = new SpiGUI(this);

        this.loadLanguageFile();
        // Plugin startup logic

        Bukkit.getServer().getPluginManager().registerEvents(new GPSListener(this), this);

        PluginCommand cmd = this.getCommand("gps");
        if (cmd != null) {
            cmd.setExecutor(new GPSCommand(this));
        }
    }

    public void reload(CommandSender sender) {
        this.reloadConfig();
        this.loadLanguageFile();
        sender.sendMessage(
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        getConfig().getString("prefix") + this.getLanguage().getString("PLUGIN_RELOADED")
                )
        );

    }

    public void arrived(Player player) {
        LocationFinder f = this.finders.get(player.getUniqueId());
        String m = getConfig().getString("destinations." + f.destinationName + ".message_end");
        if (m == null) {
            m = this.getLanguage().getString("ARRIVED_DESTINATION");
        }

        if (m != null) {
            String msg = m.replace("%destination%", f.destinationName);
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("prefix") + msg));
        }

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 127, 255), 1.5F);
        Location dest = f.destination.clone();

        dest.setY(dest.getY() + 0.5);
        for (int i = -2; i < 3; i++) {
            dest.setX(dest.getX() + 0.5 * (i % 2));
            dest.setZ(dest.getZ() + 0.2 * (i));
            player.spawnParticle(Particle.REDSTONE, f.destination, 50, dustOptions);
        }

        String ti = this.getLanguage().getString("END_TITLE");
        if (ti == null) {
            ti = "GPS active";
        }
        String sti = this.getLanguage().getString("END_SUBTITLE");
        if (sti == null) {
            sti = "Destination %destination%";
        }

        sti = sti.replace("%destination%", f.destinationName);
        Component t = LegacyComponentSerializer.legacyAmpersand().deserialize(ti);
        Component st = LegacyComponentSerializer.legacyAmpersand().deserialize(sti);
        final Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500));
        final Title title = Title.title(t, st, times);

        player.showTitle(title);

        this.finders.remove(player.getUniqueId());
    }

    public void createDestination(Player p, String[] args) {
        String name = "";
        String description = "";
        String material = "JUNGLE_LOG";
        if (args.length > 1) {
            name = args[1];
        }
        if (args.length > 2) {
            Material m = Material.getMaterial(args[2].toUpperCase());
            if (m != null) {
                material = m.name();
            }

        }

        if (args.length > 3) {
            String[] ss = Arrays.copyOfRange(args, 3, args.length);
            description = String.join(" ", ss);
        }


        ConfigurationSection dests = getConfig().getConfigurationSection("destinations");
        if (dests != null) {
            if (dests.getConfigurationSection(name) != null) {
                String err = this.getLanguage().getString("DESTINATION_ALREADY_EXISTS");
                if (err != null) {
                    p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(err.replace("%destination%", name)));
                }
                return;
            }

            ConfigurationSection d = dests.createSection(name);
            d.set("name", name);
            d.set("world", p.getWorld().getName());
            d.set("x", p.getLocation().getX());
            d.set("y", p.getLocation().getY());
            d.set("z", p.getLocation().getZ());
            d.set("description", description);
            d.set("material", material);
            getConfig().set("destinations." + name, d);
            saveConfig();
            String ce = this.getLanguage().getString("DESTINATION_CREATED");
            if (ce == null) {
                ce = "Destination &d&l%destination% created here";
            }
            String l = ce.replace("%destination%", name);
            p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("prefix") + l));
        }
    }

    public void listDestinations(CommandSender sender) {
        ConfigurationSection dests = getConfig().getConfigurationSection("destinations");
        if (dests != null) {
            Set<String> destinations = dests.getKeys(false);
            String l = this.getLanguage().getString("DESTINATIONS_LIST");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("prefix") + l));
            for (String dest: destinations) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&r&a" + dest + "&7 (" + Objects.requireNonNull(dests.getConfigurationSection(dest)).getString("world") + ")"
                ));
            }
        }
    }

    public void removeDestination(Player p, String name) {
        ConfigurationSection dests = getConfig().getConfigurationSection("destinations");
        if (dests != null) {
            ConfigurationSection d = dests.getConfigurationSection(name);
            if (d != null) {
                getConfig().set("destinations."+name, null);
                saveConfig();
                String de = this.getLanguage().getString("DESTINATION_REMOVED");
                if (de == null) {
                    de = "Destination &d&l%destination% was removed";
                }
                String l = de.replace("%destination%", name);
                p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("prefix") + l));
            } else {
                p.sendMessage(getConfig().getString("prefix") + " quest does not exist");
            }


        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }



    public Configuration getLanguage() {
        if (this.language == null) {
            this.loadLanguageFile();
        }
        return this.language;
    }

    private void loadLanguageFile() {
        String lang = getConfig().getString("language");

        if (lang == null) {
            Bukkit.getLogger().warning("[GPS] Please set a language in config.yml");
            lang = "en_US";
        }

        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            saveResource("lang/" + lang + ".yml", false);

        }

        this.language = new YamlConfiguration();
        try {
            this.language.load(langFile);
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getLogger().warning("Could not load language file for language " + lang);
        }
    }
}
