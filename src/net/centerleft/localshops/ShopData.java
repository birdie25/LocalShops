package net.centerleft.localshops;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import cuboidLocale.BookmarkedResult;
import cuboidLocale.PrimitiveCuboid;
import cuboidLocale.QuadTree;

public class ShopData {
    private LocalShops plugin = null;
    private HashMap<String, Shop> shops;
    
    // Logging
    private final Logger log = Logger.getLogger("Minecraft");    

    long shopSize = 10;
    long shopHeight = 3;
    String currencyName = "Coin";

    long shopCost = 4000;
    long moveCost = 1000;
    boolean chargeForShop = false;
    boolean chargeForMove = false;
    boolean logTransactions = true;

    int maxDamage = 0;

    long maxWidth = 30;
    long maxHeight = 10;

    public ShopData(LocalShops plugin) {
	this.plugin = plugin;
    }

    public Shop getShop(String name) {
	return shops.get(name);
    }

    public void addShop(Shop shop) {
	shops.put(shop.getName(), shop);
    }

    public Collection<Shop> getAllShops() {
	return shops.values();
    }

    public int getNumShops() {
	return shops.size();
    }

    public void LoadShops(File shopsDir) {
	// initialize and setup the hash of shops
	shops = new HashMap<String, Shop>();
	shops.clear();

	LocalShops.cuboidTree = new QuadTree();

	String worldName = null;
	boolean defaultWorld = false;

	if (LocalShops.foundWorlds.size() == 1) {
	    worldName = LocalShops.foundWorlds.get(0).getName().toString();
	    defaultWorld = true;
	}

	if (plugin.pluginListener.useiConomy) {
	    currencyName = plugin.pluginListener.iConomy.getBank().getCurrency();
	}

	File[] shopsList = shopsDir.listFiles();
	for (File shop : shopsList) {

	    if (!shop.isFile())
		continue;
	    if (!shop.getName().contains(".shop"))
		continue;
	    // read the file and put the data away nicely
	    Shop tempShop = new Shop();
	    String text = null;
	    String[] split = null;
	    Scanner scanner;
	    PrimitiveCuboid tempShopCuboid = null;

	    String[] shopName = shop.getName().split("\\.");

	    tempShop.setName(shopName[0]);

	    log.info(String.format("[%s] Loading shop %s", plugin.pdfFile.getName(), shopName[0]));

	    // set default world just in case we're converting old files
	    // will be over-written in case the shop files are setup correctly
	    if (defaultWorld) {
		tempShop.setWorld(worldName);
	    }

	    try {
		scanner = new Scanner(new FileInputStream(shop));

		int itemType, itemData;
		int buyPrice, buyStackSize;
		int sellPrice, sellStackSize;
		int stock, maxStock;
		long[] xyzA = new long[3];
		long[] xyzB = new long[3];

		while (scanner.hasNextLine()) {
		    text = scanner.nextLine();
		    // check if the next line is empty or a comment
		    // ignore comments
		    if (!text.startsWith("#") && !text.isEmpty()) {
			split = text.split("=");
			try {

			    itemType = Integer.parseInt(split[0].split(":")[0]
									.trim());
			    String[] args = split[1].split(",");
			    // args may be in one of two formats now:
			    // buyPrice:buyStackSize sellPrice:sellStackSize
			    // stock
			    // or
			    // dataValue buyPrice:buyStackSize
			    // sellPrice:sellStackSize stock
			    if (args.length == 3) {
				String[] temp = args.clone();
				args = new String[4];
				args[0] = "0";
				args[1] = temp[0];
				args[2] = temp[1];
				args[3] = temp[2];
			    }

			    try {

				if (split[0].split(":").length == 1) {
				    itemData = Integer.parseInt(args[0]);

				    String[] buy = args[1].split(";");
				    buyPrice = Integer.parseInt(buy[0]);
				    if (buy.length == 1) {
					buyStackSize = 1;
				    } else {
					buyStackSize = Integer.parseInt(buy[1]);
				    }

				    String[] sell = args[2].split(";");
				    sellPrice = Integer.parseInt(sell[0]);
				    if (sell.length == 1) {
					sellStackSize = 1;
				    } else {
					sellStackSize = Integer
												.parseInt(sell[1]);
				    }

				    stock = Integer.parseInt(args[3]);
				    tempShop.addItem(itemType, itemData,
											buyPrice, buyStackSize, sellPrice,
											sellStackSize, stock, 0);
				} else {
				    itemData = Integer.parseInt(split[0]
											.split(":")[1]);

				    String[] buy = args[1].split(":");
				    buyPrice = Integer.parseInt(buy[0]);
				    if (buy.length == 1) {
					buyStackSize = 1;
				    } else {
					buyStackSize = Integer.parseInt(buy[1]);
				    }

				    String[] sell = args[2].split(":");
				    sellPrice = Integer.parseInt(sell[0]);
				    if (sell.length == 1) {
					sellStackSize = 1;
				    } else {
					sellStackSize = Integer
												.parseInt(sell[1]);
				    }

				    String[] stockInfo = args[3].split(":");
				    stock = Integer.parseInt(stockInfo[0]);

				    if (stockInfo.length == 1) {
					maxStock = 0;
				    } else {
					maxStock = Integer
												.parseInt(stockInfo[1]);
				    }

				    tempShop.addItem(itemType, itemData,
											buyPrice, buyStackSize, sellPrice,
											sellStackSize, stock, maxStock);
				}

			    } catch (NumberFormatException ex3) {
				System.out.println(plugin.pdfFile.getName() + ": Error - Problem with item data in " + shop.getName());
			    }

			} catch (NumberFormatException ex) {
			    // this isn't an item number, so check what property
			    // it is
			    if (split[0].equalsIgnoreCase("owner")) {
				tempShop.setOwner(split[1]);
			    } else if (split[0].equalsIgnoreCase("creator")) {
				tempShop.setCreator(split[1]);

			    } else if (split[0].equalsIgnoreCase("managers")) {
				if (split.length > 1) {
				    String[] args = split[1].split(",");
				    tempShop.setShopManagers(args);
				}
			    } else if (split[0].equalsIgnoreCase("world")) {
				tempShop.setWorld(split[1]);
			    } else if (split[0].equalsIgnoreCase("position")) {
				String[] args = split[1].split(",");

				xyzA = new long[3];
				xyzB = new long[3];
				long lx = 0;
				long ly = 0;
				long lz = 0;
				try {

				    lx = Long.parseLong(args[0].trim());
				    ly = Long.parseLong(args[1].trim());
				    lz = Long.parseLong(args[2].trim());

				    if (shopSize % 2 == 1) {
					xyzA[0] = lx - (shopSize / 2);
					xyzB[0] = lx + (shopSize / 2);
					xyzA[2] = lz - (shopSize / 2);
					xyzB[2] = lz + (shopSize / 2);
				    } else {
					xyzA[0] = lx - (shopSize / 2) + 1;
					xyzB[0] = lx + (shopSize / 2);
					xyzA[2] = lz - (shopSize / 2) + 1;
					xyzB[2] = lz + (shopSize / 2);
				    }

				    xyzA[1] = ly - 1;
				    xyzB[1] = ly + shopHeight - 1;

				    tempShopCuboid = new PrimitiveCuboid(xyzA,
											xyzB);

				} catch (NumberFormatException ex2) {

				    lx = 0;
				    ly = 0;
				    lz = 0;
				    System.out.println(plugin.pdfFile.getName() + ": Error - Problem with position data in " + shop.getName());
				}
				tempShop.setLocations(new ShopLocation(xyzA), new ShopLocation(xyzB));

			    } else if (split[0].equalsIgnoreCase("position1")) {
				String[] args = split[1].split(",");

				xyzA = new long[3];
				xyzB = new long[3];
				try {

				    xyzA[0] = Long.parseLong(args[0].trim());
				    xyzA[1] = Long.parseLong(args[1].trim());
				    xyzA[2] = Long.parseLong(args[2].trim());

				} catch (NumberFormatException ex2) {

				    xyzA[0] = 0;
				    xyzA[1] = 0;
				    xyzA[2] = 0;
				    System.out.println(plugin.pdfFile.getName() + ": Error - Problem with position1 data in " + shop.getName());
				}
				tempShop.setLocationA(new ShopLocation(xyzA));

			    } else if (split[0].equalsIgnoreCase("position2")) {
				String[] args = split[1].split(",");

				xyzA = new long[3];
				xyzB = new long[3];
				try {

				    xyzB[0] = Long.parseLong(args[0].trim());
				    xyzB[1] = Long.parseLong(args[1].trim());
				    xyzB[2] = Long.parseLong(args[2].trim());

				} catch (NumberFormatException ex2) {

				    xyzB[0] = 0;
				    xyzB[1] = 0;
				    xyzB[2] = 0;
				    System.out.println(plugin.pdfFile.getName() + ": Error - Problem with position2 data in " + shop.getName());
				}
				tempShop.setLocationB(new ShopLocation(xyzB));

			    } else if (split[0].equalsIgnoreCase("unlimited")) {
				if (split[1].equalsIgnoreCase("true")) {
				    tempShop.setUnlimitedMoney(true);
				} else {
				    tempShop.setUnlimitedMoney(false);
				}

			    } else if (split[0]
									.equalsIgnoreCase("unlimited-money")) {
				if (split[1].equalsIgnoreCase("true")) {
				    tempShop.setUnlimitedMoney(true);
				} else {
				    tempShop.setUnlimitedMoney(false);
				}

			    } else if (split[0]
									.equalsIgnoreCase("unlimited-stock")) {
				if (split[1].equalsIgnoreCase("true")) {
				    tempShop.setUnlimitedStock(true);
				} else {
				    tempShop.setUnlimitedStock(false);
				}

			    }
			}

		    }
		}

		tempShopCuboid = new PrimitiveCuboid(tempShop.getLocationA().toArray(), tempShop.getLocationB().toArray());

		tempShopCuboid.name = tempShop.getName();
		tempShopCuboid.world = tempShop.getWorld();

		if (shopPositionOk(tempShop, xyzA, xyzB)) {

		    LocalShops.cuboidTree.insert(tempShopCuboid);
		    shops.put(shopName[0], tempShop);

		    // convert to new format
		    saveShop(tempShop);
		}

	    } catch (FileNotFoundException e) {
		System.out.println(plugin.pdfFile.getName() + ": Error - Could not read file " + shop.getName());
	    }
	}

    }

