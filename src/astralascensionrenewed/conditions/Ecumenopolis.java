package astralascensionrenewed.conditions;

/** Sizes 13 and 14. */
public class Ecumenopolis extends AscensionStage {

    @Override
    protected int industries() {
        return 2;
    }

    @Override
    protected float accessibility() {
        return 0.40f;
    }

    @Override
    protected float incomeMult() {
        return 1.5f;
    }

    @Override
    protected float groundDefenseMult() {
        return 2f;
    }

    @Override
    protected float fleetSize() {
        return 0.5f;
    }

    @Override
    protected float hazard() {
        return -0.5f;
    }
}
