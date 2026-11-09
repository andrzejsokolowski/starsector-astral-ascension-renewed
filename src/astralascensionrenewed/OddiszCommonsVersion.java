package astralascensionrenewed;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;

/**
 * The game can only ask for one exact version of a required mod: it turns this mod off when the
 * installed OddiszCommons is any other version. So mod_info.json names OddiszCommons without a
 * version, and the lowest version that has the shared ledger is checked here instead.
 */
final class OddiszCommonsVersion {

    private static final String ID = "oddisz_commons";
    private static final int MIN_MAJOR = 0;
    private static final int MIN_MINOR = 2;
    private static final String TOPIC = "https://fractalsoftworks.com/forum/index.php?topic=36282";

    private OddiszCommonsVersion() {}

    /** Stops the game at start with a clear message when OddiszCommons is older than 0.2.0. */
    static void require() {
        ModSpecAPI spec = Global.getSettings().getModManager().getModSpec(ID);
        if (spec == null) return; // The launcher already refuses to start without it.
        if (atLeast(spec.getVersion(), MIN_MAJOR, MIN_MINOR)) return;
        throw new RuntimeException("Astral Ascension Renewed needs OddiszCommons " + MIN_MAJOR + "." + MIN_MINOR
                + ".0 or newer, but " + spec.getVersion() + " is installed. Update it: " + TOPIC);
    }

    /**
     * Reads the version text itself ("0.2.1"). The game's own split cannot be used: it reads a
     * version that starts with 0 as major "0.2", minor "1".
     */
    static boolean atLeast(String version, int major, int minor) {
        if (version == null) return true;
        String[] parts = version.trim().split("[.]");
        Integer haveMajor = number(parts[0]);
        Integer haveMinor = parts.length > 1 ? number(parts[1]) : Integer.valueOf(0);
        // A version this check cannot read is let through rather than blocking the game.
        if (haveMajor == null || haveMinor == null) return true;
        if (haveMajor.intValue() != major) return haveMajor.intValue() > major;
        return haveMinor.intValue() >= minor;
    }

    private static Integer number(String text) {
        int end = 0;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        if (end == 0) return null;
        try {
            return Integer.valueOf(text.substring(0, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
