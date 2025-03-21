package com.TNTStudios.paexium.client.ruleta;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class RuletaOverlay implements HudRenderCallback {

    // Ruta a la textura de la ruleta (200x200 px, por ejemplo)
    private static final Identifier RULETA_TEXTURE = new Identifier("paexium", "textures/gui/ruleta.png");

    // Opciones que se muestran en la ruleta.
    // Puedes cambiar estos textos o cargarlos dinámicamente.
    private static final String[] OPCIONES = {
            "opcion1", "opcion2", "opcion3", "opcion4",
            "opcion5", "opcion6", "opcion7", "opcion8"
    };

    // Estado de la animación
    private static boolean spinning = false;
    private static float angle = 0;          // Ángulo actual de la ruleta en grados
    private static float speed = 0;          // Velocidad actual en grados/tick
    private static int chosenOption = -1;    // Índice de la opción ganadora

    // Control de tiempo para asegurar ~6 segundos de giro
    private static int spinTick = 0;         // Cuántos ticks han pasado desde que empezó el giro
    private static final int MAX_SPIN_TICKS = 120; // 120 ticks = 6 segundos a 20 TPS

    // Para el sonido de “tick” de la ruleta
    private static int tickSoundCounter = 0;

    /**
     * Llamado desde el packet cuando el servidor decide la opción ganadora.
     */
    public static void startSpin(int opcionGanadora) {
        spinning = true;
        chosenOption = opcionGanadora;

        // Reiniciamos la animación
        angle = 0.0f;
        speed = 25.0f;        // Velocidad inicial (grados/tick). Ajusta si quieres giros más rápidos/lentos
        spinTick = 0;
        tickSoundCounter = 0;
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (!spinning) return;

        MinecraftClient client = MinecraftClient.getInstance();
        // Centro de la ventana
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Guardar matriz original
        drawContext.getMatrices().push();

        // Mover al centro
        drawContext.getMatrices().translate(centerX, centerY, 0);

        // Rotar según el ángulo actual (ruleta girada)
        drawContext.getMatrices().multiply(
                new org.joml.Quaternionf().rotationXYZ(0, 0, (float) Math.toRadians(angle))
        );

        // Dibujar la textura
        int radius = 100; // La mitad del tamaño de la imagen (200x200)
        drawContext.drawTexture(RULETA_TEXTURE, -radius, -radius, 0, 0, 200, 200, 200, 200);

        // **Dibujar el texto** sobre cada slice de la ruleta, girando con ella
        // Cada slice son 45° (360/8). Usamos un radio menor que 'radius' para centrar el texto.
        int textRadius = 60;
        for (int i = 0; i < 8; i++) {
            // Ángulo central de cada porción (slice)
            float sliceAngle = (45 * i) + 22.5f;
            double rad = Math.toRadians(sliceAngle);

            // Posición del texto en coordenadas locales (ya rotadas por 'angle')
            float textX = (float) (Math.cos(rad) * textRadius);
            float textY = (float) (Math.sin(rad) * textRadius);

            // Dibujar el texto centrado y con sombra
            drawContext.drawCenteredTextWithShadow(
                    client.textRenderer,
                    OPCIONES[i],
                    (int) textX,
                    (int) textY,
                    0xFFFFFF // color blanco
            );
        }

        // Restaurar la matriz (para no afectar otros renders de HUD)
        drawContext.getMatrices().pop();

        // Avanzar la animación
        updateSpin(client);
    }

    /**
     * Lógica de animación (velocidad, frenado, sonidos, etc.)
     */
    private void updateSpin(MinecraftClient client) {
        spinTick++;
        if (spinTick <= MAX_SPIN_TICKS) {
            // Sigue girando
            angle += speed;

            // Sonido “tick” cada cierto número de ticks
            tickSoundCounter++;
            if (tickSoundCounter >= 5) {
                client.getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F)
                );
                tickSoundCounter = 0;
            }

            // Frenado lineal a lo largo de MAX_SPIN_TICKS ( de speed inicial a 0 en 120 ticks )
            float decPerTick = 25.0f / MAX_SPIN_TICKS;
            speed = Math.max(0, speed - decPerTick);

        } else {
            // Ya pasaron los 6 segundos (o más), paramos
            spinning = false;

            // Ajustar ángulo final para que el sector chosenOption quede "arriba" (o donde quieras)
            // Cada slice son 45°, restamos ~22.5 para centrar
            float finalAngle = 360f - (45f * chosenOption) - 22.5f;
            angle = (finalAngle % 360f + 360f) % 360f; // normalizar

            // Sonido final
            client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F)
            );

            // Mensaje de resultado
            if (client.player != null) {
                client.player.sendMessage(
                        net.minecraft.text.Text.literal(
                                "¡La ruleta cayó en: " + OPCIONES[chosenOption] + "!"
                        )
                );
            }
        }
    }
}
