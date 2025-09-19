package com.sprint.otboo.user.service.support;

public final class UserListEnums {

    private UserListEnums() {}

    public enum SortBy {
        EMAIL("email"),
        CREATED_AT("createdAt");

        private final String param;
        SortBy(String p) { this.param = p; }
        public String toParam() { return param; }

        public static SortBy fromParam(String s) {
            if (s == null) throw new IllegalArgumentException("sortBy required");
            return switch (s) {
                case "email" -> EMAIL;
                case "createdAt" -> CREATED_AT;
                default -> throw new IllegalArgumentException("invalid sortBy: " + s);
            };
        }
    }

    public enum SortDirection {
        ASCENDING("ASCENDING"), DESCENDING("DESCENDING");
        private final String param;
        SortDirection(String p) { this.param = p; }
        public String toParam() { return param; }

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