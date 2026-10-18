package astralascensionrenewed.conditions;

/** Size 15. The orbital ring around the planet is drawn by {@link astralascensionrenewed.campaign.OrbitalRing}. */
public class CrownedWorld extends AscensionStage {

    @Override
    protected int industries() {
        return 3;
    }

    @Override
    protected float accessibility() {
        return 0.60f;
    }

    @Override
    protected float incomeMult() {
        return 2f;
    }

    @Override
    protected float groundDefenseMult() {
        return 3f;
    }

    @Override
    protected float fleetSize() {
        return 1f;
    }

    @Override
    protected float hazard() {
        return -0.75f;
    }

    /** The shipyards on the ring. */
    @Override
    protected float shipQuality() {
        return 0.25f;
    }
}