    public boolean saveShop(Shop shop) {
	String filePath = LocalShops.folderPath + LocalShops.shopsPath
				+ shop.getName() + ".shop";

	File shopFile = new File(filePath);
	try {

	    shopFile.createNewFile();

	    ArrayList<String> fileOutput = new ArrayList<String>();

	    fileOutput.add("#" + shop.getName() + " shop file\n");

	    DateFormat dateFormat = new SimpleDateFormat(
					"EEE MMM dd HH:mm:ss z yyyy");
	    Date date = new Date();
	    fileOutput.add("#" + dateFormat.format(date) + "\n");

	    fileOutput.add("world=" + shop.getWorld() + "\n");
	    fileOutput.add("owner=" + shop.getOwner() + "\n");

	    String outString = "";
	    if (shop.getManagers() != null) {
		for (String manager : shop.getManagers()) {
		    outString = outString + manager + ",";
		}
	    }
	    if (outString.equalsIgnoreCase("null"))
		outString = "";

	    fileOutput.add(String.format("managers=%s\n", outString));
	    fileOutput.add(String.format("creator=%s\n", shop.getCreator()));
	    fileOutput.add(String.format("position1=%s\n", shop.getLocationA().toString()));
	    fileOutput.add(String.format("position2=%s\n", shop.getLocationB().toString()));
	    fileOutput.add(String.format("unlimited-money=%s\n", String.valueOf(shop.isUnlimitedMoney())));
	    fileOutput.add("unlimited-stock=" + String.valueOf(shop.isUnlimitedStock()) + "\n");

	    for (Item item : shop.getItems()) {
		int buyPrice = item.getBuyPrice();
		int buySize = item.getBuySize();
		int sellPrice = item.getSellPrice();
		int sellSize = item.getSellSize();
		int stock = item.getStock();
		int maxStock = item.getMaxStock();
		int[] itemInfo = LocalShops.itemList.getItemInfo(null, item.itemName());
		if (itemInfo == null)
		    continue;
		// itemId=dataValue,buyPrice:buyStackSize,sellPrice:sellStackSize,stock
		fileOutput.add(itemInfo[0] + ":" + itemInfo[1] + "=" + buyPrice
						+ ":" + buySize + "," + sellPrice + ":" + sellSize
						+ "," + stock + ":" + maxStock + "\n");
	    }

	    FileOutputStream shopFileOut = new FileOutputStream(filePath);

	    for (String line : fileOutput) {
		shopFileOut.write(line.getBytes());
	    }

	    shopFileOut.close();

	} catch (IOException e1) {
	    System.out.println(plugin.pdfFile.getName() + ": Error - Could not create file " + shopFile.getName());
	    return false;
	}
	return true;
    }

