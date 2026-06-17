package com.konayuki.statflex.anticheat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class FlagScoreTracker {
    private static final long WINDOW_MS = 3000L;

    private final List<Long> points = new ArrayList<Long>();

    void addPoint(long timestamp) {
        points.add(Long.valueOf(timestamp));
        prune(timestamp);
    }

    int getScore(long timestamp) {
        prune(timestamp);
        return points.size();
    }

    void clear() {
        points.clear();
    }

    private void prune(long timestamp) {
        Iterator<Long> iterator = points.iterator();
        while (iterator.hasNext()) {
            if (timestamp - iterator.next().longValue() > WINDOW_MS) {
                iterator.remove();
            }
        }
    }
}
