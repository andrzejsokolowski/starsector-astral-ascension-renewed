package astralascensionrenewed;

import lunalib.lunaSettings.LunaSettings;

/** LunaLib values, read live. A missing value falls back to the default in LunaSettings.csv. */
public final class Settings {

    public static final String MOD_ID = "astral_ascension_renewed";

    static final int DEFAULT_BASE_MAX_SIZE = 10;
    static final float DEFAULT_GROWTH_STRAIN = 1.5f;
    static final boolean DEFAULT_CAP_TOOLBOX = true;
    static final boolean DEFAULT_DROPS_ENABLED = true;
    static final float DEFAULT_DROP_CHANCE = 0.33f;

    private Settings() {}

    /** How big a colony grows without an Omega administrator. */
    public static int baseMaxSize() {
        Integer value = LunaSettings.getInt(MOD_ID, "aar_base_max_size");
        if (value == null) return DEFAULT_BASE_MAX_SIZE;
        return Math.max(6, Math.min(10, value));
    }

    /** Each size past 10 takes this much longer to reach than it normally would, compounding. */
    public static float growthStrain() {
        Double value = LunaSettings.getDouble(MOD_ID, "aar_growth_strain");
        if (value == null || value.isNaN()) return DEFAULT_GROWTH_STRAIN;
        return (float) Math.max(1d, Math.min(3d, value));
    }

    public static boolean capToolboxScaling() {
        Boolean value = LunaSettings.getBoolean(MOD_ID, "aar_tot_cap");
        return value == null ? DEFAULT_CAP_TOOLBOX : value;
    }

    static boolean dropsEnabled() {
        Boolean value = LunaSettings.getBoolean(MOD_ID, "aar_drops_enabled");
        return value == null ? DEFAULT_DROPS_ENABLED : value;
    }

    static float dropChance() {
        Double value = LunaSettings.getDouble(MOD_ID, "aar_drop_chance");
        if (value == null || value.isNaN()) return DEFAULT_DROP_CHANCE;
        return (float) Math.max(0d, Math.min(1d, value));
    }
}
