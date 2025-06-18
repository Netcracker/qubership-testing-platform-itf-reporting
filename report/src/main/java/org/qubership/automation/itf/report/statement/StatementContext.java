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

package org.qubership.automation.itf.report.statement;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class StatementContext {
    private final StringBuilder query = new StringBuilder();
    private final List<Setter> params = Lists.newArrayList();

    @Nonnull
    public StringBuilder getQuery() {
        return query;
    }

    public void addParameter(@Nonnull Setter setter) {
        params.add(setter);
    }

    /**
     * Prepare statement.
     */
    @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
            justification = "Only application-controlled code can be here")
    public PreparedStatement prepare(JsonObject json, Connection connection) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(getQuery().toString());
            int i = 1;
            for (Setter s : params) {
                i = s.set(json, statement, i);
            }
            return statement;
        } catch (SQLException ex) {
            if (statement != null) {
                statement.close();
            }
            throw ex;
        }
    }

    /**
     * Get List of params.
     */
    public List<String> getParameters() {
        List<String> params = Lists.newArrayList();
        for (Setter property : this.params) {
            params.add(property.getProperty());
        }
        return params;
    }

    /**
     * Append String queryPart to the query.
     */
    public StatementContext appendQueryPart(final String queryPart) {
        getQuery().append(queryPart);
        return this;
    }

    /**
     * Add long parameter to the query.
     */
    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON",
            justification = "Old code; and not so clear how to fix")
    public StatementContext asLong(final String prop) {
        addParameter(new AbstractSetter(prop, Types.BIGINT) {
            @Override
            protected Object convertJsonValue(JsonElement element) {
                return element.getAsLong();
            }
        });
        getQuery().append("?");
        return this;
    }

    /**
     * Add int parameter to the query..
     */
    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON",
            justification = "Old code; and not so clear how to fix")
    public StatementContext asInt(final String prop) {
        addParameter(new AbstractSetter(prop, Types.INTEGER) {
            @Override
            protected Object convertJsonValue(JsonElement element) {
                return element.getAsInt();
            }
        });
        getQuery().append("?");
        return this;
    }

    /**
     * Add Date parameter to the query.
     */
    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON",
            justification = "Old code; and not so clear how to fix")
    public StatementContext asDate(final String prop) {
        addParameter(new AbstractSetter(prop, Types.DATE) {
            @Override
            protected Object convertJsonValue(JsonElement element) {
                long dateLong = element.getAsLong();
                return new Date(dateLong);
            }
        });
        getQuery().append("?");
        return this;
    }

    /**
     * Add Timestamp parameter to the query.
     */
    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON",
            justification = "Old code; and not so clear how to fix")
    public StatementContext asDataTime(final String prop) {
        addParameter(new AbstractSetter(prop, Types.TIMESTAMP) {
            @Override
            protected Object convertJsonValue(JsonElement element) {
                long dateLong = element.getAsLong();
                return new Timestamp(dateLong);
            }
        });
        getQuery().append("?");
        return this;
    }

    /**
     * Add Boolean parameter to the query.
     */
    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON",
            justification = "Old code; and not so clear how to fix")
    public StatementContext asBoolean(final String prop) {
        addParameter(new AbstractSetter(prop, Types.BOOLEAN) {
            @Override
            protected Object convertJsonValue(JsonElement element) {
                return element.getAsBoolean();
            }
        });
        getQuery().append("?");
        return this;
    }

    /**
     * Add String parameter to the query.
     */
    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON",
            justification = "Old code; and not so clear how to fix")
    public StatementContext asString(final String prop) {
        addParameter(new AbstractSetter(prop, Types.VARCHAR) {
            @Override
            protected Object convertJsonValue(JsonElement element) {
                return element.getAsString();
            }
        });
        getQuery().append("?");
        return this;
    }

    /**
     * Add String parameter to the query (fixing char(0) characters).
     */
    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON",
            justification = "Old code; and not so clear how to fix")
    public StatementContext asStringFixedZero(final String prop) {
        addParameter(new AbstractSetter(prop, Types.VARCHAR) {
            @Override
            protected Object convertJsonValue(JsonElement element) {
                /*
                    Old replacement was:
                        .replace((char) 0, (char) 32)
                    It worked fine; have changed to the below variant for unification
                    with ExecutionReportQueryExecutor.java#fixUnicodeZeroByteSequence.
                    It was tested: both replacements do the same (except different new char).
                 */
                return element.getAsString()
                        //.replace((char) 0, (char) 32)
                        .replace("\u0000", StringUtils.EMPTY);
            }
        });
        getQuery().append("?");
        return this;
    }
}
