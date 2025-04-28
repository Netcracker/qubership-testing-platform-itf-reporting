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

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.qubership.automation.itf.report.Processor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;

public abstract class AbstractProcessor implements Processor {
    private final Cache<String, Long> lastProcessedEntity =
            CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
    private static final LoadingCache<String, Object> locks = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(CacheLoader.from(Object::new));

    /**
     * Sync by objectId and process json message to store into db.
     * Only TcContexts are synchronized, because all others reported once.
     *
     * @param jsonObject message as json object received from executor that will be processed to store in reporting db.
     * @param time       received time
     * @param objectId   message id from activemq property.
     * @param objectType message type from activemq property.
     * @throws SQLException             sql exceptions.
     * @throws IllegalArgumentException if objectType is null or empty.
     */
    public void process(JsonObject jsonObject, long time, String objectId, String objectType)
            throws SQLException, IllegalArgumentException {
        if (objectType.contains("TcContext")) {
            synchronized (locks.getUnchecked(objectId)) {
                Long lastTime = lastProcessedEntity.getIfPresent(objectId);
                if (lastTime == null || lastTime < time) {
                    process(jsonObject);
                    lastProcessedEntity.put(objectId, time);
                }
            }
        } else {
            process(jsonObject);
        }
    }

    /**
     * Process.
     *
     * @param object the object
     */
    protected abstract void process(JsonObject object) throws SQLException;
}
