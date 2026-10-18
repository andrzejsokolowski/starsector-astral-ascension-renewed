package astralascensionrenewed.campaign;

import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin;

/**
 * The base game's hidden ai_core_admin condition, which decides whether an AI administrator can be
 * unplugged on the spot. When it says no, the administrator picker opens the "RemoveAICoreAdmin"
 * dialog instead - for an Alpha core that is the rogue AI, and in this mod's rules.csv it is the
 * Great Civil War.
 *
 * An Omega core governing a colony past size 10 can never just be unplugged. Below that it can,
 * always. Every other core keeps the base game's behaviour.
 */
public class OmegaAwareAICoreAdmin extends AICoreAdmin {

    @Override
    public boolean canRemove() {
        if (market != null && Ascension.isOmegaGoverned(market)) {
            return !Ascension.needsOmega(market);
        }
        return super.canRemove();
    }
}
