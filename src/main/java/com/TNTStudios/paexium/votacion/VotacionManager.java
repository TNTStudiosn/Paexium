package com.TNTStudios.paexium.votacion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estructura interna:
 * {
 *   "1": { // Ronda 1
 *     "1": { // Parcela 1
 *       "total": 11,
 *       "jugadores": {
 *         "uuidPaco": 10,
 *         "uuidJuan": 1
 *       }
 *     },
 *     "2": {
 *       "total": 5,
 *       "jugadores": {
 *         "uuidMiguel": 5
 *       }
 *     }
 *   },
 *   "2": { ... },
 *   "3": { ... },
 *   "4": { ... }
 * }
 */
public class VotacionManager {

    private static final File file = new File("config/paexium/votaciones.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // { rondaStr -> { parcelaStr -> InfoParcela } }
    private static final Map<String, Map<String, InfoParcela>> data = new HashMap<>();

    // Ronda actual a la que se le suman votos
    private static int rondaActual = 0;

    public static void setRondaActual(int ronda) {
        rondaActual = ronda;
    }
    public static int getRondaActual() {
        return rondaActual;
    }

    /**
     * Llamar cuando se inicie la votación (ej: /iniciovotacion 1).
     * Deja todo listo para esa ronda en la data.
     */
    public static void iniciarVotacion(int ronda) {
        cargar();
        rondaActual = ronda;
        String rKey = String.valueOf(ronda);
        if (!data.containsKey(rKey)) {
            data.put(rKey, new HashMap<>());
        }
        guardar();
    }

    /**
     * Registrar un voto a x jugador, en la parcela dada, con 'puntos'.
     */
    public static void registrarVoto(int ronda, int parcela, UUID uuidVictima, int puntos) {
        cargar();

        String rKey = String.valueOf(ronda);
        data.computeIfAbsent(rKey, k -> new HashMap<>()); // Asegura que la ronda exista

        Map<String, InfoParcela> rondaMap = data.get(rKey);

        String parcelaStr = String.valueOf(parcela);
        InfoParcela info = rondaMap.get(parcelaStr);
        if (info == null) {
            info = new InfoParcela();
            rondaMap.put(parcelaStr, info);
        }

        // Sumar al total de la parcela
        info.total += puntos;

        // Sumar al jugador
        int actual = info.jugadores.getOrDefault(uuidVictima, 0);
        info.jugadores.put(uuidVictima, actual + puntos);

        guardar();
    }

    /**
     * Devuelve la info de una parcela en una ronda, o null si no existe.
     */
    public static InfoParcela getInfo(int ronda, int parcela) {
        cargar();
        String rKey = String.valueOf(ronda);
        if (!data.containsKey(rKey)) return null;
        return data.get(rKey).get(String.valueOf(parcela));
    }

    private static void guardar() {
        try {
            file.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(file)) {
                gson.toJson(data, w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cargar() {
        if (!file.exists()) return;
        try (FileReader r = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Map<String, InfoParcela>>>(){}.getType();
            Map<String, Map<String, InfoParcela>> loaded = gson.fromJson(r, type);
            data.clear();
            data.putAll(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clase interna para almacenar datos de la parcela.
     */
    public static class InfoParcela {
        public int total = 0;
        public Map<UUID, Integer> jugadores = new HashMap<>();
    }
}
