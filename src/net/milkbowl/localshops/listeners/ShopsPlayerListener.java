package net.milkbowl.localshops.listeners;

import java.util.UUID;
import java.util.logging.Logger;

import net.milkbowl.localshops.Config;
import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.ResourceManager;
import net.milkbowl.localshops.commands.ShopCommandExecutor;
import net.milkbowl.localshops.objects.LocalShop;
import net.milkbowl.localshops.objects.PlayerData;
import net.milkbowl.localshops.objects.Shop;
import net.milkbowl.localshops.objects.ShopSign;
import net.milkbowl.localshops.util.GenericFunctions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;


/**
 * Handle events for all Player related events
 * 
 * @author Jonbas
 */
public class ShopsPlayerListener extends PlayerListener {
    private LocalShops plugin;

    // Logging
    private static final Logger log = Logger.getLogger("Minecraft");    

    public ShopsPlayerListener(LocalShops plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        String playerName = player.getName();
        if (!plugin.getPlayerData().containsKey(playerName)) {
            plugin.getPlayerData().put(playerName, new PlayerData(plugin, playerName));
        }
        Location eventBlockLoc = event.getClickedBlock().getLocation();
        LocalShop shop = plugin.getShopManager().getLocalShop(eventBlockLoc);
        //If user Right clicks a sign try to buy/sell from it.
        if (((event.getClickedBlock().getType().equals(Material.WALL_SIGN) || event.getClickedBlock().getType().equals(Material.SIGN_POST)) && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && (player.getItemInHand().getType().equals(Material.AIR) || player.getItemInHand().equals(Material.STICK)) && shop != null) {
            for (ShopSign sign : shop.getSignSet()) {
                if (sign.getLoc().equals(eventBlockLoc)) {
                    if (sign.getType().equals(ShopSign.SignType.BUY)) {
                        ShopCommandExecutor.commandTypeMap.get("buy").getCommandInstance(plugin, "buy", event.getPlayer(), "buy " + sign.getItemName(), false).process();
                        //TODO: Remove when bukkit fixes inventory updating
                        try {
                            player.updateInventory();
                        } catch (Exception e) {}
                        return;
                    } else if (sign.getType().equals(ShopSign.SignType.SELL)) {
                        ShopCommandExecutor.commandTypeMap.get("sell").getCommandInstance(plugin, "sell", event.getPlayer(), "sell " + sign.getItemName(), false).process();
                        player.updateInventory();
                        return;
                    } else {
                        //Stop the loop if it's not a Buy/Sell Sign - only 1 possible sign location match
                        break;
                    }
                }
            }

        } else if (event.getClickedBlock().getType().equals(Material.CHEST) && event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && shop != null ) {
         // Block access to chests when inside a shop, but allow the owner or managers to use them.
            if ( !shop.getManagers().contains(playerName) && !shop.getOwner().equals(playerName) && !plugin.getPermManager().hasPermission(player, "localshops.admin.local")) {
                player.sendMessage(plugin.getResourceManager().getString(ResourceManager.GEN_USER_ACCESS_DENIED));
                event.setCancelled(true);
                return;
            }
           /* if (shop.getChests().contains(eventBlockLoc)) {
            }
            */
        }
        // If our user is select & is not holding an item, selection time
        if (plugin.getPlayerData().get(playerName).isSelecting() && player.getItemInHand().getType() == Material.AIR) {
            int x, y, z;
            Location loc = event.getClickedBlock().getLocation();
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();

            PlayerData pData = plugin.getPlayerData().get(playerName);

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                String size = null;
                pData.setPositionA(loc);
                if (pData.getPositionB() != null)
                    size = GenericFunctions.calculateCuboidSize(pData.getPositionA(), pData.getPositionB(), Config.getShopSizeMaxWidth(), Config.getShopSizeMaxHeight());
                if(size != null) {
                    player.sendMessage(ChatColor.DARK_AQUA + "First Position " + ChatColor.LIGHT_PURPLE + x + " " + y + " " + z + ChatColor.DARK_AQUA + " size " + ChatColor.LIGHT_PURPLE + size);
                } else {
                    player.sendMessage(ChatColor.DARK_AQUA + "First Position " + ChatColor.LIGHT_PURPLE + x + " " + y + " " + z);
                }

                if(pData.getPositionA() != null && pData.getPositionB() == null) {
                    player.sendMessage(ChatColor.DARK_AQUA + "Now, right click to select the far upper corner for the shop.");
                } else if(pData.getPositionA() != null && pData.getPositionB() != null) {
                    player.sendMessage(ChatColor.DARK_AQUA + "Type " + ChatColor.WHITE + "/shop create [Shop Name]" + ChatColor.DARK_AQUA + ", if you're happy with your selection, otherwise keep selecting!");
                }
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pData.setPositionB(loc);
                String size = GenericFunctions.calculateCuboidSize(pData.getPositionA(), pData.getPositionB(), Config.getShopSizeMaxWidth(), Config.getShopSizeMaxHeight());
                if(size != null) {
                    player.sendMessage(ChatColor.DARK_AQUA + "Second Position " + ChatColor.LIGHT_PURPLE + x + " " + y + " " + z + ChatColor.DARK_AQUA + " size " + ChatColor.LIGHT_PURPLE + size);
                } else {
                    player.sendMessage(ChatColor.DARK_AQUA + "Second Position " + ChatColor.LIGHT_PURPLE + x + " " + y + " " + z);
                }

                if(pData.getPositionB() != null && pData.getPositionA() == null) {
                    player.sendMessage(ChatColor.DARK_AQUA + "Now, left click to select the bottom corner for a shop.");
                } else if(pData.getPositionA() != null && pData.getPositionB() != null) {
                    player.sendMessage(ChatColor.DARK_AQUA + "Type " + ChatColor.WHITE + "/shop create [Shop Name]" + ChatColor.DARK_AQUA + ", if you're happy with your selection, otherwise keep selecting!");
                }
            }
        }

    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (!plugin.getPlayerData().containsKey(playerName)) {
            plugin.getPlayerData().put(playerName, new PlayerData(plugin, playerName));
        }

        int x, y, z;
        Location xyz = player.getLocation();
        x = xyz.getBlockX();
        y = xyz.getBlockY();
        z = xyz.getBlockZ();

        checkPlayerPosition(player, x, y, z);        
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (!plugin.getPlayerData().containsKey(playerName)) {
            plugin.getPlayerData().remove(playerName);
        }
    }

    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (!plugin.getPlayerData().containsKey(playerName)) {
            plugin.getPlayerData().remove(playerName);
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (!plugin.getPlayerData().containsKey(playerName)) {
            plugin.getPlayerData().put(playerName, new PlayerData(plugin, playerName));
        }

        checkPlayerPosition(player, event.getTo());
    }

