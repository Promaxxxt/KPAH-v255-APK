import java.util.Vector;

/**
 * V122-style local park entry. Fishing UI is hooked separately through hr,
 * exactly like LocalFarmHub258, so this class does not manufacture an NPC.
 */
public final class LocalPark258 {
    private static boolean entering;

    private LocalPark258() {}

    private static int defaultX(int map) {
        switch (map) {
            case 0: return 92;
            case 1: return 165;
            case 2: return 204;
            case 3: return 288;
            case 5: return 188;
            case 7: return 130;
            case 8: return 264;
            case 9: return 258;
            case 11: return 120;
            case 13: return 180;
            case 17: return 150;
            case 20: return 150;
            case 23: return 646;
            default: return 160;
        }
    }

    private static int defaultY(int map) {
        switch (map) {
            case 0: return 81;
            case 1: return 100;
            case 2: return 85;
            case 3: return 97;
            case 5: return 89;
            case 7: return 48;
            case 8: return 69;
            case 9: return 156;
            case 11: return 47;
            case 13: return 80;
            case 17: return 80;
            case 20: return 80;
            case 23: return 96;
            default: return 80;
        }
    }

    public static void enter(int requestedMap, int requestedZone) {
        if (entering) return;
        entering = true;
        try {
            main.a.h();
            int map = requestedMap;
            try {
                if (main.a.r == ea.b() && ea.b().e == 5) map = 11;
            } catch (Throwable ignored) {}

            hn me = main.GameMidlet.i;
            if (me == null) return;

            eo.a();
            main.GameMidlet.e = 9;
            LocalFarmHub258.active = false;

            int zone = requestedZone < 0 ? 0 : requestedZone;
            int x = ir.B >= 0 ? ir.B : defaultX(map);
            int y = ir.C >= 0 ? ir.C : defaultY(map);
            ir.B = -1;
            ir.C = -1;
            ir.H = -1;
            ir.I = -1;
            ir.t = -1;
            ir.y = -1;

            me.aw = x;
            me.ax = y;
            me.C = x;
            me.D = y;
            me.L = false;
            me.h();

            Vector users = new Vector();
            users.addElement(me);
            Vector v2 = new Vector();
            Vector v3 = new Vector();
            ae.b().a((byte) map, (byte) zone, (short) x, (short) y, users, v2, v3);
            if (ir.m != null && !ir.m.contains(me)) ir.b(me);
        } catch (Throwable ignored) {
        } finally {
            entering = false;
            try { main.a.h(); } catch (Throwable ignored) {}
        }
    }
}
