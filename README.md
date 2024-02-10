# Simplest Minecraft bukkit GPS plugin

This is a GPS plugin for minecraft Bukkit servers. I've tested on Paper 1.20.4
But it should work on compatible bukkit servers.

It will guide your player to interesting places in your world.

If you have the Citizens2 plugin installed, an NPC will spawn that will guide the player to their destination.

In all cases, a "bossbar" will show the direction and the distance left to cover.

## Commands

The following commands are available

- `/gps` will show the player GUI to select a destination. The same command will also allow a player to cancel their travel plans. Permission : `gps.use`
- `/gps add <name> [material] [description]` will add a destination to the GPS. The destination is where you are standing. The second argument is the material to use e.g. `DIAMOND_SWORD` or `SOUL_FIRE`. The last argument is the destination description (will appear in the material lore in the GUI). Permission : `gps.admin`
- `/gps delete <name>` will delete the selected destination. Permission : `gps.admin`
- `/gps list` will list all available destinations. Permission : `gps.admin`
- `/gps reload` will reload the GPS configuration

## Permissions

- `gps.use` gives access to the GPS GUI to go to a destination
- `gps.admin` gives access to the admin commands (add, delete, list and reload)

## Translation

Add your translation file in the `lang` folder or copy the `en_US.yml` file and edit the strings therein.
Then change the `language` configuration in config.yml to be the name of your file without the `.yml` part
Restart your server.