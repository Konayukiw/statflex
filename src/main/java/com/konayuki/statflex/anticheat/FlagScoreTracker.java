package com.konayuki.statflex.anticheat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class FlagScoreTracker {
    private static final long WINDOW_MS = 4000L;

    private static final class Entry {
        final long timestamp;
        final int weight;

        Entry(long timestamp, int weight) {
            this.timestamp = timestamp;
            this.weight = weight;
        }
    }

    private final List<Entry> entries = new ArrayList<Entry>();

    void addPoint(long timestamp) {
        addPoints(1, timestamp);
    }

    void addPoints(int weight, long timestamp) {
        entries.add(new Entry(timestamp, weight));
        prune(timestamp);
    }

    int getScore(long timestamp) {
        prune(timestamp);
        int total = 0;
        for (Entry e : entries) {
            total += e.weight;
        }
        return total;
    }

    void clear() {
        entries.clear();
    }

    private void prune(long timestamp) {
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            if (timestamp - it.next().timestamp > WINDOW_MS) {
                it.remove();
            }
        }
    }
}