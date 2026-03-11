package org.project.floodalert.floodprocessor.enums;

public enum LifecycleEventType {

    CREATED,
    ESCALATED,
    /** Sự kiện được gia hạn/cập nhật bởi báo cáo của người dùng hoặc dữ liệu sensor mới */
    UPDATED,
    RESOLVED
}
