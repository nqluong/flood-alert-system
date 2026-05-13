package org.project.floodalert.floodprocessor.enums;

public enum LifecycleEventType {

    CREATED,        // Phát hiện ngập mới
    ESCALATED,      // Mức độ ngập tăng (WARNING → DANGER)
    DE_ESCALATED,   // Mức độ ngập giảm (DANGER → WARNING)
    UPDATED,        // Cập nhật thông tin (không thay đổi severity)
    RESOLVED        // Hết ngập (về SAFE)
}
