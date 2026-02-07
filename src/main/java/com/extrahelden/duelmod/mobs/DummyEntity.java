package com.extrahelden.duelmod.mobs;

import com.extrahelden.duelmod.handler.DummyManager;
import com.extrahelden.duelmod.helper.Helper;
import com.extrahelden.duelmod.serializer.GameProfileSerializer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DummyEntity extends Zombie {
    private static final EntityDataAccessor<GameProfile> GAME_PROFILE =
            SynchedEntityData.defineId(DummyEntity.class, GameProfileSerializer.GAME_PROFILE_SERIALIZER);

    private final SkinInfo skinInfo;
    private boolean reloadTextures = true;
    private final List<ItemStack> inventoryItems = new ArrayList<>();
    private UUID playerUUID;
    private Component playerName;

    public DummyEntity(EntityType<? extends Zombie> type, Level world) {
        super(type, world);
        this.skinInfo = new SkinInfo();
        this.entityData.set(GAME_PROFILE, new GameProfile(UUID.randomUUID(), "DummyZombie"));
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GAME_PROFILE, new GameProfile(UUID.randomUUID(), "DummyZombie"));
    }

    public GameProfile getGameProfile() {
        return this.entityData.get(GAME_PROFILE);
    }

    public void setGameProfile(GameProfile profile) {
        this.entityData.set(GAME_PROFILE, profile);
        if (!level().isClientSide) {
            this.reloadTextures = true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void updateSkinTexture() {
        if (!reloadTextures) return;
        reloadTextures = false;
        GameProfile prof = getGameProfile();
        if (prof == null || prof.getId() == null) return;

        Minecraft.getInstance().getSkinManager()
                .registerSkins(prof, (type, loc, tex) -> {
                    synchronized (skinInfo) {
                        skinInfo.textures.put(type, loc);
                        if (type == Type.SKIN) {
                            skinInfo.skinType = tex.getMetadata("model");
                            if (skinInfo.skinType == null) skinInfo.skinType = "default";
                        }
                    }
                }, true);
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getSkin() {
        updateSkinTexture();
        synchronized (skinInfo) {
            return skinInfo.textures.getOrDefault(
                    Type.SKIN,
                    net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(getGameProfile().getId())
            );
        }
    }

    public void storeInventory(Inventory inv, Player player) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty()) inventoryItems.add(st.copy());
        }
    }

    @Override
    public void die(DamageSource source) {
        dropInventoryItems();
        DummyManager.setPlayerDummyStatusDead(this);
        DummyManager.removeDummy(this);

        Entity atk = source.getEntity();
        if (atk instanceof Player p) {
            Component msg = Component.literal(Helper.getPrefix() + " ")
                    .append(Component.literal(playerName.getString())
                            .withStyle(s -> s.withColor(0xFFA533).withBold(true)))
                    .append(Component.literal("§r§f wurde von "))
                    .append(Component.literal(p.getName().getString())
                            .withStyle(s -> s.withColor(0x5600DD).withBold(true)))
                    .append(Component.literal("§r§f besiegt während er ausgeloggt war!"));
            MinecraftServer srv = Objects.requireNonNull(p.getServer());
            srv.getPlayerList().broadcastSystemMessage(msg, false);
        }
        super.die(source);
    }

    @Override
    public boolean hurt(DamageSource src, float amt) {
        return super.hurt(src, amt);
    }

    @Override protected SoundEvent getAmbientSound()              { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource ds) { return null; }
    @Override protected SoundEvent getDeathSound()                { return null; }

    private void dropInventoryItems() {
        for (ItemStack st : inventoryItems) this.spawnAtLocation(st);
        inventoryItems.clear();
    }

    @Override
    public void tick() {
        super.tick();
        // korrigierte Spieler‑Suche:
        Player target = level().getNearestPlayer(getX(), getY(), getZ(), 10, false);
        if (target != null) {
            this.getLookControl().setLookAt(target, 10.0F, 10.0F);
        }
    }

    public void setPlayerUUID(UUID id)     { this.playerUUID = id; }
    public UUID getPlayerUUID()            { return this.playerUUID; }
    public void setPlayerName(Component n) { this.playerName = n; }
    public Component getPlayerName()       { return this.playerName; }

    private static class SkinInfo {
        final Map<Type, ResourceLocation> textures = new ConcurrentHashMap<>();
        String skinType = "default";
    }
}



