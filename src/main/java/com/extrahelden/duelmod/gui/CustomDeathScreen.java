package com.extrahelden.duelmod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CustomDeathScreen extends DeathScreen {

    private static final int FRAME_COUNT = 43;
    private static final int FPS = 50; // echte FPS der Animation
    private static final ResourceLocation[] FRAMES = new ResourceLocation[FRAME_COUNT];

    private int frameIndex = 0;
    private long lastFrameTime = 0;
    private boolean animationFinished = false;

    public CustomDeathScreen(Component title, boolean causeReported) {
        super(title, causeReported);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {

        long now = System.currentTimeMillis();
        long frameDelay = 1000L / FPS;

        // === FRAME UPDATE (nur wenn nötig!) ===
        if (!animationFinished && now - lastFrameTime >= frameDelay) {
            frameIndex++;
            lastFrameTime = now;

            if (frameIndex >= FRAME_COUNT - 1) {
                frameIndex = FRAME_COUNT - 1;
                animationFinished = true;
            }
        }

        ResourceLocation tex = FRAMES[frameIndex];
        RenderSystem.setShaderTexture(0, tex);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        g.blit(
                tex,
                0, 0,
                0, 0,
                this.width, this.height,
                this.width, this.height
        );

        RenderSystem.disableBlend();

        // Vanilla DeathScreen erst nach Animation
        if (animationFinished) {
            super.render(g, mouseX, mouseY, partialTick);
        }
    }

    // 🔥 TEXTURE PRELOAD (EXTREM WICHTIG!)
    public static void preloadTextures() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < FRAME_COUNT; i++) {
            mc.getTextureManager().getTexture(FRAMES[i]);
        }
    }

    static {
        for (int i = 0; i < FRAME_COUNT; i++) {
            FRAMES[i] = new ResourceLocation(
                    "duelmod",
                    "textures/gui/frames_anim/minecrafthelden_" +
                            String.format("%03d", i + 1) + ".png"
            );
        }
    }
}
