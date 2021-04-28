package com.garbagemule.MobArena;

import com.garbagemule.MobArena.framework.Arena;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

public class SpawnsPets {

    private final Map<Material, EntityType> materialToEntity;

    SpawnsPets() {
        this.materialToEntity = new HashMap<>();
    }

    void register(Material material, EntityType entity) {
        materialToEntity.put(material, entity);
    }

    void clear() {
        materialToEntity.clear();
    }

    void spawn(Arena arena) {
        arena.getPlayersInArena()
            .forEach(player -> spawnPets(player, arena));
    }

    private void spawnPets(Player player, Arena arena) {
        if (player == null || !player.isOnline()) {
            return;
        }
        ArenaClass ac = arena.getArenaPlayer(player).getArenaClass();
        if (ac == null || ac.getConfigName().equals("My Items")) {
            return;
        }

        for (Map.Entry<Material, EntityType> entry : materialToEntity.entrySet()) {
            spawnPetsFor(player, arena, entry.getKey(), entry.getValue());
        }
    }

    private void spawnPetsFor(Player player, Arena arena, Material material, EntityType entity) {
        PlayerInventory inv = player.getInventory();

        int index = inv.first(material);
        if (index < 0) {
            return;
        }

        int amount = inv.getItem(index).getAmount();
        for (int i = 0; i < amount; i++) {
            Entity pet = arena.getWorld().spawn(player.getLocation(), entity.getEntityClass());
            pet.setCustomName(player.getDisplayName() + "'s pet");
            pet.setCustomNameVisible(true);
            if (pet instanceof Tameable) {
                Tameable tameable = (Tameable) pet;
                tameable.setTamed(true);
                tameable.setOwner(player);
            }
            arena.setMetaData(pet);
            arena.getMonsterManager().addPet(player, pet);
        }

        inv.setItem(index, null);
    }

}
