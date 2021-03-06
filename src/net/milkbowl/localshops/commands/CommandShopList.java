package net.milkbowl.localshops.commands;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.localshops.Config;
import net.milkbowl.localshops.LocalShops;
import net.milkbowl.localshops.comparator.ShopSortByName;
import net.milkbowl.localshops.objects.GlobalShop;
import net.milkbowl.localshops.objects.LocalShop;
import net.milkbowl.localshops.objects.Shop;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class CommandShopList extends Command {

    public CommandShopList(LocalShops plugin, String commandLabel, CommandSender sender, String command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command, isGlobal);
    }
    
    public CommandShopList(LocalShops plugin, String commandLabel, CommandSender sender, String[] command, boolean isGlobal) {
        super(plugin, commandLabel, sender, command, isGlobal);
    }

    public boolean process() {
        int idWidth = Config.getUuidMinLength() + 1;
        if(idWidth < 4) {
            idWidth = 4;
        }

        boolean showAll = false;
        boolean isPlayer = false;

        // list all
        Pattern pattern = Pattern.compile("(?i)list\\s+(.*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            showAll = true;
        }        

        if(sender instanceof Player) {
            isPlayer = true;
        }

        if(isPlayer) {
            sender.sendMessage(String.format("%-"+idWidth+"s  %s", "Id", "Name"));
        } else {
            sender.sendMessage(String.format("%-"+idWidth+"s  %-25s %s", "Id", "Name", "Owner"));
        }
        
        List<Shop> shops = plugin.getShopManager().getAllShops();
        Collections.sort(shops, new ShopSortByName());
        
        //What is this here for?
        if(isGlobal && !canUseCommand(CommandTypes.ADMIN_GLOBAL)) {
            // send nice message
        }
        
        Iterator<Shop> it = shops.iterator();
        while(it.hasNext()) {
            Shop shop = it.next();
            if (isGlobal) {
                if(!(shop instanceof GlobalShop)) {
                    continue;
                }
            } else {
                if(!(shop instanceof LocalShop)) {
                    continue;
                }
                
                if (!showAll && isPlayer && !isShopController(shop)) {
                    continue;
                }
            }
            
            if(isPlayer) {
                if(shop instanceof GlobalShop) {
                    if(((GlobalShop) shop).getWorlds().size() == 0) {
                        sender.sendMessage(String.format("%-"+idWidth+"s  %s *", shop.getShortUuidString(), shop.getName()));
                    } else {
                        sender.sendMessage(String.format("%-"+idWidth+"s  %s", shop.getShortUuidString(), shop.getName()));
                    }
                } else {
                    sender.sendMessage(String.format("%-" + idWidth + "s  %s", shop.getShortUuidString(), shop.getName()));
                }
            } else {
                if(shop instanceof GlobalShop) {
                    if(((GlobalShop) shop).getWorlds().size() == 0) {
                        sender.sendMessage(String.format("%-"+idWidth+"s  %-25s %s *", shop.getShortUuidString(), shop.getName(), shop.getOwner()));
                    } else {
                        sender.sendMessage(String.format("%-"+idWidth+"s  %-25s %s", shop.getShortUuidString(), shop.getName(), shop.getOwner()));
                    }
                } else {
                    sender.sendMessage(String.format("%-"+idWidth+"s  %-25s %s", shop.getShortUuidString(), shop.getName(), shop.getOwner()));
                }
            }
        }
        return true;
    }
}
