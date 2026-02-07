package com.extrahelden.duelmod.client;

import com.extrahelden.duelmod.helper.LinkedHeartHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Overlay für Custom-Hearts + LinkedHeart */
public class HeartBarOverlay {
    private static final ResourceLocation MYLIVES    = new ResourceLocation("duelmod", "textures/extrahelden/mylives.png");
    private static final ResourceLocation EMPTYLIVES = new ResourceLocation("duelmod", "textures/extrahelden/emptylives.png");

    private static final int ICON_W = 9, ICON_H = 9;

    // Kopftextur ist 64x64 bei modernen Skins
    private static final int SKIN_TEX_W = 64, SKIN_TEX_H = 64;

    public static final IGuiOverlay HEARTS = HeartBarOverlay::render;

    private static void render(ForgeGui gui, GuiGraphics gg, float partialTicks, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (mc.gameMode == null || mc.gameMode.getPlayerMode() != GameType.SURVIVAL) return;
        if (!player.getPersistentData().contains("MyLives")) return;
        if (player.getPersistentData().getBoolean("InDuel")) return;

         int lives = player.getPersistentData().getInt("MyLives");
        boolean linkedActive = player.getPersistentData().getBoolean("LinkedHeartActive");

        // gleiche Basis-Position wie bisher
        int x = sw / 2 - 4;
        int y = sh - 44;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        gg.pose().pushPose();

        if (lives > 0) {
            // --- Leere Herzen ---
            RenderSystem.setShaderTexture(0, EMPTYLIVES);
            gg.blit(EMPTYLIVES, x,      y - 7, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H); // top
            gg.blit(EMPTYLIVES, x - 4,  y,     0, 0, ICON_W, ICON_H, ICON_W, ICON_H); // left
            gg.blit(EMPTYLIVES, x + 4,  y,     0, 0, ICON_W, ICON_H, ICON_W, ICON_H); // right

            // --- Gefüllte Herzen ---
            RenderSystem.setShaderTexture(0, MYLIVES);
            if (lives >= 1) gg.blit(MYLIVES, x - 4, y,     0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
            if (lives >= 2) gg.blit(MYLIVES, x + 4, y,     0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
            if (lives >= 3) gg.blit(MYLIVES, x,     y - 7, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);

        } else if (linkedActive) {
            // ----- LINKED HEART (Spielerkopf 16x16, mittig über dem oberen Herz) -----
            String ownerName = player.getPersistentData().getString("LinkedHeartOwner");
            String ownerUuid = player.getPersistentData().getString("LinkedHeartOwnerUUID");

            ResourceLocation skin = LinkedHeartHelper.getSkinFor(ownerName, ownerUuid, mc, player);
            if (skin != null) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, skin);

                // Wir wollen 16x16 mittig an der Position des oberen 9x9-Herzens (x, y-7)
                // Daher um (16 - 9) / 2 = 3 Pixel nach links/oben verschieben:
                int headX = x - 3;
                int headY = (y - 7) - 3;

                gg.pose().pushPose();
                gg.pose().translate(headX, headY, 0);
                // Wir zeichnen 8x8 aus der Skin-Textur und skalieren auf 16x16 → scale 2.0
                gg.pose().scale(2f, 2f, 1f);

                // Gesicht: UV (8,8)-(16,16) → draw 8x8, skaliert 2x = 16x16
                gg.blit(skin, 0, 0, 8, 8, 8, 8, SKIN_TEX_W, SKIN_TEX_H);
                // Hut/Overlay: UV (40,8)-(48,16), ebenfalls 8x8
                gg.blit(skin, 0, 0, 40, 8, 8, 8, SKIN_TEX_W, SKIN_TEX_H);

                gg.pose().popPose();

                RenderSystem.disableBlend();
            }

        } else {
            // lives == 0 aber LinkedHeartActive == false
            // → optional NICHTS rendern (oder nur leere Herzen, wenn du willst):
            // RenderSystem.setShaderTexture(0, EMPTYLIVES);
            // gg.blit(EMPTYLIVES, x,      y - 7, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
            // gg.blit(EMPTYLIVES, x - 4,  y,     0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
            // gg.blit(EMPTYLIVES, x + 4,  y,     0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
        }

        gg.pose().popPose();
    }
}
