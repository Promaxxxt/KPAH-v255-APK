import java.util.Vector;

/**
 * Fishing hub UI using the same client-first interaction pattern as
 * LocalFarmHub258. No NPC round-trip is required to open the menu/shop.
 */
public final class LocalFishingHub258 {
    public static boolean active;
    private static final int FISHER_NPC = 2000000430;
    private static final short[] SHOP_IDS = new short[] {
        442, 443, 445, 446, 447, 448, 458, 459, 460
    };

    private LocalFishingHub258() {}

    private static void syncMap13() {
        try {
            offline.server.FishingProtocol.clientEnteredMap(13, ae.c, 0, 0);
        } catch (Throwable ignored) {}
    }

    public static void select() {
        if (!active) return;
        syncMap13();
        try {
            Vector menu = new Vector();
            menu.addElement(new by("Cửa hàng câu cá", new LocalFishingAction258(1)));
            menu.addElement(new by("Bán cá", new LocalFishingAction258(2)));
            menu.addElement(new by("Hướng dẫn", new LocalFishingAction258(3)));
            menu.addElement(new by("Thoát", new LocalFishingAction258(4)));
            le.a().a(menu, 0);
        } catch (Throwable e) {
            try { main.a.h(); } catch (Throwable ignored) {}
            try { main.a.b("Câu cá: " + e.toString()); } catch (Throwable ignored) {}
        }
    }

    static void openShop() {
        syncMap13();
        try {
            main.a.h();
            // Same native call used after the normal OPEN_SHOP (-49) handler.
            ae.b().a((byte) 0, 10, "Câu cá", SHOP_IDS, -1, null);
        } catch (Throwable e) {
            try { main.a.h(); } catch (Throwable ignored) {}
            try { main.a.b("Không thể mở cửa hàng câu cá: " + e.toString()); } catch (Throwable ignored) {}
        }
    }

    static void sellFish() {
        syncMap13();
        try {
            main.a.h();
            cx.a().a(FISHER_NPC, (byte) 0, 1);
        } catch (Throwable e) {
            try { main.a.b("Không thể bán cá: " + e.toString()); } catch (Throwable ignored) {}
        }
    }

    static void guide() {
        try {
            main.a.h();
            main.a.b("Mua cần, vé và mồi tại Cửa hàng câu cá. Sau đó vào khu cá phù hợp để bắt đầu câu.");
        } catch (Throwable ignored) {}
    }

    public static void exit() {
        active = false;
    }
}
