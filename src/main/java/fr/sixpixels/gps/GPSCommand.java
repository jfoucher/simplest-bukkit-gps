package fr.sixpixels.gps;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;

public class GPSCommand implements CommandExecutor {
    GPS plugin;


    public GPSCommand(GPS plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (commandSender instanceof Player && args.length == 0) {
            Player p = (Player) commandSender;
            this.openMainMenu(p);
            return true;
        }

        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    commandSender.sendMessage(TextComponent.fromLegacyText(this.plugin.getConfig().getString("prefix") + "&r&7 usage &a/gps delete <quest>"));
                    return false;
                }
                this.plugin.removeDestination(p, args[1]);
                return true;
            } else if (args[0].equalsIgnoreCase("add")) {
                if (args.length < 2) {
                    commandSender.sendMessage(TextComponent.fromLegacyText(this.plugin.getConfig().getString("prefix") + "&r&7 usage &a/gps add <quest> [material] [description]"));
                    return false;
                }
                this.plugin.createDestination(p, args);
                return true;
            }
        }

        return false;
    }

    private void openMainMenu(Player player) {

        // Create a GUI with 3 rows (27 slots)
        Bukkit.getLogger().log(Level.FINER, "[GPS] Opening menu " + t("MAIN_MENU_TITLE"));
        SGMenu mainMenu = GPS.GUI.create(t("MAIN_MENU_TITLE"), 3);

        ConfigurationSection destinations = this.plugin.getConfig().getConfigurationSection("destinations");
        if (destinations == null) {
            Bukkit.getLogger().warning("[GPS] No destinations are present");
            return;
        }
        Set<String> keys = destinations.getKeys(false);
        for (String destination: keys) {
            // Create a button
            ConfigurationSection dest = destinations.getConfigurationSection(destination);

            if (dest != null) {
                Material material = Material.JUNGLE_LOG;
                String m = dest.getString("material");
                if (m != null) {
                    material = Material.getMaterial(m);
                }
                String name = "&a" + dest.getString("name");
                ArrayList<String> lore = new ArrayList<>();
                lore.add(dest.getString("description"));
                if (!player.getWorld().getName().equals(dest.getString("world"))) {
                    name = "&7" + dest.getString("name") + " (" + dest.getString("world_desc") + ")";
                    lore.add(t("GO_TO_WORLD") + " &3&l" + dest.getString("world_desc"));
                }

                SGButton btn = new SGButton(
                        new ItemBuilder(material)
                                .name(name)
                                .lore(lore)
                                .build()
                ).withListener((InventoryClickEvent event) -> {
                    // Events are cancelled automatically, unless you turn it off
                    // for your plugin or for this inventory.

                    // check if we are on the correct world
                    if (player.getWorld().getName().equals(dest.getString("world"))) {
                        String msg = dest.getString("message_start");
                        if (msg != null) {
                            event.getWhoClicked().sendMessage(msg);
                        } else {
                            event.getWhoClicked().sendMessage(t("DEFAULT_MESSAGE_START"));
                        }
                        Bukkit.getLogger().info("[GPS] " + player.getName() + " set GPS to " + dest.getString("name"));
                        // trigger GPS
                        // get destination point
                        String destName = dest.getString("name");
                        Location loc = new Location(player.getWorld(), (float)dest.getInt("x"), (float)dest.getInt("y"), (float)dest.getInt("z"));
                        LocationFinder finder = new LocationFinder(player, loc, destName, this.plugin);
                        this.plugin.finders.put(player.getUniqueId(), finder);
                        // create bossbar
                        finder.addBossbar();

                        finder.actionBarTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                            String message = "Direction: §6§l" + destName;
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                        }, 0, 20L*2);

                        // Close inventory
                        event.getInventory().close();
                    }
                });

                // Add the button to your GUI
                mainMenu.addButton(btn);
            }

        }


        // Show the GUI
        player.openInventory(mainMenu.getInventory());

    }

    private String t(String key) {
        return this.plugin.getLanguage().getString(key);
    }
}
