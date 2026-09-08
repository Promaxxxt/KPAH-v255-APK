package offline.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Random;
import javax.microedition.rms.RecordStore;

/**
 * V122-compatible fishing implementation reconstructed from the authoritative
 * Avatar server. Native command payloads are preserved; fishing inventory and
 * the currently worn rod share one persisted RMS state.
 */
public final class FishingProtocol {
    private static final int FISHER_NPC = 2000000430;
    private static final String STORE = "av258_fishing_source_v127";
    private static final int MAGIC = 0x46313237;
    private static final Hashtable STATES = new Hashtable();
    private static final Random RNG = new Random();
    private static final short FIRST_ID = 442;
    private static final short LAST_ID = 460;
    private static final short[] SHOP_IDS = {442,443,445,446,447,448,458,459,460};
    private static final int[] SHOP_XU = {10000,5,-1,-1,20,30,1000,10000,-1};
    private static final int[] SHOP_LUONG = {-1,-1,25,100,-1,-1,-1,-1,2};
    private static final short[] FISH_IDS = {444,449,450,451,452,453,454,455,456,457};
    private static final int[] FISH_SELL = {200,500,1000,2000,4000,8000,15000,30000,60000,500000};

    private FishingProtocol() {}

    public static boolean handle(PacketCodec codec, DataOutputStream out, PlayerState player,
                                 WorldState world, ResourceStore resources, ClientHooks hooks,
                                 Packet packet) throws IOException {
        int cmd = packet.command;
        if (cmd == 50) {
            int map = requestedMap(packet.data, world.defaultMapId);
            if (map >= 13 && map <= 16) {
                sendFishingJoin(codec, out, player, packet.data, map);
                return true;
            }
            return false;
        }
        if (cmd == -61 && player.mapId == 13 && firstInt(packet.data, -1) == FISHER_NPC) {
            sendFisherMenu(codec, out); return true;
        }
        if (cmd == -59 && player.mapId == 13 && firstInt(packet.data, -1) == FISHER_NPC) {
            handleFisherMenu(codec, out, player, packet.data); return true;
        }
        if (cmd == -24 && player.mapId == 13 && isFishingShopPurchase(packet.data)) {
            handleFishingShopPurchase(codec, out, player, packet.data); return true;
        }
        if (cmd == -47 && player.mapId >= 13 && player.mapId <= 16) {
            sendFishingInventory(codec, out, player); return true;
        }
        if (cmd == 86 && isFishingArea(player.mapId)) { startFishing(codec, out, player); return true; }
        if (cmd == 82 && isFishingArea(player.mapId)) { castBait(codec, out, player, hooks); return true; }
        if (cmd == 84 && isFishingArea(player.mapId)) { handleFishing(codec, out, player, packet.data); return true; }
        if (cmd == 85 && isFishingArea(player.mapId)) { fishingFinished(codec, out, player); return true; }
        if (cmd == 87 && player.mapId >= 13 && player.mapId <= 16) { sendStatus(codec, out, player); return true; }
        if (cmd == 88 && player.mapId >= 13 && player.mapId <= 16) { sendInfo(codec, out, player); return true; }
        return false;
    }

    private static boolean isFishingArea(int map) { return map >= 14 && map <= 16; }

    private static void startFishing(PacketCodec codec, DataOutputStream out, PlayerState player) throws IOException {
        State s = getState(player);
        boolean ok = true;
        String text = "Bắt đầu câu cá";
        if (!validRod(player.mapId, s.wornRod)) {
            ok = false;
            if (player.mapId == 14) text = "Bạn cần mặc cần câu tre, sắt hoặc VIP.";
            else if (player.mapId == 15) text = "Bạn cần mặc cần câu sắt hoặc VIP.";
            else text = "Bạn cần mặc cần câu VIP.";
        } else if (s.get(ticketForMap(player.mapId)) <= 0) {
            ok = false; text = "Bạn chưa có vé câu cá phù hợp.";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos);
        ds.writeBoolean(ok); ds.writeUTF(text); ds.flush();
        send(codec, out, 86, bos.toByteArray());
    }

