package dev.xdev.moneytycoon.model;

public class Quest {

    private final String id;
    private final String displayName;
    private final String description;
    private final String type;
    private final int target;
    private final double reward;
    private final boolean daily;

    private int progress;
    private boolean completed;
    private boolean claimed;
    private long resetTime;

    public Quest(String id, String displayName, String description,
                 String type, int target, double reward, boolean daily) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.target = target;
        this.reward = reward;
        this.daily = daily;
        this.progress = 0;
        this.completed = false;
        this.claimed = false;
        this.resetTime = 0;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public int getTarget() { return target; }
    public double getReward() { return reward; }
    public boolean isDaily() { return daily; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) {
        this.progress = progress;
        if (this.progress >= target) this.completed = true;
    }
    public void incrementProgress(int amount) { setProgress(this.progress + amount); }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }

    public long getResetTime() { return resetTime; }
    public void setResetTime(long resetTime) { this.resetTime = resetTime; }

    public void reset() {
        this.progress = 0;
        this.completed = false;
        this.claimed = false;
    }
}
