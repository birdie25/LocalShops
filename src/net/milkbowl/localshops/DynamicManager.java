/**
 * 
 */
package net.milkbowl.localshops;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.milkbowl.localshops.objects.ItemInfo;


/**
 * @author sleaker
 *
 */
public class DynamicManager {
    @SuppressWarnings("unused")
    private LocalShops plugin = null;
    private static Map<ItemInfo, Double> priceAdjMap = Collections.synchronizedMap(new HashMap<ItemInfo, Double>());
        
    public DynamicManager(LocalShops plugin) {
        this.plugin = plugin;
    }

    public static Map<ItemInfo, Double> getPriceAdjMap() {
        return priceAdjMap;
    } 

}
