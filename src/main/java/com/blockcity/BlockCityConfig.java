package com.blockcity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Config JSON in <game dir>/config/blockcity.json (se creeaza automat la prima pornire). */
public class BlockCityConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BlockCityConfig instance = new BlockCityConfig();

    // --- Masini (simulate pe clientul soferului) ---
    public double carAcceleration = 0.022;   // blocuri/tick^2
    public double carBrake = 0.05;
    public double carMaxSpeed = 1.1;         // blocuri/tick (1.0 = 72 km/h)
    public double carReverseFraction = 0.3;  // viteza maxima la marsarier, fractie din max
    public double carTurnRate = 4.5;         // grade/tick la viraj complet
    public float carHealth = 100f;
    public boolean carRunOverDamage = true;

    // --- Arme ---
    public float pistolDamage = 6f;
    public double pistolRange = 64;
    public int pistolCooldownTicks = 6;

    // --- Wanted / politie ---
    public boolean wantedEnabled = true;
    public int heatDecayDelayTicks = 600;    // cate tick-uri fara crime inainte sa scada wanted
    public int maxPolice = 8;
    public int policeSpawnIntervalTicks = 100;
    public float policeDamage = 2f;

    // --- Pietoni ---
    public boolean pedestriansEnabled = true;
    public int maxPedestriansNearPlayer = 10;

    // --- Client ---
    public boolean thirdPersonOnJoin = true;
    public boolean minimapEnabled = true;
    public int minimapSize = 110;            // pixeli pe ecran
    public double minimapBlocksPerPixel = 1.0;

    // --- Oras ---
    public int citySize = 3;                 // blocuri de oras (NxN) pentru /blockcity city

    public static BlockCityConfig get() { return instance; }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("blockcity.json");
        try {
            if (Files.exists(path)) {
                BlockCityConfig loaded = GSON.fromJson(Files.readString(path), BlockCityConfig.class);
                if (loaded != null) instance = loaded;
            }
            Files.writeString(path, GSON.toJson(instance)); // rescrie ca sa adauge campuri noi
        } catch (Exception e) {
            BlockCityMod.LOGGER.error("Nu pot citi/scrie configul blockcity.json, folosesc valorile implicite", e);
        }
    }
}
