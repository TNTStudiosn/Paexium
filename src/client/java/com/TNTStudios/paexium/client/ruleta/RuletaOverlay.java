package com.TNTStudios.paexium.client.ruleta;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class RuletaOverlay implements HudRenderCallback {

    // Textura de la ruleta
    private static final Identifier RULETA_TEXTURE = new Identifier("paexium", "textures/gui/ruleta.png");
    // Textura de la flecha
    private static final Identifier ARROW_TEXTURE = new Identifier("paexium", "textures/gui/flecha.png");

    private static final String[] OPCIONES = {
            "opcion1","opcion2","opcion3","opcion4",
            "opcion5","opcion6","opcion7","opcion8"
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

    public static void startSpin(int opcionGanadora, long serverTick) {
        spinning = true;
        finishedSpin = false;
        chosenOption = opcionGanadora;
        startServerTick = serverTick;
        postSpinTicks = 0;
        prevStep = -1;
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (!spinning && postSpinTicks <= 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        long currentServerTime = client.world.getTime();
        long elapsed = currentServerTime - startServerTick;

        // Aún no empieza
        if (elapsed < 0) return;

        float t = (float) elapsed / (float) DURATION;
        if (t > 1.0f) t = 1.0f;

        // Ease out
        float ease = 1.0f - (float)Math.pow(1.0f - t, 2);

        // Ángulo para dejar la rebanada ganadora arriba
        float targetAngle = 360f - (chosenOption * 45f) - 22.5f;
        finalAngle = BASE_SPINS_DEGREES + targetAngle;

        float angle = (!finishedSpin) ? finalAngle * ease : finalAngle;

        // Dibuja la ruleta (rotando)
        drawRuleta(drawContext, angle);

        // Si quieres que la flecha sea FIJA (no rote con la ruleta),
        // la dibujamos FUERA del push/pop de la ruleta:
        drawArrow(drawContext);

        if (!finishedSpin) {
            playTickSound(client, t);

            if (t >= 1.0f) {
                finishedSpin = true;
                postSpinTicks = 180;

                client.getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F)
                );
                if (client.player != null && chosenOption >= 0 && chosenOption < OPCIONES.length) {
                    client.player.sendMessage(
                            net.minecraft.text.Text.literal("¡La ruleta cayó en: " + OPCIONES[chosenOption] + "!")
                    );
                }
            }
        } else {
            postSpinTicks--;
            if (postSpinTicks <= 0) {
                spinning = false;
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
        // Trasladar al centro + rotar la ruleta
        drawContext.getMatrices().translate(centerX, centerY, 0);
        drawContext.getMatrices().multiply(
                new org.joml.Quaternionf().rotationXYZ(0, 0, (float)Math.toRadians(angle))
        );

        // Dibujamos la ruleta en 200x200
        int radius = 100;
        drawContext.drawTexture(RULETA_TEXTURE, -radius, -radius, 0, 0, 200, 200, 200, 200);

        // Dibujamos el texto “inclinado/tangencial”
        int textRadius = 60;
        for (int i = 0; i < 8; i++) {
            String label = OPCIONES[i];
            float sliceAngle = (45 * i) + 22.5f;
            float rad = (float)Math.toRadians(sliceAngle);
            float textX = (float)(Math.cos(rad) * textRadius);
            float textY = (float)(Math.sin(rad) * textRadius);

            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(textX, textY, 0);
            // Rotamos para “seguir” la curva, por ejemplo “sliceAngle”
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

        // Salimos del push() de la ruleta
        drawContext.getMatrices().pop();
    }

    /**
     * Dibuja la flecha por encima de la ruleta, sin rotación.
     * Ajusta 'flechaWidth/Height' y las coords para posicionarla.
     */
    private void drawArrow(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Por ejemplo, una flecha de 200x200
        int flechaWidth = 200;
        int flechaHeight = 200;


        // La ubicamos en el centro Y, a la derecha de la ruleta (x = centro + radius + 10)
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int radius = 100;

        int arrowX = centerX - (flechaWidth / 2) + 14;
        int arrowY = centerY - (flechaHeight / 2);

        // Simplemente dibujamos la flecha
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
