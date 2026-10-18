package astralascensionrenewed.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreAdminPlugin;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;

import astralascensionrenewed.skills.AcumeniousOracle;
import astralascensionrenewed.skills.DelphicOptimiser;
import astralascensionrenewed.skills.EtherealEnlightenment;

/**
 * Makes the Omega core a colony administrator. The game's administrator picker lists every AI core
 * in your cargo or storage that some plugin can turn into a person; this is that plugin for Omega.
 */
public class OmegaAdminPlugin implements AICoreAdminPlugin {

    @Override
    public PersonAPI createPerson(String aiCoreId, String factionId, long seed) {
        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);
        person.setName(new FullName("Omega Core", "", Gender.ANY));
        person.setPortraitSprite("graphics/portraits/characters/omega.png");
        person.setRankId(null);
        person.setPostId(Ranks.POST_ADMINISTRATOR);
        person.getStats().setSkillLevel(EtherealEnlightenment.ID, 1f);
        person.getStats().setSkillLevel(AcumeniousOracle.ID, 1f);
        person.getStats().setSkillLevel(DelphicOptimiser.ID, 1f);
        return person;
    }
}
