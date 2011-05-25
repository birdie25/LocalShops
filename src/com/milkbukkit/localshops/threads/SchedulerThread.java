package com.milkbukkit.localshops.threads;

import java.util.logging.Logger;

import com.milkbukkit.localshops.LocalShops;

public class SchedulerThread extends Thread {
    protected final Logger log = Logger.getLogger("Minecraft");
    private int interval = 5;
    private boolean run = true;
    private static final int TICKS_PER_SECOND = 20;
    private ThreadGroup dynamicThreadGroup = new ThreadGroup("dynamic");
    
    private LocalShops plugin = null;
    
    public SchedulerThread(LocalShops plugin) {
        this.plugin = plugin;
    }
    
    public void setRun(boolean run) {
        this.run = run;
    }
    
    public void run() {
        while(true) {
            if(!run) {
                break;
            }
            
            long worldTime = plugin.getServer().getWorlds().get(0).getTime();
            
            // Launch Dynamic Thread (5pm)
            if(worldTime >= 9000 && worldTime < (9000 + (interval * TICKS_PER_SECOND))) {
                if(dynamicThreadGroup.activeCount() == 0) {
                    log.info("Launch Dynamic Thread");
                    DynamicThread dt = new DynamicThread(dynamicThreadGroup, "dynamic", plugin);
                    dt.start();
                } else {
                    log.info("Dynamic Thread already running!");
                }
            }
         
            for(int i = 0; i < interval; i++) {
                if(!run) {
                    break;
                }
                
                try{
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    // care
                }
            }
            
        }
        
        log.info(String.format("[%s] SchedulerThread exited safely.", plugin.getDescription().getName()));
    }
    
}