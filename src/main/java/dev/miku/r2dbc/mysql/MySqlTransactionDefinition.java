/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.miku.r2dbc.mysql;

import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.Option;
import io.r2dbc.spi.TransactionDefinition;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link TransactionDefinition} for MySQL transactions.
 * <p>
 * Note: The lock wait timeout is only available in InnoDB, and only supports seconds, which must be between 1
 * and 1073741824.
 *
 * @since 0.9.0
 */
public final class MySqlTransactionDefinition implements TransactionDefinition {

    /**
     * Use {@code WITH CONSISTENT SNAPSHOT} syntax, all MySQL-compatible servers should support this syntax.
     * The option starts a consistent read for storage engines such as InnoDB and XtraDB that can do so, the
     * same as if a {@code START TRANSACTION} followed by a {@code SELECT ...} from any InnoDB table was
     * issued.
     */
    public static final Option<Boolean> WITH_CONSISTENT_SNAPSHOT = Option.valueOf("withConsistentSnapshot");

    /**
     * Use {@code START TRANSACTION WITH CONSISTENT [engine] SNAPSHOT} for Facebook/MySQL or similar syntax.
     * Only available when {@link #WITH_CONSISTENT_SNAPSHOT} is set to {@code true}.
     * <p>
     * NOTICE: This is an extended syntax for special servers. Before using it, check whether the server
     * supports the syntax.
     */
    public static final Option<ConsistentSnapshotEngine> CONSISTENT_SNAPSHOT_ENGINE =
        Option.valueOf("consistentSnapshotEngine");

    /**
     * Use {@code START TRANSACTION WITH CONSISTENT SNAPSHOT FROM SESSION [session_id]} for Percona/MySQL or
     * similar syntax. Only available when {@link #WITH_CONSISTENT_SNAPSHOT} is set to {@code true}.
     * <p>
     * The {@code session_id} is the session identifier reported in the {@code Id} column of the process list.
     * Reported by {@code SHOW COLUMNS FROM performance_schema.processlist}, it should be an unsigned 64-bit
     * integer. Use {@code SHOW PROCESSLIST} to find session identifier of the process list.
     * <p>
     * NOTICE: This is an extended syntax for special servers. Before using it, check whether the server
     * supports the syntax.
     */
    public static final Option<Long> CONSISTENT_SNAPSHOT_FROM_SESSION =
        Option.valueOf("consistentSnapshotFromSession");

    private static final MySqlTransactionDefinition EMPTY =
        new MySqlTransactionDefinition(Collections.emptyMap());

    private final Map<Option<?>, Object> options;

    private MySqlTransactionDefinition(Map<Option<?>, Object> options) {
        this.options = options;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(Option<T> option) {
        return (T) this.options.get(option);
    }

    public Builder mutate() {
        return new Builder(new HashMap<>(this.options));
    }

    public static MySqlTransactionDefinition empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder(new HashMap<>());
    }

    public static final class Builder {

        private final Map<Option<?>, Object> options;

        public MySqlTransactionDefinition build() {
            switch (this.options.size()) {
                case 0:
                    return EMPTY;
                case 1:
                    Map.Entry<Option<?>, Object> entry = this.options.entrySet().iterator().next();

                    return new MySqlTransactionDefinition(Collections.singletonMap(entry.getKey(),
                        entry.getValue()));
                default:
                    return new MySqlTransactionDefinition(new HashMap<>(this.options));
            }
        }

        public Builder isolationLevel(@Nullable IsolationLevel isolationLevel) {
            return option(ISOLATION_LEVEL, isolationLevel);
        }

        public Builder lockWaitTimeout(@Nullable Duration lockWaitTimeout) {
            return option(LOCK_WAIT_TIMEOUT, lockWaitTimeout);
        }

        public Builder readOnly(@Nullable Boolean readOnly) {
            return option(READ_ONLY, readOnly);
        }

        public Builder withConsistentSnapshot(@Nullable Boolean withConsistentSnapshot) {
            return option(WITH_CONSISTENT_SNAPSHOT, withConsistentSnapshot);
        }

        public Builder consistentSnapshotEngine(@Nullable ConsistentSnapshotEngine snapshotEngine) {
            return option(CONSISTENT_SNAPSHOT_ENGINE, snapshotEngine);
        }

        public Builder consistentSnapshotFromSession(@Nullable Long sessionId) {
            return option(CONSISTENT_SNAPSHOT_FROM_SESSION, sessionId);
        }

        private <T> Builder option(Option<T> key, @Nullable T value) {
            if (value == null) {
                this.options.remove(key);
            } else {
                this.options.put(key, value);
            }

            return this;
        }

        private Builder(Map<Option<?>, Object> options) {
            this.options = options;
        }
    }
}