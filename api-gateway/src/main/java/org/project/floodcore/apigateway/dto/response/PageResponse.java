package org.project.floodcore.apigateway.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse <T>{
    List<T>content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;
    boolean empty;

    @Builder.Default
    int numberOfElements = 0;

    @Builder.Default
    boolean sorted = false;
}
