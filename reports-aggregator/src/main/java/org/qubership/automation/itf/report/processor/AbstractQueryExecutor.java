/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.qubership.automation.itf.report.processor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.qubership.automation.itf.core.util.logger.TimeLogger;
import org.qubership.automation.itf.report.QueryExecutor;
import org.qubership.automation.itf.report.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import com.google.gson.JsonObject;

public abstract class AbstractQueryExecutor implements QueryExecutor {

    private JdbcTemplate jdbcTemplate;

    /**
     * TODO Add JavaDoc.
     */
    public Object execute(StatementContext sql, JsonObject object, boolean isNeedResultSet) {
        TimeLogger.LOGGER.debug("Start for method: org.qubership.automation.itf.core.report.impl"
                + ".AbstractQueryExecutor.execute. sql:" + sql.getQuery());
        Object result = jdbcTemplate.execute(connection -> sql.prepare(object, connection),
                (PreparedStatementCallback<Object>) preparedStatement -> {
                    Object result1 = null;
                    if (isNeedResultSet) {
                        result1 = prepareData(insert(preparedStatement), object);
                    } else {
                        update(preparedStatement);
                    }
                    return result1;
                });
        TimeLogger.LOGGER.debug("End for method: org.qubership.automation.itf.core.report.impl"
                + ".AbstractQueryExecutor" + ".execute. sql:" + sql.getQuery());
        return result;
    }

    private ResultSet insert(PreparedStatement statement) throws SQLException {
        return statement.executeQuery();
    }

    private void update(PreparedStatement statement) throws SQLException {
        statement.execute();
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected abstract Object prepareData(ResultSet resultSet, JsonObject objectType) throws SQLException;
}
