package com.extrahelden.duelmod.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class SpectatorHudOverlay {

    /** Spieler, den wir beobachten */
    private static Player spectated = null;

    /** Vanilla-Herz-Textur */
    private static final ResourceLocation ICONS =
            new ResourceLocation("minecraft", "textures/gui/icons.png");

    /** Spieler setzen (vom Client aus!) */
    public static void setSpectated(Player player) {
        spectated = player;
    }

    /** Spectate-Modus beenden */
    public static void clearSpectated() {
        spectated = null;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (spectated == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        PoseStack pose = gui.pose();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Vanilla-Herz-Position
        int x = screenWidth / 2 - 91;
        int y = screenHeight - 39;

        RenderSystem.setShaderTexture(0, ICONS);

        int health = (int) Math.ceil(spectated.getHealth());
        int maxHealth = (int) Math.ceil(spectated.getMaxHealth());

        int hearts = maxHealth / 2;

        for (int i = 0; i < hearts; i++) {
            int drawX = x + i * 8;
            int drawY = y;

            // Herz-Hintergrund
            gui.blit(ICONS, drawX, drawY, 16, 0, 9, 9);

            int heartHealth = (i + 1) * 2;

            if (health >= heartHealth) {
                // volles Herz
                gui.blit(ICONS, drawX, drawY, 52, 0, 9, 9);
            } else if (health == heartHealth - 1) {
                // halbes Herz
                gui.blit(ICONS, drawX, drawY, 61, 0, 9, 9);
            }
        }
    }
}
