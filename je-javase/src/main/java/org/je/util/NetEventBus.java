package org.je.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight global network event bus used by core I/O layers and UI.
 * Lives in je-javase so CLDC connections can publish without depending on Swing.
 */
public final class NetEventBus {
    private NetEventBus() {}

    public static final class NetEvent {
        public final long ts;
        public final String type;      // HTTP/HTTPS/TCP/UDP
        public final String direction; // OUT/IN
        public final String target;    // host:port or URL
        public final String info;      // short description

        public NetEvent(String type, String direction, String target, String info) {
            this.ts = System.currentTimeMillis();
            this.type = type; this.direction = direction; this.target = target; this.info = info;
        }
    }

    private static final CopyOnWriteArrayList<NetEvent> events = new CopyOnWriteArrayList<>();

    public static void publish(String type, String direction, String target, String info) {
        events.add(new NetEvent(type, direction, target, info));
    }

    public static List<NetEvent> snapshot() {
        return new ArrayList<>(events);
    }

    public static void clear() {
        events.clear();
    }

    public static int count() {
        return events.size();
    }
}
