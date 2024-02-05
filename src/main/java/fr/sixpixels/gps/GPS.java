package fr.sixpixels.gps;

import com.samjakob.spigui.SpiGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public final class GPS extends JavaPlugin {
    public static SpiGUI GUI;
    public Configuration config;
    private YamlConfiguration language;
    public HashMap<UUID, LocationFinder> finders = new HashMap<>();


    @Override
    public void onEnable() {
        saveDefaultConfig();
        GUI = new SpiGUI(this);
        this.config = getConfig();

        this.loadLanguageFile();
        // Plugin startup logic

        Bukkit.getServer().getPluginManager().registerEvents(new GPSListener(this), this);

        PluginCommand cmd = this.getCommand("gps");
        if (cmd != null) {
            cmd.setExecutor(new GPSCommand(this));
        }

    }

    public void arrived(Player player) {
        String m = this.getLanguage().getString("ARRIVED_DESTINATION");
        if (m != null) {
            LocationFinder f = this.finders.get(player.getUniqueId());
            String msg = m.replace("%destination%", f.destinationName);
            player.sendMessage(msg);
        }

        this.finders.remove(player.getUniqueId());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }



    public Configuration getLanguage() {
        return this.language;
    }

    private void loadLanguageFile() {
        String lang = this.config.getString("language");

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
