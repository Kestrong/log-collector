package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.LogCollectorConstant;
import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.io.Reader;
import java.io.StringReader;
import java.sql.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kesc
 * @since 2023-03-30 12:13
 */
public class DataBaseLogCollector extends AbstractLogCollector<LogInfo, LogInfo> {
    private final DataSource dataSource;
    private String wrapper;
    private String tableName = "log_info";
    private volatile String sqlTemplate;

    public DataBaseLogCollector(DataSource dataSource) {
        super();
        this.dataSource = dataSource;
    }

    public String getWrapper() {
        return wrapper;
    }

    public void setWrapper(String wrapper) {
        this.wrapper = wrapper;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    protected void closeDbResource(Connection conn, Statement statement, Boolean connAutoCommit) {
        if (conn != null) {
            try {
                if (connAutoCommit != null) {
                    conn.setAutoCommit(connAutoCommit);
                }
            } catch (SQLException e) {
                //ignore
            }
            try {
                conn.close();
            } catch (SQLException e) {
                //ignore
            }
        }
        // close PreparedStatement
        if (null != statement) {
            try {
                statement.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    @Override
    public String type() {
        return CollectorType.DATABASE.getType();
    }

    protected String wrap(String name) {
        if (StringUtils.isBlank(getWrapper())) {
            return name;
        }
        return getWrapper() + name + getWrapper();
    }

    private String getInsertSqlTemplate() {
        if (sqlTemplate == null) {
            synchronized (this) {
                if (sqlTemplate == null) {
                    List<String> fields = Arrays.asList("log_id", "user_id", "tenant_id", "business_no", "application", "module", "action", "state", "type", "handle_method", "user_agent", "message", "request_id", "request_ip", "request_url", "request_method", "request_time", "create_time", "response_time", "params", "response");
                    sqlTemplate = String.format("INSERT INTO %s(%s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", wrap(getTableName()), fields.stream().map(this::wrap).collect(Collectors.joining(",")));
                }
            }
        }
        return sqlTemplate;
    }

    @Override
    protected void doLog(List<LogInfo> logInfos) throws Exception {
        Connection conn = null;
        Boolean connAutoCommit = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = dataSource.getConnection();
            connAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            preparedStatement = conn.prepareStatement(getInsertSqlTemplate());
            for (LogInfo logInfo : logInfos) {
                int index = 1;
                preparedStatement.setString(index++, logInfo.getLogId());
                preparedStatement.setString(index++, logInfo.getUserId());
                preparedStatement.setString(index++, logInfo.getTenantId());
                preparedStatement.setString(index++, logInfo.getBusinessNo());
                preparedStatement.setString(index++, logInfo.getApplication());
                preparedStatement.setString(index++, logInfo.getModule());
                preparedStatement.setString(index++, logInfo.getAction());
                preparedStatement.setString(index++, logInfo.getState());
                preparedStatement.setString(index++, logInfo.getType());
                preparedStatement.setString(index++, logInfo.getHandleMethod());
                preparedStatement.setString(index++, logInfo.getUserAgent());
                preparedStatement.setString(index++, logInfo.getMessage());
                preparedStatement.setString(index++, logInfo.getRequestId());
                preparedStatement.setString(index++, logInfo.getRequestIp());
                preparedStatement.setString(index++, logInfo.getRequestUrl());
                preparedStatement.setString(index++, logInfo.getRequestMethod());
                preparedStatement.setTimestamp(index++, logInfo.getRequestTime() == null ? null : new Timestamp(logInfo.getRequestTime().getTime()));
                preparedStatement.setTimestamp(index++, logInfo.getCreateTime() == null ? null : new Timestamp(logInfo.getCreateTime().getTime()));
                preparedStatement.setTimestamp(index++, logInfo.getResponseTime() == null ? null : new Timestamp(logInfo.getResponseTime().getTime()));
                String params = JsonLogUtil.toJson(logInfo.getParams());
                try {
                    preparedStatement.setString(index, params);
                } catch (SQLException e) {
                    try (Reader reader = new StringReader(params)) {
                        preparedStatement.setCharacterStream(index, reader);
                    }
                }
                index++;
                String response = JsonLogUtil.toJson(logInfo.getResponse());
                try {
                    preparedStatement.setString(index, response);
                } catch (SQLException e) {
                    try (Reader reader = new StringReader(response)) {
                        preparedStatement.setCharacterStream(index, reader);
                    }
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            closeDbResource(conn, preparedStatement, connAutoCommit);
        }
    }

    @Override
    public void cleanLog(Date before) throws Exception {
        log.info("clean up application[{}]'s log before:{}", LogCollectorConstant.APPLICATION, before);
        Connection conn = null;
        Boolean connAutoCommit = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = dataSource.getConnection();
            connAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            preparedStatement = conn.prepareStatement(String.format("delete from %s where %s <=? and %s =?", wrap(getTableName()), wrap("create_time"), wrap("application")));
            preparedStatement.setTimestamp(1, new Timestamp(before.getTime()));
            preparedStatement.setString(2, LogCollectorConstant.APPLICATION);
            preparedStatement.execute();
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            closeDbResource(conn, preparedStatement, connAutoCommit);
        }
    }

}