    public boolean deleteShop(Shop shop) {
	long[] xyzA = shop.getLocation();
	BookmarkedResult res = new BookmarkedResult();

	res = LocalShops.cuboidTree.relatedSearch(res.bookmark, xyzA[0],
				xyzA[1], xyzA[2]);

	// get the shop's tree node and delete it
	for (PrimitiveCuboid shopLocation : res.results) {

	    // for each shop that you find, check to see if we're already in it
	    // this should only find one shop node
	    if (shopLocation.name == null)
		continue;
	    if (!shopLocation.world.equalsIgnoreCase(shop.getWorld()))
		continue;
	    LocalShops.cuboidTree.delete(shopLocation);

	}

	// delete the file from the directory
	String filePath = LocalShops.folderPath + LocalShops.shopsPath
				+ shop.getName() + ".shop";
	File shopFile = new File(filePath);
	shopFile.delete();

	// remove shop from data structure
	String name = shop.getName();
	shops.remove(name);

	return true;
    }

    private static boolean shopPositionOk(Shop shop, long[] xyzA, long[] xyzB) {
	BookmarkedResult res = new BookmarkedResult();

	// make sure coords are in right order
	for (int i = 0; i < 3; i++) {
	    if (xyzA[i] > xyzB[i]) {
		long temp = xyzA[i];
		xyzA[i] = xyzB[i];
		xyzB[i] = temp;
	    }
	}

	// Need to test every position to account for variable shop sizes

	for (long x = xyzA[0]; x <= xyzB[0]; x++) {
	    for (long z = xyzA[2]; z <= xyzB[2]; z++) {
		for (long y = xyzA[1]; y <= xyzB[1]; y++) {
		    res = LocalShops.cuboidTree.relatedSearch(res.bookmark, x,
							y, z);
		    if (shopOverlaps(shop, res))
			return false;
		}
	    }
	}
	return true;
    }

