CREATE TABLE `log_info`
(
    `log_id`         varchar(36) NOT NULL,
    `user_id`        varchar(36)  DEFAULT NULL,
    `business_no`    varchar(64)  DEFAULT NULL,
    `application`    varchar(64)  DEFAULT NULL,
    `module`         varchar(16)  DEFAULT NULL,
    `action`         varchar(16)  DEFAULT NULL,
    `state`          varchar(16)  DEFAULT NULL,
    `type`           varchar(16)  DEFAULT NULL,
    `handle_method`  varchar(256) DEFAULT NULL,
    `user_agent`     varchar(256) DEFAULT NULL,
    `request_id`     varchar(36)  DEFAULT NULL,
    `request_ip`     varchar(64)  DEFAULT NULL,
    `request_url`    varchar(512) DEFAULT NULL,
    `request_method` varchar(16)  DEFAULT NULL,
    `request_time`   datetime     DEFAULT NULL,
    `create_time`    datetime     DEFAULT NULL,
    `response_time`  datetime     DEFAULT NULL,
    `params`         longtext,
    `response`       longtext,
    PRIMARY KEY (`log_id`)
);

CREATE INDEX idx_log_info_create_time ON log_info ( create_time );