    private static void castBait(final PacketCodec codec, final DataOutputStream out,
                                 final PlayerState player, final ClientHooks hooks) throws IOException {
        final State s = getState(player);
        int bait = baitForMap(player.mapId);
        if (s.get(bait) <= 0) {
            fishingFinished(codec, out, player);
            sendDialog(codec, out, "Bạn chưa có mồi câu phù hợp.");
            return;
        }
        if (!validRod(player.mapId, s.wornRod) || s.get(ticketForMap(player.mapId)) <= 0) {
            fishingFinished(codec, out, player);
            sendDialog(codec, out, "Hãy kiểm tra cần câu và vé.");
            return;
        }
        s.add(bait, -1);
        s.pendingFish = rollFish(player.mapId);
        s.arrows = makeDirections(4 + RNG.nextInt(7));
        saveState(player, s);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos);
        ds.writeInt(player.id); ds.flush();
        send(codec, out, 82, bos.toByteArray());
        final int delay = 8000 + RNG.nextInt(6) * 1000;
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(delay); sendFishMiniGame(codec, out, player, hooks, s); }
                catch (Throwable ignored) {}
            }
        }).start();
    }

    private static void sendFishMiniGame(PacketCodec codec, DataOutputStream out, PlayerState player,
                                         ClientHooks hooks, State s) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos);
        ds.writeInt(player.id);
        ds.writeShort(s.pendingFish);
        ds.writeShort(s.pendingFish < 0 ? -1 : 3000);
        ds.writeByte(s.arrows.length);
        for (int i = 0; i < s.arrows.length; i++) {
            byte[] image = readArrow(directionImageIndex(s.arrows[i], i), hooks);
            if (image == null) image = new byte[0];
            ds.writeShort(image.length); ds.write(image);
        }
        ds.flush(); send(codec, out, 91, bos.toByteArray());
    }

    private static void handleFishing(PacketCodec codec, DataOutputStream out, PlayerState player,
                                      byte[] data) throws IOException {
        State s = getState(player);
        short fish = s.pendingFish;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            in.readBoolean();
            int length = in.readUnsignedByte();
            if (length != s.arrows.length) fish = -1;
            for (int i = 0; i < length; i++) {
                byte d = in.readByte();
                if (i >= s.arrows.length || d != s.arrows[i]) fish = -1;
            }
        } catch (Throwable e) { fish = -1; }
        s.pendingFish = fish;
        if (fish > 0) {
            s.add(fish, 1); s.totalCaught++; s.points += fishPoints(fish); saveState(player, s);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos);
        ds.writeInt(player.id); ds.writeShort(fish); ds.flush();
        send(codec, out, 84, bos.toByteArray());
        if (fish > 0) sendFishingInventory(codec, out, player);
        if (fish == 457) sendDialog(codec, out, "Chúc mừng! Bạn vừa câu được Cá Mập!");
    }

    private static void fishingFinished(PacketCodec codec, DataOutputStream out, PlayerState player) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos); ds.writeInt(player.id); ds.flush();
        send(codec, out, 85, bos.toByteArray());
    }

    private static void sendStatus(PacketCodec codec, DataOutputStream out, PlayerState player) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos); ds.writeInt(player.id); ds.writeByte(1); ds.flush();
        send(codec, out, 87, bos.toByteArray());
    }

    private static void sendInfo(PacketCodec codec, DataOutputStream out, PlayerState player) throws IOException {
        State s = getState(player);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos);
        ds.writeInt(player.id); ds.writeByte(1); ds.writeByte(1); ds.writeInt(s.points); ds.writeShort(bestFish(s)); ds.flush();
        send(codec, out, 88, bos.toByteArray());
    }

    private static void sendFisherMenu(PacketCodec codec, DataOutputStream out) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos);
        ds.writeInt(FISHER_NPC); ds.writeByte(0); ds.writeByte(4);
        ds.writeUTF("Câu cá"); ds.writeUTF("Bán cá"); ds.writeUTF("Xem hướng dẫn"); ds.writeUTF("Thoát");
        ds.flush(); send(codec, out, -59, bos.toByteArray());
    }

    private static void handleFisherMenu(PacketCodec codec, DataOutputStream out, PlayerState player,
                                         byte[] data) throws IOException {
        int select = -1;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            in.readInt(); if (in.available() > 0) in.readByte(); if (in.available() > 0) select = in.readUnsignedByte();
        } catch (Throwable ignored) {}
        if (select == 0) sendFishingShop(codec, out);
        else if (select == 1) sellFish(codec, out, player);
        else if (select == 2) sendDialog(codec, out, "Mua cần, vé và mồi tại Thợ Câu. Vào khu cá phù hợp và làm đúng chuỗi mũi tên.");
    }

    private static void sendFishingShop(PacketCodec codec, DataOutputStream out) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos);
        ds.writeByte(10); ds.writeUTF("Câu cá"); ds.writeShort(SHOP_IDS.length);
        for (int i = 0; i < SHOP_IDS.length; i++) ds.writeShort(SHOP_IDS[i]);
        ds.flush(); send(codec, out, -49, bos.toByteArray());
    }

    private static boolean isFishingShopPurchase(byte[] data) {
        if (data == null || data.length < 3) return false;
        int id = ((data[0] & 255) << 8) | (data[1] & 255);
        for (int i = 0; i < SHOP_IDS.length; i++) if (SHOP_IDS[i] == id) return true;
        return false;
    }

    private static void handleFishingShopPurchase(PacketCodec codec, DataOutputStream out,
                                                  PlayerState player, byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        short id = in.readShort(); byte type = in.readByte();
        int index = shopIndex(id);
        if (index < 0 || (type != 1 && type != 2)) { sendDialog(codec, out, "Vật phẩm câu cá không hợp lệ."); return; }
        State s = getState(player);
        int price = type == 1 ? SHOP_XU[index] : SHOP_LUONG[index];
        if (price < 0) { sendDialog(codec, out, "Vật phẩm không bán bằng loại tiền này."); return; }
        if (type == 1) {
            if (player.money < price) { sendDialog(codec, out, "Bạn không đủ xu."); return; }
            player.money -= price;
        } else {
            if (s.gold < price) { sendDialog(codec, out, "Bạn không đủ lượng."); return; }
            s.gold -= price;
        }
        if (id == 442 || id == 445 || id == 446) {
            s.set(id, 1); s.wornRod = id; sendUsingPart(codec, out, player.id, id);
        } else s.add(id, 1);
        saveState(player, s);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bos);
        ds.writeShort(id); ds.writeInt(price); ds.writeByte(type); ds.writeUTF("Bạn đã mua vật phẩm thành công.");
        ds.writeInt(player.money); ds.writeInt(s.gold); ds.writeInt(0); ds.flush();
        send(codec, out, -24, bos.toByteArray());
        sendMoney(codec, out, player, s); sendFishingInventory(codec, out, player);
    }

    private static void sendUsingPart(PacketCodec codec, DataOutputStream out, int userId, short itemId) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(bos);
        ds.writeInt(userId); ds.writeShort(itemId); ds.flush(); send(codec, out, -48, bos.toByteArray());
    }

    private static void sellFish(PacketCodec codec, DataOutputStream out, PlayerState player) throws IOException {
        State s = getState(player); int count = 0; long total = 0;
        for (int i = 0; i < FISH_IDS.length; i++) {
            int qty = s.get(FISH_IDS[i]);
            if (qty > 0) { count += qty; total += (long) qty * FISH_SELL[i]; s.set(FISH_IDS[i], 0); }
        }
        if (count == 0) { sendDialog(codec, out, "Bạn không có cá để bán."); return; }
        long next = (long) player.money + total; player.money = next > 2147483647L ? 2147483647 : (int) next;
        s.revenue += total; saveState(player, s); sendMoney(codec, out, player, s); sendFishingInventory(codec, out, player);
        sendDialog(codec, out, "Bạn đã bán " + count + " con cá và nhận " + total + " xu.");
    }

    private static void sendFishingInventory(PacketCodec codec, DataOutputStream out, PlayerState player) throws IOException {
        State s = getState(player); int count = 3;
        for (int id = FIRST_ID; id <= LAST_ID; id++) if (s.get(id) > 0) count++;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(bos);
        ds.writeShort(count); writeChest(ds,0,1,"Áo Offline"); writeChest(ds,1,1,"Quần Offline"); writeChest(ds,2,1,"Nón Offline");
        for (int id = FIRST_ID; id <= LAST_ID; id++) { int qty = s.get(id); if (qty > 0) writeChest(ds,id,0,"Số lượng: " + qty); }
        ds.flush(); send(codec, out, -47, bos.toByteArray());
    }

    private static void writeChest(DataOutputStream ds, int id, int type, String text) throws IOException {
        ds.writeShort(id); ds.writeByte(type); ds.writeUTF(text);
    }

    private static void sendFishingJoin(PacketCodec codec, DataOutputStream out, PlayerState player,
                                        byte[] request, int map) throws IOException {
        int zone = 0;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(request));
            if (in.available() >= 2) { in.readByte(); int z = in.readByte(); if (z >= 0) zone = z; }
        } catch (Throwable ignored) {}
        player.mapId = map; player.zone = zone; player.x = map == 16 ? 300 : 120; player.y = 90;
        NpcState[] npcs = map == 13
                ? new NpcState[]{new NpcState(FISHER_NPC,"tho.cau",new short[]{3387,3386,0,82,3385,3384,442},326,64,(byte)0,(byte)0,(short)0,(short)0)}
                : new NpcState[0];
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(bos);
        ds.writeByte(player.mapId); ds.writeByte(player.zone); ds.writeShort(player.x); ds.writeShort(player.y); ds.writeByte(npcs.length);
        for (int i = 0; i < npcs.length; i++) {
            NpcState n = npcs[i]; ds.writeInt(n.id); ds.writeUTF(n.name); ds.writeByte(n.parts.length);
            for (int j = 0; j < n.parts.length; j++) ds.writeShort(n.parts[j]);
            ds.writeShort(n.x); ds.writeShort(n.y); ds.writeByte(n.role);
        }
        for (int i = 0; i < npcs.length; i++) ds.writeByte(npcs[i].direct);
        for (int i = 0; i < npcs.length; i++) ds.writeByte(0);
        for (int i = 0; i < npcs.length; i++) ds.writeShort(npcs[i].clanIcon);
        ds.writeByte(0); ds.writeByte(0); ds.writeShort(0); ds.writeByte(0); ds.writeByte(0);
        for (int i = 0; i < npcs.length; i++) ds.writeShort(npcs[i].iconWedding);
        ds.flush(); send(codec, out, 50, bos.toByteArray());
    }

    private static int requestedMap(byte[] data, int fallback) { return data == null || data.length == 0 ? fallback : (data[0] < 0 ? data[0] + 256 : data[0]); }
    private static int firstInt(byte[] d, int f) { return d == null || d.length < 4 ? f : ((d[0]&255)<<24)|((d[1]&255)<<16)|((d[2]&255)<<8)|(d[3]&255); }
    private static int baitForMap(int map) { return map == 14 ? 443 : map == 15 ? 447 : 448; }
    private static int ticketForMap(int map) { return map == 14 ? 458 : map == 15 ? 459 : 460; }
    private static boolean validRod(int map, short rod) { return map == 14 ? rod==442||rod==445||rod==446 : map == 15 ? rod==445||rod==446 : map==16&&rod==446; }

    private static short rollFish(int map) {
        int r = RNG.nextInt(100);
        if (map == 14) { if(r<50)return -1; if(r<67)return 444; if(r<84)return 449; return 450; }
        if (map == 15) { if(r<50)return -1; if(r<67)return 451; if(r<84)return 452; return 453; }
        if(r<48)return -1; if(r<64)return 454; if(r<81)return 455; if(r<99)return 456; return 457;
    }

    private static int fishPoints(int id) {
        if(id==444||id==449||id==450)return 1; if(id==451||id==452||id==453)return 2;
        if(id==454||id==455||id==456)return 3; return id==457?10:0;
    }

    private static short bestFish(State s) { for(int i=FISH_IDS.length-1;i>=0;i--) if(s.get(FISH_IDS[i])>0)return FISH_IDS[i]; return -1; }
    private static byte[] makeDirections(int count) { byte[] a=new byte[count]; for(int i=0;i<count;i++)a[i]=(byte)(1+RNG.nextInt(4)); return a; }
    private static int directionImageIndex(byte d,int step) { return ((d-1)&3)+4*(step%9); }

    private static byte[] readArrow(int index, ClientHooks hooks) {
        boolean hd=false; try { hd=hooks!=null&&hooks.getPixelScale()>1; } catch(Throwable ignored) {}
        String path=(hd?"/offline/arrows_hd/":"/offline/arrows_medium/")+index+".png"; InputStream in=null;
        try {
            in=FishingProtocol.class.getResourceAsStream(path); if(in==null)return null;
            ByteArrayOutputStream bos=new ByteArrayOutputStream(); byte[] buf=new byte[256]; int n;
            while((n=in.read(buf))>0)bos.write(buf,0,n); return bos.toByteArray();
        } catch(Throwable ignored) { return null; }
        finally { try { if(in!=null)in.close(); } catch(Throwable ignored) {} }
    }

    private static void sendMoney(PacketCodec codec, DataOutputStream out, PlayerState player, State s) throws IOException {
        ByteArrayOutputStream bos=new ByteArrayOutputStream(); DataOutputStream ds=new DataOutputStream(bos);
        ds.writeInt(0); ds.writeByte(0); ds.writeInt(player.money); ds.writeInt(s.gold); ds.writeInt(0); ds.flush(); send(codec,out,-33,bos.toByteArray());
    }
    private static void sendDialog(PacketCodec codec, DataOutputStream out, String text) throws IOException {
        ByteArrayOutputStream bos=new ByteArrayOutputStream(); DataOutputStream ds=new DataOutputStream(bos); ds.writeUTF(text); ds.flush(); send(codec,out,-10,bos.toByteArray());
    }
    private static void send(PacketCodec codec, DataOutputStream out, int cmd, byte[] data) throws IOException { codec.write(out,(byte)cmd,data,codec.isEncrypted()); out.flush(); }
    private static int shopIndex(short id) { for(int i=0;i<SHOP_IDS.length;i++)if(SHOP_IDS[i]==id)return i; return -1; }
    private static String key(PlayerState p) { return p.name==null?String.valueOf(p.id):p.name; }

    private static State getState(PlayerState p) {
        String k=key(p); State s=(State)STATES.get(k);
        if(s==null){s=load(k);if(s==null)s=new State();STATES.put(k,s);} return s;
    }
    private static void saveState(PlayerState p, State s) { try { save(key(p),s); } catch(Throwable ignored) {} }

    private static byte[] encode(String key, State s) throws IOException {
        ByteArrayOutputStream bos=new ByteArrayOutputStream(); DataOutputStream ds=new DataOutputStream(bos);
        ds.writeInt(MAGIC); ds.writeUTF(key); ds.writeByte(2); for(int i=0;i<s.qty.length;i++)ds.writeInt(s.qty[i]);
        ds.writeInt(s.totalCaught);ds.writeInt(s.points);ds.writeLong(s.revenue);ds.writeInt(s.gold);ds.writeShort(s.wornRod);ds.flush();return bos.toByteArray();
    }
    private static State decode(byte[] data,String key) {
        try { DataInputStream in=new DataInputStream(new ByteArrayInputStream(data)); if(in.readInt()!=MAGIC)return null; if(!key.equals(in.readUTF()))return null;
            in.readByte();State s=new State();for(int i=0;i<s.qty.length;i++)s.qty[i]=in.readInt();s.totalCaught=in.readInt();s.points=in.readInt();s.revenue=in.readLong();
            if(in.available()>=4)s.gold=in.readInt();if(in.available()>=2)s.wornRod=in.readShort();return s; } catch(Throwable ignored){return null;}
    }
    private static State load(String key) {
        RecordStore rs=null;try {rs=RecordStore.openRecordStore(STORE,true);int next=rs.getNextRecordID();for(int id=1;id<next;id++){byte[] d=null;try{d=rs.getRecord(id);}catch(Throwable ignored){}if(d==null)continue;State s=decode(d,key);if(s!=null)return s;}}
        catch(Throwable ignored){}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Throwable ignored){}}return null;
    }
    private static void save(String key,State s) {
        RecordStore rs=null;try {byte[] encoded=encode(key,s);rs=RecordStore.openRecordStore(STORE,true);int next=rs.getNextRecordID();for(int id=1;id<next;id++){byte[] old=null;try{old=rs.getRecord(id);}catch(Throwable ignored){}if(old==null)continue;
            try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(old));if(in.readInt()==MAGIC&&key.equals(in.readUTF())){rs.setRecord(id,encoded,0,encoded.length);return;}}catch(Throwable ignored){}}
            rs.addRecord(encoded,0,encoded.length);}catch(Throwable ignored){}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Throwable ignored){}}
    }

    static final class State {
        final int[] qty=new int[LAST_ID-FIRST_ID+1]; int totalCaught; int points; long revenue; int gold=500; short wornRod=-1; short pendingFish=-1; byte[] arrows=new byte[0];
        int get(int id){int i=id-FIRST_ID;return i>=0&&i<qty.length?qty[i]:0;}
        void set(int id,int value){int i=id-FIRST_ID;if(i>=0&&i<qty.length)qty[i]=value<0?0:value;}
        void add(int id,int delta){set(id,get(id)+delta);}
    }
}
