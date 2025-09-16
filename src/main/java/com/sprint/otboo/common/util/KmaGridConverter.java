package com.sprint.otboo.common.util;

public final class KmaGridConverter {
    private KmaGridConverter() {}
    public record XY(int x, int y) {}
    public static XY toXY(double lat, double lon) {
        return new XY(0, 0);
    }
}
