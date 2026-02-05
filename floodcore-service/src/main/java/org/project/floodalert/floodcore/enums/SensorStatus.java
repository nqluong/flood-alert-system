package org.project.floodalert.floodcore.enums;

public enum SensorStatus {
    ACTIVE("Đang hoạt động", true),
    DISABLED("Tạm ngừng", false),
    MAINTENANCE("Đang bảo trì", true),
    OFFLINE("Mất kết nối", false),
    DELETED("Đã xóa", false);

    private final String description;
    private final boolean acceptData;

    SensorStatus(String description, boolean acceptData) {
        this.description = description;
        this.acceptData = acceptData;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAcceptData() {
        return acceptData;
    }

    /**
     * Check xem sensor có đang hoạt động không
     */
    public boolean isOperational() {
        return this == ACTIVE || this == MAINTENANCE;
    }

    /**
     * Check xem có nên hiển thị trên map không
     */
    public boolean isVisibleOnMap() {
        return this != DELETED;
    }
}
