import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import javax.microedition.lcdui.Image;

/** Client-side fishing NPC dynamic-part bootstrap for Avatar 2.5.8 offline. */
public final class FishingClientParts258 {
    private static final short[] PART_IDS = {3384, 3385, 3386, 3387};
    private static final short[] IMAGE_IDS = {
        5608, 5609, 5610, 5611, 5612, 5613, 5614, 5615, 5616,
        5617, 5618, 5619, 5620, 5621, 5622, 5623, 5624
    };
    private static boolean prepared;

    private FishingClientParts258() {}

    public static synchronized void prepare() {
        if (prepared) return;
        boolean ok = true;
        for (int i = 0; i < PART_IDS.length; i++) {
            try { preloadPart(PART_IDS[i]); } catch (Throwable ignored) { ok = false; }
        }
        for (int i = 0; i < IMAGE_IDS.length; i++) {
            try { preloadImage(IMAGE_IDS[i]); } catch (Throwable ignored) { ok = false; }
        }
        prepared = ok;
    }

    private static void preloadPart(short requestedId) throws Exception {
        String path = "/offline/part/" + requestedId + ".dat";
        InputStream raw = FishingClientParts258.class.getResourceAsStream(path);
        if (raw == null) throw new Exception("missing part " + requestedId);
        DataInputStream in = new DataInputStream(raw);
        try {
            short id = in.readShort();
            int coin = in.readInt();
            short gold = in.readShort();
            short type = in.readShort();
            if (id != requestedId || type != -1) throw new Exception("bad part " + requestedId);

            fa part = new fa();
            part.g = id;
            part.i[0] = coin;
            part.i[1] = gold;
            part.f = type;
            part.l = in.readUTF();
            part.k = in.readByte();
            part.j = in.readByte();
            part.b = in.readByte();
            part.a = in.readByte();
            part.h = in.readShort();
            part.c = new short[15];
            part.d = new byte[15];
            part.e = new byte[15];
            for (int i = 0; i < 15; i++) {
                part.c[i] = in.readShort();
                part.d[i] = in.readByte();
                part.e[i] = in.readByte();
            }
            fx.j.put(String.valueOf(id), part);
        } finally {
            try { in.close(); } catch (Throwable ignored) {}
        }
    }

    private static void preloadImage(short imageId) throws Exception {
        String key = String.valueOf(imageId);
        String size = gy.Y > 1 ? "hd" : "medium";
        String path = "/offline/item_" + size + "/" + imageId + ".png";
        InputStream in = FishingClientParts258.class.getResourceAsStream(path);
        if (in == null) throw new Exception("missing image " + imageId);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[512];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            byte[] png = bos.toByteArray();
            Image image = Image.createImage(png, 0, png.length);
            if (image == null) throw new Exception("bad image " + imageId);
            fx.i.put(key, new f(image));
        } finally {
            try { in.close(); } catch (Throwable ignored) {}
        }
    }
}