    private static boolean shopOverlaps(Shop shop, BookmarkedResult res) {
	if (res.results.size() != 0) {
	    for (PrimitiveCuboid foundShop : res.results) {
		if (foundShop.name != null) {
		    if (foundShop.world.equalsIgnoreCase(shop.getWorld())) {
			System.out
								.println("Could not create shop, it overlaps with "
										+ foundShop.name);
			return true;
		    }
		}
	    }
	}
	return false;
    }

    public boolean logItems(String playerName, String shopName, String action, String itemName,
			int numberOfItems, int startNumberOfItems, int endNumberOfItems) {

	return logTransaciton(playerName, shopName, action, itemName, numberOfItems,
				startNumberOfItems, endNumberOfItems, 0, 0, 0);

    }

    public boolean logPayment(String playerName, String action, double moneyTransfered,
			double startingbalance, double endingbalance) {

	return logTransaciton(playerName, null, action, null, 0, 0, 0,
				moneyTransfered, startingbalance, endingbalance);
    }

    public boolean logTransaciton(String playerName, String shopName, String action, String itemName,
			int numberOfItems, int startNumberOfItems, int endNumberOfItems, double moneyTransfered,
			double startingbalance, double endingbalance) {
	if (!logTransactions)
	    return false;

	String filePath = LocalShops.folderPath + "transactions.log";

	File logFile = new File(filePath);
	try {

	    logFile.createNewFile();

	    String fileOutput = "";

	    DateFormat dateFormat = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss z");
	    Date date = new Date();
	    fileOutput += dateFormat.format(date) + ": ";
	    fileOutput += "Action: ";
	    if (action != null)
		fileOutput += action;
	    fileOutput += ": ";
	    fileOutput += "Player: ";
	    if (playerName != null)
		fileOutput += playerName;
	    fileOutput += ": ";
	    fileOutput += "Shop: ";
	    if (shopName != null)
		fileOutput += shopName;
	    fileOutput += ": ";
	    fileOutput += "Item Type: ";
	    if (itemName != null)
		fileOutput += itemName;
	    fileOutput += ": ";
	    fileOutput += "Number Transfered: ";
	    fileOutput += numberOfItems;
	    fileOutput += ": ";
	    fileOutput += "Stating Stock: ";
	    fileOutput += startNumberOfItems;
	    fileOutput += ": ";
	    fileOutput += "Ending Stock: ";
	    fileOutput += endNumberOfItems;
	    fileOutput += ": ";
	    fileOutput += "Money Transfered: ";
	    fileOutput += moneyTransfered;
	    fileOutput += ": ";
	    fileOutput += "Starting balance: ";
	    fileOutput += startingbalance;
	    fileOutput += ": ";
	    fileOutput += "Ending balance: ";
	    fileOutput += endingbalance;
	    fileOutput += ": ";
	    fileOutput += "\n";

	    FileOutputStream logFileOut = new FileOutputStream(logFile, true);
	    logFileOut.write(fileOutput.getBytes());
	    logFileOut.close();

	} catch (IOException e1) {
	    System.out.println(plugin.pdfFile.getName() + ": Error - Could not write to file " + logFile.getName());
	    return false;
	}

	return true;
    }
}
