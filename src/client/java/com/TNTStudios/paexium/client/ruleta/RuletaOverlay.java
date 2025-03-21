package com.TNTStudios.paexium.client.ruleta;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class RuletaOverlay implements HudRenderCallback {

    private static final Identifier RULETA_TEXTURE = new Identifier("paexium", "textures/gui/ruleta.png");
    private static final Identifier ARROW_TEXTURE = new Identifier("paexium", "textures/gui/flecha.png");

    private static final String[] OPCIONES = {
            "Castillos", "Memes", "Videojuego", "Caricatura",
            "Laberinto", "Casa", "Libre", "XPG"
    };

    private static final int DURATION = 280;
    private static final float BASE_SPINS_DEGREES = 3240.0f;

    private static boolean spinning = false;
    private static boolean finishedSpin = false;
    private static int chosenOption = -1;
    private static int postSpinTicks = 0;

    private static long startServerTick = 0;
    private static float finalAngle = 0;
    private static int prevStep = -1;
    private static boolean titleDisplayed = false;

    public static void startSpin(int opcionGanadora, long serverTick) {
        spinning = true;
        finishedSpin = false;
        chosenOption = opcionGanadora;
        startServerTick = serverTick;
        postSpinTicks = 0;
        prevStep = -1;
        titleDisplayed = false;
    }

    private static float finalOverlayYOffset = 0;

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (!spinning && postSpinTicks <= 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        long currentServerTime = client.world.getTime();
        long elapsed = currentServerTime - startServerTick;
        if (elapsed < 0) return;

        float t = (float) elapsed / (float) DURATION;
        if (t > 1.0f) t = 1.0f;

        float ease = 1.0f - (float)Math.pow(1.0f - t, 2);
        float targetAngle = 360f - (chosenOption * 45f) - 22.5f;
        finalAngle = BASE_SPINS_DEGREES + targetAngle;
        float angle = (!finishedSpin) ? finalAngle * ease : finalAngle;

        float overlayOpacity = 1.0f;
        float overlayYOffset = 0.0f;

        // Fade In: primeros 10% de la animación
        if (!finishedSpin && t < 0.1f) {
            float progress = t / 0.1f;
            overlayOpacity = progress;          // de 0 a 1
            overlayYOffset = (1 - progress) * 80; // parte 80px abajo y sube a 0
        }
        // Fade Out: durante la fase estática (finishedSpin)
        else if (finishedSpin) {
            int fadeOutDuration = 200; // fade out prolongado en 200 ticks
            if (postSpinTicks > fadeOutDuration) {
                overlayOpacity = 1.0f;
                overlayYOffset = finalOverlayYOffset;
            } else {
                float progress = (float)(fadeOutDuration - postSpinTicks) / fadeOutDuration;
                overlayOpacity = 1.0f - progress; // de 1 a 0
                overlayYOffset = finalOverlayYOffset + progress * 400; // se mueve 400px más abajo al desaparecer
            }
        }

        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(0, overlayYOffset, 0);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, overlayOpacity);

        drawRuleta(drawContext, angle);
        drawArrow(drawContext);

        drawContext.getMatrices().pop();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Manejo de tiempo y fin de animación
        if (!finishedSpin) {
            playTickSound(client, t);
            if (t >= 1.0f) {
                finishedSpin = true;
                finalOverlayYOffset = overlayYOffset;
                postSpinTicks = 260; // tiempo estático antes del fade out
            }
        } else {
            postSpinTicks--;
            if (postSpinTicks <= 0 && !titleDisplayed) {
                TitleS2CPacket titlePacket = new TitleS2CPacket(
                        net.minecraft.text.Text.literal("La Tematica es:").formatted(Formatting.GOLD)
                );
                SubtitleS2CPacket subtitlePacket = new SubtitleS2CPacket(
                        net.minecraft.text.Text.literal(OPCIONES[chosenOption]).formatted(Formatting.AQUA)
                );
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().onTitle(titlePacket);
                    client.getNetworkHandler().onSubtitle(subtitlePacket);
                }
                titleDisplayed = true;
                spinning = false;
                client.getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0F, 2.0F)
                );
            }
        }
    }


    private void drawRuleta(DrawContext drawContext, float angle) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(centerX, centerY, 1000);
        drawContext.getMatrices().multiply(
                new org.joml.Quaternionf().rotationXYZ(0, 0, (float)Math.toRadians(angle))
        );

        int radius = 100;
        drawContext.drawTexture(RULETA_TEXTURE, -radius, -radius, 0, 0, 200, 200, 200, 200);

        int textRadius = 55;
        for (int i = 0; i < 8; i++) {
            String label = OPCIONES[i];
            float sliceAngle = (45 * i) + 22.5f;
            float rad = (float)Math.toRadians(sliceAngle);
            float textX = (float)(Math.cos(rad) * textRadius);
            float textY = (float)(Math.sin(rad) * textRadius);

            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(textX, textY, 0);
            drawContext.getMatrices().multiply(
                    new org.joml.Quaternionf().rotationXYZ(0, 0, (float)Math.toRadians(sliceAngle))
            );

            int textWidth = client.textRenderer.getWidth(label);
            int textHeight = 9;
            drawContext.drawText(
                    client.textRenderer,
                    label,
                    -textWidth / 2,
                    -textHeight / 2,
                    0xFFFFFF,
                    false
            );
            drawContext.getMatrices().pop();
        }
        drawContext.getMatrices().pop();
    }

    private void drawArrow(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int flechaWidth = 200;
        int flechaHeight = 200;
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int arrowX = centerX - (flechaWidth / 2) + 14;
        int arrowY = centerY - (flechaHeight / 2);

        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(0, 0, 1000);
        drawContext.drawTexture(
                ARROW_TEXTURE,
                arrowX,
                arrowY,
                0,
                0,
                flechaWidth,
                flechaHeight,
                flechaWidth,
                flechaHeight
        );
        drawContext.getMatrices().pop();
    }

    private void playTickSound(MinecraftClient client, float t) {
        int stepCount = 20;
        float step = 1.0f / stepCount;
        int currentStep = (int)(t / step);
        if (currentStep != prevStep) {
            prevStep = currentStep;
            client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F)
            );
        }
    }
}
