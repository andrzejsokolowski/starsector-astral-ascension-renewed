package astralascensionrenewed;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import astralascensionrenewed.campaign.AscensionScript;
import astralascensionrenewed.campaign.AwakeningGuard;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

public class ColonialHypergrowthPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        OddiszCommonsVersion.require();
        // LunaLib keeps its listeners for the whole session, so register once.
        if (!LunaSettings.hasSettingsListenerOfClass(SettingsListener.class)) {
            LunaSettings.addSettingsListener(new SettingsListener());
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        applySettings();
        AscensionScript.install();
        if (Global.getSettings().getModManager().isModEnabled(AwakeningGuard.DREAMS_OF_THE_PAST_ID)) {
            AwakeningGuard.install();
        }
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
