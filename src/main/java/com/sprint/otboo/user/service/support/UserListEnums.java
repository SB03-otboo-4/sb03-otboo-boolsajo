package com.sprint.otboo.user.service.support;

/**
 * 사용자 목록 조회 시 컨트롤러/서비스/레포지토리 사이에서
 * 정렬 파라미터를 전달하기 위한 Enum 모음
 * */
public final class UserListEnums {

    private UserListEnums() {}

    /**
     * 지원하는 정렬 필드
     * EMAIL : 이메일 기준 정렬
     * CREATED_AT : 생성일 기준 정렬
     * */
    public enum SortBy {
        EMAIL("email"),
        CREATED_AT("createdAt");

        private final String param;
        SortBy(String p) { this.param = p; }
        public String toParam() { return param; }

        // 쿼리 파라미터에서 들어온 문자열을 Enum으로 반환
        public static SortBy fromParam(String s) {
            if (s == null) throw new IllegalArgumentException("sortBy required");
            return switch (s) {
                case "email" -> EMAIL;
                case "createdAt" -> CREATED_AT;
                default -> throw new IllegalArgumentException("invalid sortBy: " + s);
            };
        }
    }

    /**
     * 정렬 방향
     * ASCENDING : 오름 차순
     * DESCENDING : 내림 차순
     * */
    public enum SortDirection {
        ASCENDING("ASCENDING"), DESCENDING("DESCENDING");
        private final String param;
        SortDirection(String p) { this.param = p; }
        public String toParam() { return param; }

        // 쿼리 파라미터에서 들어온 문자열을 Enum으로 반환.
        public static SortDirection fromParam(String s) {
            if (s == null) throw new IllegalArgumentException("sortDirection required");
            return switch (s) {
                case "ASCENDING" -> ASCENDING;
                case "DESCENDING" -> DESCENDING;
                default -> throw new IllegalArgumentException("invalid sortDirection: " + s);
            };
        }
    }
}