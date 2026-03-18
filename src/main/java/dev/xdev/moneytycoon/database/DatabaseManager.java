package dev.xdev.moneytycoon.database;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.Tycoon;
import dev.xdev.moneytycoon.model.TycoonTheme;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final MoneyTycoon plugin;
    private Connection connection;

    public DatabaseManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws SQLException {
        String type = plugin.getConfig().getString("database.type", "SQLITE");
        if (type.equalsIgnoreCase("MYSQL")) {
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String db = plugin.getConfig().getString("database.mysql.database", "moneytycoon");
            String user = plugin.getConfig().getString("database.mysql.username", "root");
            String pass = plugin.getConfig().getString("database.mysql.password", "");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + db + "?autoReconnect=true", user, pass);
        } else {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
        createTables();
        migrateTycoons();
        plugin.getLogger().info("Database connection established (" + type + ")");
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tycoons (
                    uuid TEXT PRIMARY KEY,
                    owner_name TEXT NOT NULL,
                    base_world TEXT NOT NULL,
                    base_x REAL NOT NULL, base_y REAL NOT NULL, base_z REAL NOT NULL,
                    grid_index INTEGER NOT NULL,
                    balance REAL DEFAULT 0,
                    dropper_type TEXT DEFAULT 'COBBLESTONE',
                    prestige_level INTEGER DEFAULT 0,
                    rebirth_level INTEGER DEFAULT 0,
                    theme TEXT DEFAULT 'CLASSIC',
                    total_earned INTEGER DEFAULT 0,
                    items_sold INTEGER DEFAULT 0,
                    active_pet TEXT,
                    created_at INTEGER DEFAULT 0,
                    booster_multiplier REAL DEFAULT 1.0,
                    booster_end_time INTEGER DEFAULT 0,
                    last_milestone INTEGER DEFAULT 0
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tycoon_upgrades (
                    uuid TEXT NOT NULL,
                    upgrade_id TEXT NOT NULL,
                    level INTEGER DEFAULT 0,
                    PRIMARY KEY (uuid, upgrade_id)
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tycoon_coop (
                    owner_uuid TEXT NOT NULL,
                    member_uuid TEXT NOT NULL,
                    member_name TEXT NOT NULL,
                    PRIMARY KEY (owner_uuid, member_uuid)
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tycoon_pets (
                    uuid TEXT NOT NULL,
                    pet_id TEXT NOT NULL,
                    PRIMARY KEY (uuid, pet_id)
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quest_progress (
                    uuid TEXT NOT NULL,
                    quest_id TEXT NOT NULL,
                    progress INTEGER DEFAULT 0,
                    completed INTEGER DEFAULT 0,
                    claimed INTEGER DEFAULT 0,
                    reset_time INTEGER DEFAULT 0,
                    PRIMARY KEY (uuid, quest_id)
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS trade_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player1_uuid TEXT NOT NULL, player2_uuid TEXT NOT NULL,
                    money1 REAL DEFAULT 0, money2 REAL DEFAULT 0,
                    pet1 TEXT, pet2 TEXT,
                    timestamp INTEGER NOT NULL
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS leaderboard (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    total_earned INTEGER DEFAULT 0,
                    prestige_level INTEGER DEFAULT 0,
                    rebirth_level INTEGER DEFAULT 0
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY,
                    last_login INTEGER DEFAULT 0,
                    login_streak INTEGER DEFAULT 0,
                    last_offline INTEGER DEFAULT 0
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_settings (
                    uuid TEXT PRIMARY KEY,
                    sounds INTEGER DEFAULT 1,
                    trade_requests INTEGER DEFAULT 1,
                    visits_allowed INTEGER DEFAULT 1,
                    actionbar INTEGER DEFAULT 1,
                    bossbar INTEGER DEFAULT 1,
                    notifications INTEGER DEFAULT 1
                )""");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tycoon_conveyor_items (
                    uuid TEXT NOT NULL,
                    material TEXT NOT NULL,
                    amount INTEGER DEFAULT 1,
                    conveyor_index INTEGER DEFAULT 0
                )""");
        }
    }

    private void migrateTycoons() {
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(tycoons)")) {
                boolean hasIslandType = false;
                while (rs.next()) {
                    if ("island_type".equals(rs.getString("name"))) { hasIslandType = true; break; }
                }
                if (!hasIslandType) {
                    stmt.executeUpdate("ALTER TABLE tycoons ADD COLUMN island_type TEXT DEFAULT 'DEFAULT'");
                    plugin.getLogger().info("Migrated tycoons: added island_type column");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Migration check failed: " + e.getMessage());
        }
    }

    public int[] getLoginData(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_login, login_streak FROM player_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new int[] { rs.getInt("last_login"), rs.getInt("login_streak") };
            }
        } catch (SQLException ignored) {}
        return new int[] { 0, 0 };
    }

    public void saveLoginData(UUID uuid, long lastLogin, int streak) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO player_data (uuid, last_login, login_streak) VALUES (?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, lastLogin);
            ps.setInt(3, streak);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public void setLastOffline(UUID uuid, long time) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET last_offline = ? WHERE uuid = ?")) {
            ps.setLong(1, time);
            ps.setString(2, uuid.toString());
            if (ps.executeUpdate() == 0) {
                try (PreparedStatement ins = connection.prepareStatement(
                        "INSERT INTO player_data (uuid, last_offline) VALUES (?,?)")) {
                    ins.setString(1, uuid.toString());
                    ins.setLong(2, time);
                    ins.executeUpdate();
                }
            }
        } catch (SQLException ignored) {}
    }

    public long getLastOffline(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_offline FROM player_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("last_offline");
            }
        } catch (SQLException ignored) {}
        return 0;
    }

    public int getSetting(UUID uuid, String key, int defaultValue) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT " + key + " FROM player_settings WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(key);
            }
        } catch (SQLException ignored) {}
        return defaultValue;
    }

    public void setSetting(UUID uuid, String key, int value) {
        int s = getSetting(uuid, "sounds", 1);
        int t = getSetting(uuid, "trade_requests", 1);
        int v = getSetting(uuid, "visits_allowed", 1);
        int a = getSetting(uuid, "actionbar", 1);
        int b = getSetting(uuid, "bossbar", 1);
        int n = getSetting(uuid, "notifications", 1);
        switch (key) {
            case "sounds" -> s = value;
            case "trade_requests" -> t = value;
            case "visits_allowed" -> v = value;
            case "actionbar" -> a = value;
            case "bossbar" -> b = value;
            case "notifications" -> n = value;
        }
        setSettings(uuid, s, t, v, a, b, n);
    }

    public void setSettings(UUID uuid, int sounds, int tradeRequests, int visitsAllowed,
                           int actionbar, int bossbar, int notifications) {
        String u = uuid.toString();
        try (PreparedStatement upd = connection.prepareStatement("""
                UPDATE player_settings SET sounds=?, trade_requests=?, visits_allowed=?, actionbar=?, bossbar=?, notifications=?
                WHERE uuid=?""")) {
            upd.setInt(1, sounds);
            upd.setInt(2, tradeRequests);
            upd.setInt(3, visitsAllowed);
            upd.setInt(4, actionbar);
            upd.setInt(5, bossbar);
            upd.setInt(6, notifications);
            upd.setString(7, u);
            if (upd.executeUpdate() == 0) {
                try (PreparedStatement ins = connection.prepareStatement("""
                        INSERT INTO player_settings (uuid, sounds, trade_requests, visits_allowed, actionbar, bossbar, notifications)
                        VALUES (?,?,?,?,?,?,?)""")) {
                    ins.setString(1, u);
                    ins.setInt(2, sounds);
                    ins.setInt(3, tradeRequests);
                    ins.setInt(4, visitsAllowed);
                    ins.setInt(5, actionbar);
                    ins.setInt(6, bossbar);
                    ins.setInt(7, notifications);
                    ins.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save settings: " + e.getMessage());
        }
    }

    public void saveConveyorItems(UUID uuid, java.util.List<Object[]> items) {
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM tycoon_conveyor_items WHERE uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to clear conveyor items: " + e.getMessage());
            return;
        }
        if (items.isEmpty()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO tycoon_conveyor_items (uuid, material, amount, conveyor_index) VALUES (?,?,?,?)")) {
            for (Object[] row : items) {
                ps.setString(1, uuid.toString());
                ps.setString(2, (String) row[0]);
                ps.setInt(3, (Integer) row[1]);
                ps.setInt(4, (Integer) row[2]);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save conveyor items: " + e.getMessage());
        }
    }

    public java.util.List<Object[]> loadConveyorItems(UUID uuid) {
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT material, amount, conveyor_index FROM tycoon_conveyor_items WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new Object[]{rs.getString("material"), rs.getInt("amount"), rs.getInt("conveyor_index")});
            }
        } catch (SQLException ignored) {}
        return list;
    }

    public List<Tycoon> loadAllTycoons() {
        List<Tycoon> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM tycoons");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Tycoon t = parseTycoon(rs);
                if (t != null) {
                    loadUpgrades(t);
                    loadCoopMembers(t);
                    loadOwnedPets(t);
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load tycoon data: " + e.getMessage());
        }
        return list;
    }

    private Tycoon parseTycoon(ResultSet rs) throws SQLException {
        String worldName = rs.getString("base_world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        UUID uuid = UUID.fromString(rs.getString("uuid"));
        Location base = new Location(world, rs.getDouble("base_x"),
                rs.getDouble("base_y"), rs.getDouble("base_z"));

        Tycoon t = new Tycoon(uuid, rs.getString("owner_name"), base, rs.getInt("grid_index"));
        t.setBalance(rs.getDouble("balance"));
        t.setCurrentDropperType(Material.valueOf(rs.getString("dropper_type")));
        t.setPrestigeLevel(rs.getInt("prestige_level"));
        t.setRebirthLevel(rs.getInt("rebirth_level"));
        try { t.setTheme(TycoonTheme.valueOf(rs.getString("theme"))); }
        catch (Exception ignored) { t.setTheme(TycoonTheme.CLASSIC); }
        try { t.setIslandType(rs.getString("island_type")); } catch (Exception ignored) { t.setIslandType("DEFAULT"); }
        t.setTotalEarned(rs.getLong("total_earned"));
        t.setItemsSold(rs.getLong("items_sold"));
        t.setActivePetId(rs.getString("active_pet"));
        t.setBoosterMultiplier(rs.getDouble("booster_multiplier"));
        t.setBoosterEndTime(rs.getLong("booster_end_time"));
        t.setLastMilestone(rs.getLong("last_milestone"));
        return t;
    }

    private void loadUpgrades(Tycoon t) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT upgrade_id, level FROM tycoon_upgrades WHERE uuid = ?")) {
            ps.setString(1, t.getOwnerUUID().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) t.setUpgradeLevel(rs.getString("upgrade_id"), rs.getInt("level"));
            }
        }
    }

    private void loadCoopMembers(Tycoon t) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT member_uuid FROM tycoon_coop WHERE owner_uuid = ?")) {
            ps.setString(1, t.getOwnerUUID().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) t.addCoopMember(UUID.fromString(rs.getString("member_uuid")));
            }
        }
    }

    private void loadOwnedPets(Tycoon t) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT pet_id FROM tycoon_pets WHERE uuid = ?")) {
            ps.setString(1, t.getOwnerUUID().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) t.getOwnedPets().add(rs.getString("pet_id"));
            }
        }
    }

    public void saveTycoon(Tycoon t) {
        String uuid = t.getOwnerUUID().toString();
        try {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT OR REPLACE INTO tycoons
                (uuid, owner_name, base_world, base_x, base_y, base_z, grid_index,
                 balance, dropper_type, prestige_level, rebirth_level, theme, island_type,
                 total_earned, items_sold, active_pet, created_at,
                 booster_multiplier, booster_end_time, last_milestone)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""")) {
                Location b = t.getBaseLocation();
                ps.setString(1, uuid);
                ps.setString(2, t.getOwnerName());
                ps.setString(3, b.getWorld().getName());
                ps.setDouble(4, b.getX()); ps.setDouble(5, b.getY()); ps.setDouble(6, b.getZ());
                ps.setInt(7, t.getGridIndex());
                ps.setDouble(8, t.getBalance());
                ps.setString(9, t.getCurrentDropperType().name());
                ps.setInt(10, t.getPrestigeLevel());
                ps.setInt(11, t.getRebirthLevel());
                ps.setString(12, t.getTheme().name());
                ps.setString(13, t.getIslandType());
                ps.setLong(14, t.getTotalEarned());
                ps.setLong(15, t.getItemsSold());
                ps.setString(16, t.getActivePetId());
                ps.setLong(17, System.currentTimeMillis());
                ps.setDouble(18, t.getBoosterMultiplier());
                ps.setLong(19, t.getBoosterEndTime());
                ps.setLong(20, t.getLastMilestone());
                ps.executeUpdate();
            }

            try (PreparedStatement del = connection.prepareStatement(
                    "DELETE FROM tycoon_upgrades WHERE uuid = ?")) {
                del.setString(1, uuid); del.executeUpdate();
            }
            for (var entry : t.getUpgrades().entrySet()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO tycoon_upgrades (uuid, upgrade_id, level) VALUES (?,?,?)")) {
                    ps.setString(1, uuid); ps.setString(2, entry.getKey());
                    ps.setInt(3, entry.getValue()); ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save tycoon: " + uuid + " - " + e.getMessage());
        }
    }

    public void saveAllTycoons(Collection<Tycoon> tycoons) {
        for (Tycoon t : tycoons) saveTycoon(t);
    }

    public void deleteTycoon(UUID uuid) {
        String id = uuid.toString();
        try {
            for (String table : List.of("tycoons", "tycoon_upgrades", "tycoon_coop",
                    "tycoon_pets", "quest_progress", "leaderboard")) {
                String col = table.equals("tycoon_coop") ? "owner_uuid" : "uuid";
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM " + table + " WHERE " + col + " = ?")) {
                    ps.setString(1, id); ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete tycoon: " + id + " - " + e.getMessage());
        }
    }

    public void saveCoopMember(UUID owner, UUID member, String memberName) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO tycoon_coop (owner_uuid, member_uuid, member_name) VALUES (?,?,?)")) {
            ps.setString(1, owner.toString()); ps.setString(2, member.toString());
            ps.setString(3, memberName); ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("Failed to save co-op data: " + e.getMessage()); }
    }

    public void removeCoopMember(UUID owner, UUID member) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM tycoon_coop WHERE owner_uuid = ? AND member_uuid = ?")) {
            ps.setString(1, owner.toString()); ps.setString(2, member.toString());
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public void saveOwnedPet(UUID uuid, String petId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO tycoon_pets (uuid, pet_id) VALUES (?,?)")) {
            ps.setString(1, uuid.toString()); ps.setString(2, petId); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public Map<String, int[]> loadQuestProgress(UUID uuid) {
        Map<String, int[]> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT quest_id, progress, completed, claimed, reset_time FROM quest_progress WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("quest_id"), new int[]{
                            rs.getInt("progress"), rs.getInt("completed"),
                            rs.getInt("claimed"), (int) (rs.getLong("reset_time") / 1000)
                    });
                }
            }
        } catch (SQLException ignored) {}
        return map;
    }

    public void saveQuestProgress(UUID uuid, String questId, int progress,
                                  boolean completed, boolean claimed, long resetTime) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT OR REPLACE INTO quest_progress
                (uuid, quest_id, progress, completed, claimed, reset_time) VALUES (?,?,?,?,?,?)""")) {
            ps.setString(1, uuid.toString()); ps.setString(2, questId);
            ps.setInt(3, progress); ps.setInt(4, completed ? 1 : 0);
            ps.setInt(5, claimed ? 1 : 0); ps.setLong(6, resetTime);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public void updateLeaderboard(UUID uuid, String name, long totalEarned, int prestige, int rebirth) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT OR REPLACE INTO leaderboard
                (uuid, player_name, total_earned, prestige_level, rebirth_level) VALUES (?,?,?,?,?)""")) {
            ps.setString(1, uuid.toString()); ps.setString(2, name);
            ps.setLong(3, totalEarned); ps.setInt(4, prestige); ps.setInt(5, rebirth);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public List<String[]> getTopPlayers(int limit) {
        List<String[]> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_name, total_earned, prestige_level, rebirth_level FROM leaderboard ORDER BY total_earned DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("player_name"),
                            String.valueOf(rs.getLong("total_earned")),
                            String.valueOf(rs.getInt("prestige_level")),
                            String.valueOf(rs.getInt("rebirth_level"))
                    });
                }
            }
        } catch (SQLException ignored) {}
        return list;
    }

    public void logTrade(UUID p1, UUID p2, double money1, double money2, String pet1, String pet2) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO trade_log (player1_uuid, player2_uuid, money1, money2, pet1, pet2, timestamp) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, p1.toString()); ps.setString(2, p2.toString());
            ps.setDouble(3, money1); ps.setDouble(4, money2);
            ps.setString(5, pet1); ps.setString(6, pet2);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to log trade: " + e.getMessage());
        }
    }

    public List<String[]> getRecentTrades(int limit) {
        List<String[]> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM trade_log ORDER BY timestamp DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("player1_uuid"), rs.getString("player2_uuid"),
                            String.format("%.2f", rs.getDouble("money1")),
                            String.format("%.2f", rs.getDouble("money2")),
                            rs.getString("pet1") != null ? rs.getString("pet1") : "-",
                            rs.getString("pet2") != null ? rs.getString("pet2") : "-",
                            String.valueOf(rs.getLong("timestamp"))
                    });
                }
            }
        } catch (SQLException ignored) {}
        return list;
    }
}
