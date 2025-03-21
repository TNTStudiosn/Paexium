package com.TNTStudios.paexium.parcelas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ParcelManager {

    private static final File file = new File("config/paexium/parcelas.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Integer, Vec3i[]> parcelas = new HashMap<>();

    public static void saveParcela(int id, BlockPos pos1, BlockPos pos2) {
        parcelas.put(id, new Vec3i[]{pos1, pos2});
        saveToFile();
    }

    private static void saveToFile() {
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(parcelas, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<Integer, Vec3i[]>>() {}.getType();
            Map<Integer, Vec3i[]> loaded = gson.fromJson(reader, type);
            parcelas.clear();
            parcelas.putAll(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
