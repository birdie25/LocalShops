package net.centerleft.localshops.commands;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.centerleft.localshops.Config;
import net.centerleft.localshops.LocalShops;
import net.centerleft.localshops.PlayerData;
import net.centerleft.localshops.Shop;
import net.centerleft.localshops.ShopLocation;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandShopCreate extends Command {

    public CommandShopCreate(LocalShops plugin, String commandLabel, CommandSender sender, String command) {
        super(plugin, commandLabel, sender, command);
    }
    
    public CommandShopCreate(LocalShops plugin, String commandLabel, CommandSender sender, String[] command) {
        super(plugin, commandLabel, sender, command);
    }

    public boolean process() {
        String creator = null;
        String world = null;
        double[] xyzA = new double[3];
        double[] xyzB = new double[3];

        // Get current shop
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerData pData = plugin.getPlayerData().get(player.getName());          

            creator = player.getName();
            world = player.getWorld().getName();

            //Check permissions
            if (!canCreateShop(creator)) {
                sender.sendMessage(LocalShops.CHAT_PREFIX + ChatColor.DARK_AQUA + "You already have the maximum number of shops or don't have permission to create them!");
                return false;
            }

            // If player is select, use their selection
            if (pData.isSelecting()) {
                if (!pData.checkSize()) {
                    String size = Config.SHOP_SIZE_MAX_WIDTH + "x" + Config.SHOP_SIZE_MAX_HEIGHT + "x" + Config.SHOP_SIZE_MAX_WIDTH;
                    player.sendMessage(ChatColor.DARK_AQUA + "Problem with selection. Max size is " + ChatColor.WHITE + size);
                    return false;
                }

                xyzA = pData.getPositionA();
                xyzB = pData.getPositionB();

                if (xyzA == null || xyzB == null) {
                    player.sendMessage(ChatColor.DARK_AQUA + "Problem with selection. Only one point selected");
                    return false;
                }
            } else {
                // get current position
                Location loc = player.getLocation();
                long x = loc.getBlockX();
                long y = loc.getBlockY();
                long z = loc.getBlockZ();

                if (Config.SHOP_SIZE_DEF_WIDTH % 2 == 0) {
                    xyzA[0] = x - (Config.SHOP_SIZE_DEF_WIDTH / 2);
                    xyzB[0] = x + (Config.SHOP_SIZE_DEF_WIDTH / 2);
                    xyzA[2] = z - (Config.SHOP_SIZE_DEF_WIDTH / 2);
                    xyzB[2] = z + (Config.SHOP_SIZE_DEF_WIDTH / 2);
                } else {
                    xyzA[0] = x - (Config.SHOP_SIZE_DEF_WIDTH / 2) + 1;
                    xyzB[0] = x + (Config.SHOP_SIZE_DEF_WIDTH / 2);
                    xyzA[2] = z - (Config.SHOP_SIZE_DEF_WIDTH / 2) + 1;
                    xyzB[2] = z + (Config.SHOP_SIZE_DEF_WIDTH / 2);
                }

                xyzA[1] = y - 1;
                xyzB[1] = y + Config.SHOP_SIZE_DEF_HEIGHT - 1;
            }

            if(!shopPositionOk(xyzA, xyzB, world)) {
                sender.sendMessage("A shop already exists here!");
                return false;
            }

            if (Config.SHOP_CHARGE_CREATE) {
                if (!canUseCommand(CommandTypes.CREATE_FREE)) {
                    if (!plugin.getPlayerData().get(player.getName()).chargePlayer(player.getName(), Config.SHOP_CHARGE_CREATE_COST)) {
                        sender.sendMessage(LocalShops.CHAT_PREFIX + ChatColor.DARK_AQUA + "You need " + plugin.getEconManager().format(Config.SHOP_CHARGE_CREATE_COST) + " to create a shop.");
                        return false;
                    }
                }
            }

        } else {
            sender.sendMessage("Console is not implemented yet.");
            return false;
        }

        // Command matching     

        Pattern pattern = Pattern.compile("(?i)create\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group(1);

            Shop shop = new Shop(UUID.randomUUID());
            shop.setCreator(creator);
            shop.setOwner(creator);
            shop.setName(name);
            shop.setWorld(world);
            shop.setLocations(new ShopLocation(xyzA), new ShopLocation(xyzB));

            // insert the shop into the world
            LocalShops.getCuboidTree().insert(shop.getCuboid());
            log.info(String.format("[%s] Created: %s", plugin.pdfFile.getName(), shop.toString()));
            plugin.getShopData().addShop(shop);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                plugin.playerListener.checkPlayerPosition(player);
            }

            // Disable selecting for player (if player)
            if(sender instanceof Player) {
                Player player = (Player) sender;
                plugin.getPlayerData().get(player.getName()).setSelecting(false);
            }

            // write the file
            if (plugin.getShopData().saveShop(shop)) {
                sender.sendMessage(LocalShops.CHAT_PREFIX + ChatColor.WHITE + shop.getName() + ChatColor.DARK_AQUA + " was created successfully.");
                return true;
            } else {
                sender.sendMessage(LocalShops.CHAT_PREFIX + ChatColor.DARK_AQUA + "There was an error, could not create shop.");
                return false;
            }
        }

        // Show usage
        sender.sendMessage(ChatColor.WHITE + "   /" + commandLabel + " create [ShopName]" + ChatColor.DARK_AQUA + " - Create a shop at your location.");
        return true;
    }
}