    public void checkPlayerPosition(Player player) {
        checkPlayerPosition(player, player.getLocation());
    }

    public void checkPlayerPosition(Player player, int[] xyz) {
        if (xyz.length == 3) {
            checkPlayerPosition(player, xyz[0], xyz[1], xyz[2]);
        } else {
            log.info(String.format("[%s] Bad Position", plugin.getDescription().getName()));
        }

    }

    public void checkPlayerPosition(Player player, int x, int y, int z) {
        PlayerData pData = plugin.getPlayerData().get(player.getName());

        Shop shop = plugin.getShopManager().getLocalShop(player.getWorld().getName(), x, y, z);

        if(shop == null) {
            // not in a shop...
            for(UUID uuid : pData.shopList) {
                notifyPlayerLeftShop(player, uuid);
            }
            pData.shopList.clear();
            return;
        }

        if(!pData.shopList.contains(shop.getUuid())) {
            // Player was not in the shop, and now is...
            pData.shopList.add(shop.getUuid());
            notifyPlayerEnterShop(player, shop.getUuid());
        }
    }

    public void checkPlayerPosition(Player player, Location loc) {
        checkPlayerPosition(player, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private void notifyPlayerLeftShop(Player player, UUID shopUuid) {
        // TODO Add formatting
        Shop shop = plugin.getShopManager().getLocalShop(shopUuid);
        player.sendMessage(ChatColor.DARK_AQUA + "[" + ChatColor.WHITE + "Shop" + ChatColor.DARK_AQUA + "] You have left the shop " + ChatColor.WHITE + shop.getName());
    }

    private void notifyPlayerEnterShop(Player player, UUID shopUuid) {
        // TODO Add formatting
        Shop shop = plugin.getShopManager().getLocalShop(shopUuid);
        player.sendMessage(ChatColor.DARK_AQUA + "[" + ChatColor.WHITE + "Shop" + ChatColor.DARK_AQUA
                + "] You have entered the shop " + ChatColor.WHITE + shop.getName());

    }

}
