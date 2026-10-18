package astralascensionrenewed.conditions;

/** Sizes 11 and 12. */
public class ArcologyWorld extends AscensionStage {

    @Override
    protected int industries() {
        return 1;
    }

    @Override
    protected float accessibility() {
        return 0.20f;
    }

    @Override
    protected float incomeMult() {
        return 1.25f;
    }

    @Override
    protected float groundDefenseMult() {
        return 1.5f;
    }

    @Override
    protected float fleetSize() {
        return 0.25f;
    }

    @Override
    protected float hazard() {
        return -0.25f;
    }
}
