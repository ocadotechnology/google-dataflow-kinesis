/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.io.kinesis.source.checkpoint.generator;

import static com.google.api.client.repackaged.com.google.common.base.Preconditions.checkNotNull;

import org.apache.beam.sdk.io.kinesis.client.SimplifiedKinesisClient;
import org.apache.beam.sdk.io.kinesis.source.checkpoint.KinesisReaderCheckpoint;

/**
 * Always returns the same instance of checkpoint.
 */
public class StaticCheckpointGenerator implements CheckpointGenerator {
    private final KinesisReaderCheckpoint checkpoint;

    public StaticCheckpointGenerator(KinesisReaderCheckpoint checkpoint) {
        checkNotNull(checkpoint);
        this.checkpoint = checkpoint;
    }

    @Override
    public KinesisReaderCheckpoint generate(SimplifiedKinesisClient client) {
        return checkpoint;
    }

    @Override
    public String toString() {
        return checkpoint.toString();
    }
}
