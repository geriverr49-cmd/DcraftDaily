package com.example.daily;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DcraftDaily extends JavaPlugin implements CommandExecutor, Listener {

    private Economy econ = null;
    private File dataFile;
    private YamlConfiguration dataYml;

    private final long BEKLEME_SURESI = 24 * 60 * 60 * 1000L; // 24 Saat

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataYml = YamlConfiguration.loadConfiguration(dataFile);

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) econ = rsp.getProvider();
        }

        if (getCommand("odul") != null) getCommand("odul").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DcraftDaily (Gunluk Odul) aktif!");
    }

    @Override
    public void onDisable() {
        try { dataYml.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        menuAc(p);
        return true;
    }

    private void menuAc(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, "§8§l» §6§lGÜNLÜK ÖDÜL §8§l«");
        
        ItemStack cam = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta cm = cam.getItemMeta(); if (cm != null) { cm.setDisplayName(" "); cam.setItemMeta(cm); }
        for (int i = 0; i < 45; i++) inv.setItem(i, cam);

        long sonAlim = dataYml.getLong(p.getUniqueId() + ".sonAlim", 0);
        int seri = dataYml.getInt(p.getUniqueId() + ".seri", 0);

        long gecenSure = System.currentTimeMillis() - sonAlim;
        boolean alabilir = gecenSure >= BEKLEME_SURESI;

        if (gecenSure >= (BEKLEME_SURESI * 2) && sonAlim != 0) {
            seri = 0;
            dataYml.set(p.getUniqueId() + ".seri", 0);
        }

        for (int gun = 1; gun <= 7; gun++) {
            ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK); // Kasa anahtarı görünümü için TRIPWIRE_HOOK (İp Kancası) kullandık
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName("§e§l" + gun + ". GÜN ÖDÜLÜ");
                
                List<String> lore = new ArrayList<>();
                lore.add("§8§m-----------------------");
                
                int paraOdul = gun * 2500;
                lore.add("§7Ödüller:");
                lore.add(" §a• " + paraOdul + " Para");
                
                // 7. GÜN ÖDÜLÜ (Sadece Kasa Anahtarı)
                if (gun == 7) {
                    lore.add(" §d• 3x Efsanevi Kasa Anahtarı");
                } else {
                    lore.add(" §d• 1x Efsanevi Kasa Anahtarı");
                }
                
                lore.add("§8§m-----------------------");

                if (seri >= gun) {
                    meta.setDisplayName("§c§l" + gun + ". GÜN (Alındı)");
                    lore.add("§cBu ödülü zaten aldın.");
                    item = new ItemStack(Material.BARRIER);
                } else if (seri == (gun - 1)) {
                    if (alabilir) {
                        meta.setDisplayName("§a§l" + gun + ". GÜN (TIKLA VE AL)");
                        lore.add("§aÖdülünü almak için tıkla!");
                        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        item = new ItemStack(Material.TRIPWIRE_HOOK);
                    } else {
                        meta.setDisplayName("§e§l" + gun + ". GÜN (Kilitli)");
                        lore.add("§cÖdülünü almak için beklemen gerekiyor.");
                        item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    }
                } else {
                    meta.setDisplayName("§7§l" + gun + ". GÜN (Kilitli)");
                    lore.add("§7Önceki günleri tamamlamalısın.");
                    item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(10 + (gun - 1), item);
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName("§e§lDurumun");
            List<String> lore = new ArrayList<>();
            lore.add("§7Mevcut Serin: §a" + seri + " Gün");
            if (alabilir) {
                lore.add("§aÖdülünü almaya hazırsın!");
            } else {
                long kalan = BEKLEME_SURESI - gecenSure;
                long saat = TimeUnit.MILLISECONDS.toHours(kalan);
                long dakika = TimeUnit.MILLISECONDS.toMinutes(kalan) % 60;
                lore.add("§cKalan Süre: §e" + saat + " sa " + dakika + " dk");
            }
            iMeta.setLore(lore);
            info.setItemMeta(iMeta);
        }
        inv.setItem(40, info);

        ItemStack kapat = new ItemStack(Material.BARRIER);
        ItemMeta kMeta = kapat.getItemMeta();
        if (kMeta != null) { kMeta.setDisplayName("§c§lKapat"); kapat.setItemMeta(kMeta); }
        inv.setItem(44, kapat);

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals("§8§l» §6§lGÜNLÜK ÖDÜL §8§l«")) return;
        
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        if (clicked.getType() == Material.BARRIER) {
            p.closeInventory();
            return;
        }

        if (clicked.getType() == Material.TRIPWIRE_HOOK && clicked.getItemMeta().getDisplayName().contains("TIKLA VE AL")) {
            odulVer(p);
        }
    }

    private void odulVer(Player p) {
        long sonAlim = dataYml.getLong(p.getUniqueId() + ".sonAlim", 0);
        int seri = dataYml.getInt(p.getUniqueId() + ".seri", 0);
        
        long gecenSure = System.currentTimeMillis() - sonAlim;
        if (gecenSure < BEKLEME_SURESI) {
            p.sendMessage("§cHenüz ödülünü alamazsın!");
            return;
        }

        if (gecenSure >= (BEKLEME_SURESI * 2) && sonAlim != 0) {
            seri = 0;
        }

        seri++; 
        if (seri > 7) seri = 1; 

        int paraOdul = seri * 2500;

        if (econ != null) {
            econ.depositPlayer(p, paraOdul);
        }

        // KASA ANAHTARI VERME KOMUTU
        int kasaAdet = (seri == 7) ? 3 : 1; // 7. gün 3 tane, diğer günler 1 tane
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate key give " + p.getName() + " Efsanevi " + kasaAdet);
        
        if (seri == 7) {
            p.sendMessage("§d§lBONUS: §d3 Adet Kasa Anahtarı kazandın!");
        }

        dataYml.set(p.getUniqueId() + ".sonAlim", System.currentTimeMillis());
        dataYml.set(p.getUniqueId() + ".seri", seri);
        try { dataYml.save(dataFile); } catch (IOException ignored) {}

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        p.sendTitle("§a§lÖDÜL ALINDI!", "§7" + seri + ". Gün Ödülü", 10, 50, 10);
        
        for (int i = 0; i < 30; i++) {
            p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.1);
        }

        p.sendMessage("§a§l✔ §a" + seri + ". Gün ödülünü aldın!");
        p.sendMessage("§7Kazanılan: §e" + paraOdul + " Para §7ve §d" + kasaAdet + "x Kasa Anahtarı");
        
        p.closeInventory();
    }
}
