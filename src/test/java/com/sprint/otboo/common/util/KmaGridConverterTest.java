package com.sprint.otboo.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KmaGridConverterTest {

    @Test
    void 서울시청_좌표는_격자_60_127_이어야_한다() {
        KmaGridConverter.XY xy = KmaGridConverter.toXY(37.5665, 126.9780);
        assertThat(xy.x()).isEqualTo(60);
        assertThat(xy.y()).isEqualTo(127);
    }

    @Test
    void 경계값_좌표도_점수_격자로_변환되어야_한다() {
        KmaGridConverter.XY xy1 = KmaGridConverter.toXY(33.0, 126.0);
        KmaGridConverter.XY xy2 = KmaGridConverter.toXY(38.0, 132.0);
        assertThat(((Object) xy1.x())).isInstanceOf(Integer.class);
        assertThat(((Object) xy2.y())).isInstanceOf(Integer.class);
    }
}
