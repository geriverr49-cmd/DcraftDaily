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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DcraftDaily extends JavaPlugin implements CommandExecutor, Listener {

    private Economy econ = null;
    private Object ppAPI = null;
    private File dataFile;
    private YamlConfiguration dataYml;

    // 24 Saat (Milisaniye cinsinden)
    private final long BEKLEME_SURESI = 24 * 60 * 60 * 1000L;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataYml = YamlConfiguration.loadConfiguration(dataFile);

        // Vault Bağlantısı
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) econ = rsp.getProvider();
        }

        // PlayerPoints Bağlantısı (Reflection)
        Plugin ppPlugin = getServer().getPluginManager().getPlugin("PlayerPoints");
        if (ppPlugin != null && ppPlugin.isEnabled()) {
            try { ppAPI = ppPlugin.getClass().getMethod("getAPI").invoke(ppPlugin); } catch (Exception ignored) {}
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

        // Oyuncu Verileri
        long sonAlim = dataYml.getLong(p.getUniqueId() + ".sonAlim", 0);
        int seri = dataYml.getInt(p.getUniqueId() + ".seri", 0);

        long gecenSure = System.currentTimeMillis() - sonAlim;
        boolean alabilir = gecenSure >= BEKLEME_SURESI;

        // Eğer 2 günden fazla girmemişse seriyi sıfırla
        if (gecenSure >= (BEKLEME_SURESI * 2) && sonAlim != 0) {
            seri = 0;
            dataYml.set(p.getUniqueId() + ".seri", 0);
        }

        // 7 Günlük Ödül Dizilimi
        for (int gun = 1; gun <= 7; gun++) {
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName("§e§l" + gun + ". GÜN ÖDÜLÜ");
                
                List<String> lore = new ArrayList<>();
                lore.add("§8§m-----------------------");
                
                // Ödülleri belirle (Burayı istediğin gibi değiştirebilirsin)
                int paraOdul = gun * 1000;
                int puanOdul = gun * 10;
                lore.add("§7Ödüller:");
                lore.add(" §a• " + paraOdul + " Para");
                lore.add(" §b• " + puanOdul + " Dcraft Puanı");
                if (gun == 7) lore.add(" §d• 1x Özel Kasa Anahtarı");
                
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
                        item = new ItemStack(Material.CHEST);
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

        // Alt Kısım Bilgi
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

        // Kapat
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

        // Ödül Alma Butonu (Sandık)
        if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().contains("TIKLA VE AL")) {
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

        // 2 günden fazla girmişse seriyi 0'la
        if (gecenSure >= (BEKLEME_SURESI * 2) && sonAlim != 0) {
            seri = 0;
        }

        seri++; // Seriyi artır
        if (seri > 7) seri = 1; // 7'den sonra başa döner

        // Ödülleri Hesapla
        int paraOdul = seri * 1000;
        int puanOdul = seri * 10;

        // Para Ver
        if (econ != null) {
            econ.depositPlayer(p, paraOdul);
        }

        // Puan Ver
        if (ppAPI != null) {
            try {
                ppAPI.getClass().getMethod("give", UUID.class, int.class).invoke(ppAPI, p.getUniqueId(), puanOdul);
            } catch (Exception ignored) {}
        }

        // 7. Gün Özel Ödül
        if (seri == 7) {
            p.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));
            p.sendMessage("§d§lBONUS: §d16 Elmas kazandın!");
        }

        // Kaydet
        dataYml.set(p.getUniqueId() + ".sonAlim", System.currentTimeMillis());
        dataYml.set(p.getUniqueId() + ".seri", seri);
        try { dataYml.save(dataFile); } catch (IOException ignored) {}

        // Efektler
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        p.sendTitle("§a§lÖDÜL ALINDI!", "§7" + seri + ". Gün Ödülü", 10, 50, 10);
        
        for (int i = 0; i < 30; i++) {
            p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.1);
        }

        p.sendMessage("§a§l✔ §a" + seri + ". Gün ödülünü aldın!");
        p.sendMessage("§7Kazanılan: §e" + paraOdul + " Para §7ve §b" + puanOdul + " Puan");
        
        p.closeInventory();
    }
                                    }
