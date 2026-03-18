package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.Quest;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class QuestManager {

    private final MoneyTycoon plugin;
    private final Map<UUID, Map<String, Quest>> playerQuests = new HashMap<>();
    private final Map<String, QuestTemplate> templates = new HashMap<>();

    public record QuestTemplate(String id, String displayName, String description,
                                String type, int target, double reward, boolean daily) {}

    public QuestManager(MoneyTycoon plugin) {
        this.plugin = plugin;
        loadTemplates();
    }

    private void loadTemplates() {
        templates.clear();
        loadSection("quests.daily", true);
        loadSection("quests.weekly", false);
    }

    private void loadSection(String path, boolean daily) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            templates.put(key, new QuestTemplate(key,
                    sec.getString(key + ".display-name", key).replace("&", "§"),
                    sec.getString(key + ".description", "").replace("&", "§"),
                    sec.getString(key + ".type", "SELL_ITEMS"),
                    sec.getInt(key + ".target", 100),
                    sec.getDouble(key + ".reward", 500),
                    daily));
        }
    }

    public void loadPlayer(UUID uuid) {
        Map<String, int[]> dbData = plugin.getDatabaseManager().loadQuestProgress(uuid);
        Map<String, Quest> quests = new HashMap<>();
        long now = System.currentTimeMillis();

        for (QuestTemplate tmpl : templates.values()) {
            Quest quest = new Quest(tmpl.id, tmpl.displayName, tmpl.description,
                    tmpl.type, tmpl.target, tmpl.reward, tmpl.daily);

            int[] saved = dbData.get(tmpl.id);
            if (saved != null) {
                long resetTime = saved[3] * 1000L;
                if (resetTime > 0 && now > resetTime) {
                    quest.reset();
                    quest.setResetTime(calcNextReset(tmpl.daily));
                } else {
                    quest.setProgress(saved[0]);
                    quest.setCompleted(saved[1] == 1);
                    quest.setClaimed(saved[2] == 1);
                    quest.setResetTime(resetTime > 0 ? resetTime : calcNextReset(tmpl.daily));
                }
            } else {
                quest.setResetTime(calcNextReset(tmpl.daily));
            }
            quests.put(tmpl.id, quest);
        }
        playerQuests.put(uuid, quests);
    }

    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid);
        playerQuests.remove(uuid);
    }

    public void savePlayer(UUID uuid) {
        Map<String, Quest> quests = playerQuests.get(uuid);
        if (quests == null) return;
        for (Quest q : quests.values()) {
            plugin.getDatabaseManager().saveQuestProgress(uuid, q.getId(),
                    q.getProgress(), q.isCompleted(), q.isClaimed(), q.getResetTime());
        }
    }

    public void saveAll() {
        for (UUID uuid : playerQuests.keySet()) savePlayer(uuid);
    }

    public void incrementProgress(UUID uuid, String questType, int amount) {
        Map<String, Quest> quests = playerQuests.get(uuid);
        if (quests == null) return;

        long now = System.currentTimeMillis();
        for (Quest quest : quests.values()) {
            if (quest.isClaimed()) continue;
            if (quest.getResetTime() > 0 && now > quest.getResetTime()) {
                quest.reset();
                quest.setResetTime(calcNextReset(quest.isDaily()));
            }
            if (quest.getType().equalsIgnoreCase(questType) && !quest.isCompleted()) {
                quest.incrementProgress(amount);
            }
        }
    }

    public boolean claimReward(UUID uuid, String questId) {
        Map<String, Quest> quests = playerQuests.get(uuid);
        if (quests == null) return false;
        Quest quest = quests.get(questId);
        if (quest == null || !quest.isCompleted() || quest.isClaimed()) return false;

        quest.setClaimed(true);
        plugin.getEconomyManager().deposit(uuid, quest.getReward());
        return true;
    }

    public Map<String, Quest> getPlayerQuests(UUID uuid) {
        return playerQuests.getOrDefault(uuid, Collections.emptyMap());
    }

    private long calcNextReset(boolean daily) {
        Calendar cal = Calendar.getInstance();
        if (daily) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
        } else {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            if (cal.getTimeInMillis() <= System.currentTimeMillis())
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
        }
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
