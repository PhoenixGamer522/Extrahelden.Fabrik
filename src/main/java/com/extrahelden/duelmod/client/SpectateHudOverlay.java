package com.extrahelden.duelmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpectateHudOverlay {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player cam = mc.getCameraEntity() instanceof Player ? (Player) mc.getCameraEntity() : null;

        // Nur wenn wir jemand anderen spectaten
        if (mc.player != null && cam != null && cam != mc.player) {
            GuiGraphics gui = event.getGuiGraphics();

            // Health (Herzen)
            int health = (int) cam.getHealth();
            int maxHealth = (int) cam.getMaxHealth();
            gui.drawString(mc.font, "❤ " + health + " / " + maxHealth, 10, 10, 0xFF5555);

            // Hunger
            int food = cam.getFoodData().getFoodLevel();
            gui.drawString(mc.font, "🍗 " + food, 10, 25, 0xFFD37F);

            // Rüstung
            int armor = cam.getArmorValue();
            gui.drawString(mc.font, "🛡 " + armor, 10, 40, 0x55FFFF);
        }
    }
}
