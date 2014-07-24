package com.garbagemule.MobArena;

import java.util.*;

import com.garbagemule.MobArena.grantable.Grantable;
import org.bukkit.entity.Player;

import com.garbagemule.MobArena.framework.Arena;

public class RewardManager
{
    private Set<Player> rewarded;
    private Map<String,List<Grantable>> rewards;
    
    public RewardManager(Arena arena) {
        this.rewarded = new HashSet<Player>();
        this.rewards  = new HashMap<String, List<Grantable>>();
    }
    
    public void reset() {
        rewards.clear();
        rewarded.clear();
    }
    
    public void addReward(Player p, Grantable reward) {
        if (reward == null) {
            throw new IllegalArgumentException("Rewards cannot be null!");
        }
        if (!rewards.containsKey(p.getName())) {
            rewards.put(p.getName(), new ArrayList<Grantable>());
        }
        rewards.get(p.getName()).add(reward);
    }
    
    public void grantRewards(Player p) {
        if (rewarded.contains(p)) return;

        List<Grantable> list = rewards.get(p.getName());
        if (list == null) return;

        for (Grantable reward : list) {
            reward.grant(p);
        }
        rewarded.add(p);
    }
}