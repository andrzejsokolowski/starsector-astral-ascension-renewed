package astralascensionrenewed.campaign;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreAdminPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

/** Registered on every game load; transient, so it never reaches a save. */
public class AarCampaignPlugin extends BaseCampaignPlugin {

    public static final String ID = "astral_ascension_renewed";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public PluginPick<AICoreAdminPlugin> pickAICoreAdminPlugin(String commodityId) {
        if (Commodities.OMEGA_CORE.equals(commodityId)) {
            return new PluginPick<AICoreAdminPlugin>(new OmegaAdminPlugin(), PickPriority.MOD_SET);
        }
        return null;
    }
}
