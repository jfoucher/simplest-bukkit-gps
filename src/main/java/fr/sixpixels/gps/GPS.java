package fr.sixpixels.gps;

import com.samjakob.spigui.SpiGUI;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
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

    public void arrived(Player player) {
        LocationFinder f = this.finders.get(player.getUniqueId());
        String m = getConfig().getString("destinations." + f.destinationName + ".message_end");
        if (m == null) {
            m = this.getLanguage().getString("ARRIVED_DESTINATION");
        }

        if (m != null) {
            String msg = m.replace("%destination%", f.destinationName);
            player.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', msg)));
        }

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
            description = String.join(" ", ss);;
        }


        ConfigurationSection dests = getConfig().getConfigurationSection("destinations");
        if (dests != null) {
            if (dests.getConfigurationSection(name) != null) {
                String err = this.getLanguage().getString("DESTINATION_ALREADY_EXISTS");
                if (err != null) {
                    p.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes( '&', err.replace("%destination%", name))));
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

            String l = this.getLanguage().getString("DESTINATION_CREATED").replace("%destination%", name);
            p.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix") + l)));
        }
    }

    public void listDestinations(CommandSender sender) {
        ConfigurationSection dests = getConfig().getConfigurationSection("destinations");
        if (dests != null) {
            Set<String> destinations = dests.getKeys(false);
            String l = this.getLanguage().getString("DESTINATIONS_LIST");
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix") + l)));
            for (String dest: destinations) {
                sender.sendMessage(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', "&r&a" + dest + "&7 (" + Objects.requireNonNull(dests.getConfigurationSection(dest)).getString("world") + ")")
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
                String l = this.getLanguage().getString("DESTINATION_REMOVED").replace("%destination%", name);
                p.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix") + l)));
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
            e.printStackTrace();
        }
    }
}
