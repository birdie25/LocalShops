package net.milkbowl.localshops.modules.economy;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;


import net.milkbowl.localshops.modules.economy.plugins.*;
import net.milkbowl.localshops.objects.Shop;

import org.bukkit.plugin.Plugin;


public class EconomyManager {
    
    private Plugin plugin = null;
    private TreeMap<Integer,Economy> econs = new TreeMap<Integer,Economy>();
    private Economy activeEconomy = null;
    private static final Logger log = Logger.getLogger("Minecraft");

    public EconomyManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean loadEconomies() {
        // Try to load BOSEconomy
        if (packageExists(new String[] { "cosine.boseconomy.BOSEconomy" })) {
            Economy bose = new Economy_BOSE(plugin);
            econs.put(10, bose);
            log.info(String.format("[%s][Economy] BOSEconomy found: %s", plugin.getDescription().getName(), bose.isEnabled() ? "Loaded" : "Waiting"));
        } else {
            log.info(String.format("[%s][Economy] BOSEconomy not found.", plugin.getDescription().getName()));
        }

        // Try to load Essentials Economy
        if (packageExists(new String[] { "com.earth2me.essentials.api.Economy", "com.earth2me.essentials.api.NoLoanPermittedException", "com.earth2me.essentials.api.UserDoesNotExistException" })) {
            Economy essentials = new Economy_Essentials(plugin);
            econs.put(9, essentials);
            log.info(String.format("[%s][Economy] Essentials Economy found: %s", plugin.getDescription().getName(), essentials.isEnabled() ? "Loaded" : "Waiting"));
        } else {
            log.info(String.format("[%s][Economy] Essentials Economy not found.", plugin.getDescription().getName()));
        }

        // Try to load iConomy 4
        if (packageExists(new String[] { "com.nijiko.coelho.iConomy.iConomy", "com.nijiko.coelho.iConomy.system.Account" })) {
            Economy icon4 = new Economy_iConomy4(plugin);
            econs.put(8, icon4);
            log.info(String.format("[%s][Economy] iConomy 4 found: ", plugin.getDescription().getName(), icon4.isEnabled() ? "Loaded" : "Waiting"));
        } else {
            log.info(String.format("[%s][Economy] iConomy 4 not found.", plugin.getDescription().getName()));
        }

        // Try to load iConomy 5
        if (packageExists(new String[] { "com.iConomy.iConomy", "com.iConomy.system.Account", "com.iConomy.system.Holdings" })) {
        Economy icon5 = new Economy_iConomy5(plugin);
        econs.put(7, icon5);
        log.info(String.format("[%s][Economy] iConomy 5 found: %s", plugin.getDescription().getName(), icon5.isEnabled() ? "Loaded" : "Waiting"));
        } else {
            log.info(String.format("[%s][Economy] iConomy 5 not found.", plugin.getDescription().getName()));
        }
        
        if(econs.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean packageExists(String[] packages) {
        try {
            for (String pkg : packages) {
                Class.forName(pkg);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private Economy getEconomy() {
        if(activeEconomy == null) {
            Iterator<Economy> it = econs.values().iterator();
            while(it.hasNext()) {
                Economy e = it.next();
                if(e.isEnabled()) {
                    return e;
                }
            }
            return null;
        } else {
            return activeEconomy;
        }
    }
    
    public String getName() {
        return getEconomy().getName();
    }
    
    public String format(double amount) {
        return getEconomy().format(amount);
    }
    
    public EconomyResponse getBalance(String playerName) {
        return getEconomy().getBalance(playerName);
    }
    
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return getEconomy().withdrawPlayer(playerName, amount);
    }
    
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return getEconomy().depositPlayer(playerName, amount);
    }
    
    public EconomyResponse withdrawShop(Shop shop, double amount) {
        return getEconomy().withdrawShop(shop, amount);
    }
    
    public EconomyResponse depositShop(Shop shop, double amount) {
        return getEconomy().depositShop(shop, amount);
    }
}