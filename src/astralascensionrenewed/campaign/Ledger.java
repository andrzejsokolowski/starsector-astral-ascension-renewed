package astralascensionrenewed.campaign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.StatBonus;

/**
 * Every change this mod makes to the game, with the way to undo it.
 *
 * Each tick re-asserts the changes that should exist. Whatever was not re-asserted (the Omega core
 * was removed, the colony shrank, a setting was switched off) is undone by {@link #sweep()}.
 *
 * Changes to a colony-wide value name their market: the buildings there copied the old value when
 * they were last applied, so the market is reapplied once the tick is over.
 */
final class Ledger {

    private static final class Hold {
        final Runnable release;
        int stamp;

        Hold(Runnable release) {
            this.release = release;
        }
    }

    private final Map<Object, Map<String, Hold>> holds = new IdentityHashMap<>();
    private final Map<MarketAPI, Boolean> dirty = new IdentityHashMap<>();
    private int stamp = 1;

    void mult(MutableStat stat, String id, float value, String desc) {
        mult(stat, id, value, desc, null);
    }

    void mult(MutableStat stat, String id, float value, String desc, MarketAPI market) {
        StatMod before = stat.getMultStatMod(id);
        if (before == null || before.getValue() != value) touch(market);
        stat.modifyMult(id, value, desc);
        keep(stat, id, () -> {
            stat.unmodify(id);
            touch(market);
        });
    }

    void flat(MutableStat stat, String id, float value, String desc) {
        flat(stat, id, value, desc, null);
    }

    void flat(MutableStat stat, String id, float value, String desc, MarketAPI market) {
        StatMod before = stat.getFlatStatMod(id);
        if (before == null ? value != 0f : before.getValue() != value) touch(market);
        stat.modifyFlat(id, value, desc);
        keep(stat, id, () -> {
            stat.unmodify(id);
            touch(market);
        });
    }

    void mult(StatBonus stat, String id, float value, String desc) {
        stat.modifyMult(id, value, desc);
        keep(stat, id, () -> stat.unmodify(id));
    }

    void flat(StatBonus stat, String id, float value, String desc, MarketAPI market) {
        StatMod before = stat.getFlatBonus(id);
        if (before == null ? value != 0f : before.getValue() != value) touch(market);
        stat.modifyFlat(id, value, desc);
        keep(stat, id, () -> {
            stat.unmodify(id);
            touch(market);
        });
    }

    /** A change that is not a stat modifier. {@code release} undoes it. */
    void hold(Object target, String id, Runnable release) {
        keep(target, id, release);
    }

    void touch(MarketAPI market) {
        if (market != null) dirty.put(market, Boolean.TRUE);
    }

    private void keep(Object target, String id, Runnable release) {
        Map<String, Hold> byId = holds.get(target);
        if (byId == null) {
            byId = new HashMap<>();
            holds.put(target, byId);
        }
        Hold hold = byId.get(id);
        if (hold == null) {
            hold = new Hold(release);
            byId.put(id, hold);
        }
        hold.stamp = stamp;
    }

    /** Undoes whatever was not re-asserted since the last sweep, then reapplies the touched colonies. */
    void sweep() {
        release(false);
        stamp++;
    }

    void releaseAll() {
        release(true);
        stamp++;
    }

    private void release(boolean all) {
        List<Runnable> stale = new ArrayList<>();
        Iterator<Map<String, Hold>> targets = holds.values().iterator();
        while (targets.hasNext()) {
            Map<String, Hold> byId = targets.next();
            Iterator<Hold> it = byId.values().iterator();
            while (it.hasNext()) {
                Hold hold = it.next();
                if (all || hold.stamp != stamp) {
                    stale.add(hold.release);
                    it.remove();
                }
            }
            if (byId.isEmpty()) targets.remove();
        }
        for (Runnable release : stale) release.run();

        if (dirty.isEmpty()) return;
        List<MarketAPI> markets = new ArrayList<>(dirty.keySet());
        dirty.clear();
        for (MarketAPI market : markets) market.reapplyIndustries();
    }
}
