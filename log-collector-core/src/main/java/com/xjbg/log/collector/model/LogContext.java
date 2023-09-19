package com.xjbg.log.collector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kesc
 * @since 2023-07-24 10:13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogContext {
    private String requestId;
    private String userId;
    private String tenantId;
}
