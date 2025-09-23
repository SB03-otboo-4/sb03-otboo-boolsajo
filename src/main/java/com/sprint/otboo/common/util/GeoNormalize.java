package com.sprint.otboo.common.util;

public final class GeoNormalize {
    private GeoNormalize() {}
    public static java.math.BigDecimal lat(double v) {
        return java.math.BigDecimal.valueOf(v).setScale(6, java.math.RoundingMode.HALF_UP);
    }
    public static java.math.BigDecimal lon(double v) {
        return java.math.BigDecimal.valueOf(v).setScale(6, java.math.RoundingMode.HALF_UP);
    }
}
