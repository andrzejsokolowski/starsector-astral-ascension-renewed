package astralascensionrenewed;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import astralascensionrenewed.campaign.AscensionScript;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

public class AstralAscensionRenewedPlugin extends BaseModPlugin {

    /** The original. Both mods set the colony size limit and claim the Omega core administrator. */
    static final String ASTRAL_ASCENSION_ID = "Planetace_AstralAscension";

    @Override
    public void onApplicationLoad() {
        if (Global.getSettings().getModManager().isModEnabled(ASTRAL_ASCENSION_ID)) {
            throw new RuntimeException("Astral Ascension Renewed replaces Astral Ascension. "
                    + "Disable one of the two in the launcher.");
        }
        // LunaLib keeps its listeners for the whole session, so register once.
        if (!LunaSettings.hasSettingsListenerOfClass(SettingsListener.class)) {
            LunaSettings.addSettingsListener(new SettingsListener());
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        applySettings();
        AscensionScript.install();
    }

    /** The colony size limit is one number the game reads everywhere, so it is set directly. */
    static void applySettings() {
        Misc.MAX_COLONY_SIZE = Settings.baseMaxSize();
        OmegaDrops.apply();
    }

    /** A changed setting takes effect at once, without reloading the save. */
    private static final class SettingsListener implements LunaSettingsListener {
        @Override
        public void settingsChanged(String modId) {
            if (Settings.MOD_ID.equals(modId) && Global.getSector() != null) applySettings();
        }
    }
}
