package com.TNTStudios.paexium.parcelas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class RondaManager {

    private static final File file = new File("config/paexium/rondas.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Integer, RondaData> rondas = new HashMap<>();

    public static void guardarRonda(int numero, int participantes, int eliminados, int porParcela, int duracion) {
        rondas.put(numero, new RondaData(participantes, eliminados, porParcela, duracion));
        guardarArchivo();
    }

    private static void guardarArchivo() {
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(rondas, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cargar() {
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Map<Integer, RondaData> cargado = gson.fromJson(reader,
                    new com.google.gson.reflect.TypeToken<Map<Integer, RondaData>>() {}.getType());
            rondas.clear();
            rondas.putAll(cargado);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, RondaData> obtenerRondas() {
        return rondas;
    }

    public static class RondaData {
        public int participantes;
        public int eliminados;
        public int porParcela;
        public int duracion;

        public RondaData(int participantes, int eliminados, int porParcela, int duracion) {
            this.participantes = participantes;
            this.eliminados = eliminados;
            this.porParcela = porParcela;
            this.duracion = duracion;
        }
    }

    public static boolean eliminarRonda(int numero) {
        if (!rondas.containsKey(numero)) return false;
        rondas.remove(numero);
        guardarArchivo();
        return true;
    }

}
