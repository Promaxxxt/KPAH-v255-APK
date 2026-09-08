package offline.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** V122 server loop with source-first fishing routed before the farm/avatar handlers. */
public final class AvatarOfflineServer implements Runnable {
    private final InputStream input;
    private final OutputStream output;
    private final ClientHooks hooks;
    private volatile boolean running = true;

    public AvatarOfflineServer(InputStream input, OutputStream output, ClientHooks hooks) {
        this.input = input;
        this.output = output;
        this.hooks = hooks;
    }

    public void stop() {
        running = false;
    }

    public void run() {
        try {
            PacketCodec codec = new PacketCodec((byte) 90);
            PlayerState player = new PlayerState();
            WorldState world = new WorldState();
            ResourceStore resources = new ResourceStore(hooks);
            DataInputStream in = new DataInputStream(input);
            DataOutputStream out = new DataOutputStream(output);
            AvatarProtocol avatar = new AvatarProtocol(codec, out, player, world, resources, hooks);
            while (running) {
                Packet packet = codec.read(in);
                if (packet == null) break;
                if (!FishingProtocol.handle(codec, out, player, world, resources, hooks, packet)
                        && !FarmProtocol.handle(codec, out, player, world, resources, hooks, packet)) {
                    avatar.handle(packet);
                }
            }
        } catch (Throwable t) {
            try {
                System.out.println("[AVATAR OFFLINE SERVER] stopped");
                t.printStackTrace();
            } catch (Throwable ignored) {
            }
        }
    }
}
