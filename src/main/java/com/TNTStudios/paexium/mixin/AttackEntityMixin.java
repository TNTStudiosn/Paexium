package com.TNTStudios.paexium.mixin;

import com.TNTStudios.paexium.commands.AsignarParcelasCommand;
import com.TNTStudios.paexium.items.PaexiumItems;
import com.TNTStudios.paexium.votacion.VotacionManager;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public class AttackEntityMixin {

    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    private void paexium$onJudgeVote(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity attacker = handler.player;

        if (attacker.getServer() == null || attacker.getWorld().isClient()) return;

        int ronda = VotacionManager.getRondaActual();
        if (ronda <= 0) return;

        // Solo si es juez y en creativo
        if (!esJuez(attacker.getUuid()) || attacker.interactionManager.getGameMode() != GameMode.CREATIVE) return;

        Entity entity = packet.getEntity((ServerWorld) attacker.getWorld());
        if (entity instanceof ServerPlayerEntity victima) {
            if (esJuez(victima.getUuid())) {
                attacker.sendMessage(Text.literal("⚠ No puedes votar a otro juez.").styled(s -> s.withColor(0xFF5555)), false);
                return;
            }

            ItemStack item = attacker.getStackInHand(Hand.MAIN_HAND);
            int puntos = obtenerPuntuacionDePaleta(item);
            if (puntos <= 0) return;

            int parcela = getParcelaDeJugador(victima.getUuid());
            if (parcela == -1) {
                attacker.sendMessage(Text.literal("⚠ El jugador no tiene parcela asignada."), false);
                return;
            }

            VotacionManager.registrarVoto(ronda, parcela, victima.getUuid(), puntos);

            // Eliminar paletas del inventario
            PlayerInventory inv = attacker.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                if (esPaleta(inv.getStack(i))) {
                    inv.setStack(i, ItemStack.EMPTY);
                }
            }

            attacker.sendMessage(Text.literal("✅ Voto de " + puntos + " puntos para "
                    + victima.getName().getString() + " en la parcela " + parcela).styled(s -> s.withColor(0x44FF44)), false);

            ci.cancel(); // Evitar que se procese el ataque como daño
        }
    }

    private boolean esJuez(UUID uuid) {
        try {
            if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("luckperms")) {
                return false;
            }

            // Usamos reflexión para evitar ClassNotFoundException en el cliente
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object provider = providerClass.getMethod("get").invoke(null);
            Object userManager = provider.getClass().getMethod("getUserManager").invoke(provider);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
            if (user == null) return false;

            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Object result = permissionData.getClass()
                    .getMethod("checkPermission", String.class)
                    .invoke(permissionData, "paexium.juez");

            return (boolean) result.getClass().getMethod("asBoolean").invoke(result);

        } catch (Exception e) {
            return false;
        }
    }


    private int getParcelaDeJugador(UUID uuid) {
        Map<UUID, Integer> asignaciones = AsignarParcelasCommand.cargarAsignaciones();
        if (asignaciones == null) return -1;
        return asignaciones.getOrDefault(uuid, -1);
    }

    private boolean esPaleta(ItemStack stack) {
        return stack.isOf(PaexiumItems.PALETA_1)
                || stack.isOf(PaexiumItems.PALETA_2)
                || stack.isOf(PaexiumItems.PALETA_3)
                || stack.isOf(PaexiumItems.PALETA_4)
                || stack.isOf(PaexiumItems.PALETA_5)
                || stack.isOf(PaexiumItems.PALETA_6)
                || stack.isOf(PaexiumItems.PALETA_7)
                || stack.isOf(PaexiumItems.PALETA_8)
                || stack.isOf(PaexiumItems.PALETA_9);
    }

    private int obtenerPuntuacionDePaleta(ItemStack stack) {
        if (stack.isOf(PaexiumItems.PALETA_1)) return 1;
        if (stack.isOf(PaexiumItems.PALETA_2)) return 2;
        if (stack.isOf(PaexiumItems.PALETA_3)) return 3;
        if (stack.isOf(PaexiumItems.PALETA_4)) return 4;
        if (stack.isOf(PaexiumItems.PALETA_5)) return 5;
        if (stack.isOf(PaexiumItems.PALETA_6)) return 6;
        if (stack.isOf(PaexiumItems.PALETA_7)) return 7;
        if (stack.isOf(PaexiumItems.PALETA_8)) return 8;
        if (stack.isOf(PaexiumItems.PALETA_9)) return 9;
        return 0;
    }
}
