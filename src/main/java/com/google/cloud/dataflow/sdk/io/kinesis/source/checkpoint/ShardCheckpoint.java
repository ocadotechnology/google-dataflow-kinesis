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
package com.google.cloud.dataflow.sdk.io.kinesis.source.checkpoint;

import static com.google.cloud.dataflow.sdk.repackaged.com.google.common.base.Preconditions
        .checkArgument;
import static com.google.cloud.dataflow.sdk.repackaged.com.google.common.base.Preconditions
        .checkNotNull;
import com.google.cloud.dataflow.sdk.io.kinesis.client.SimplifiedKinesisClient;
import com.google.cloud.dataflow.sdk.io.kinesis.source.ShardRecordsIterator;

import static com.amazonaws.services.kinesis.model.ShardIteratorType.AFTER_SEQUENCE_NUMBER;
import static com.amazonaws.services.kinesis.model.ShardIteratorType.AT_SEQUENCE_NUMBER;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.types.ExtendedSequenceNumber;
import com.amazonaws.services.kinesis.clientlibrary.types.UserRecord;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.kinesis.model.ShardIteratorType;
import java.io.IOException;
import java.io.Serializable;

/***
 * Checkpoint mark for single shard in the stream.
 * Current position in the shard is determined by either:
 * <ul>
 * <li>{@link #shardIteratorType} if it is equal to {@link ShardIteratorType#LATEST} or
 * {@link ShardIteratorType#TRIM_HORIZON}</li>
 * <li>combination of
 * {@link #sequenceNumber} and {@link #subSequenceNumber} if
 * {@link ShardIteratorType#AFTER_SEQUENCE_NUMBER} or
 * {@link ShardIteratorType#AT_SEQUENCE_NUMBER}</li>
 * </ul>
 * This class is immutable.
 */
public class ShardCheckpoint implements Serializable {
    private final String streamName;
    private final String shardId;
    private final String sequenceNumber;
    private final ShardIteratorType shardIteratorType;
    private final Long subSequenceNumber;

    public ShardCheckpoint(String streamName, String shardId, InitialPositionInStream
            initialPositionInStream) {

        this(streamName, shardId, ShardIteratorType.fromValue(initialPositionInStream.name()),
                null);
    }

    public ShardCheckpoint(String streamName, String shardId, ShardIteratorType
            shardIteratorType, String sequenceNumber) {
        this(streamName, shardId, shardIteratorType, sequenceNumber, null);
    }

    public ShardCheckpoint(String streamName, String shardId, ShardIteratorType
            shardIteratorType, String sequenceNumber, Long subSequenceNumber) {

        checkNotNull(streamName);
        checkNotNull(shardId);
        checkNotNull(shardIteratorType);
        if (shardIteratorType == AT_SEQUENCE_NUMBER || shardIteratorType == AFTER_SEQUENCE_NUMBER) {
            checkNotNull(sequenceNumber,
                    "You must provide sequence number for AT_SEQUENCE_NUMBER" +
                            " or AFTER_SEQUENCE_NUMBER");
        } else {
            checkArgument(sequenceNumber == null,
                    "Sequence number must be null for LATEST and TRIM_HORIZON");
        }

        this.subSequenceNumber = subSequenceNumber;
        this.shardIteratorType = shardIteratorType;
        this.streamName = streamName;
        this.shardId = shardId;
        this.sequenceNumber = sequenceNumber;
    }

    /***
     * Used to compare {@link ShardCheckpoint} object to {@link ExtendedSequenceNumber}.
     *
     * @param other
     * @return if current checkpoint mark points before or at given {@link ExtendedSequenceNumber}
     */
    public boolean isBeforeOrAt(ExtendedSequenceNumber other) {
        int result = extendedSequenceNumber().compareTo(other);
        if (result == 0) {
            return shardIteratorType == AT_SEQUENCE_NUMBER;
        }
        return result < 0;
    }

    private ExtendedSequenceNumber extendedSequenceNumber() {
        String fullSequenceNumber = sequenceNumber;
        if (fullSequenceNumber == null) {
            fullSequenceNumber = shardIteratorType.toString();
        }
        return new ExtendedSequenceNumber(fullSequenceNumber, subSequenceNumber);
    }

    @Override
    public String toString() {
        return String.format("Checkpoint %s for stream %s, shard %s: %s", shardIteratorType, streamName, shardId,
                sequenceNumber);
    }

    public ShardRecordsIterator getShardRecordsIterator(SimplifiedKinesisClient kinesis)
            throws IOException {
        return new ShardRecordsIterator(this, kinesis);
    }

    public String getShardIterator(SimplifiedKinesisClient kinesisClient) throws IOException {
        if (shardIteratorType == AFTER_SEQUENCE_NUMBER && subSequenceNumber != null) {
            return kinesisClient.getShardIterator(streamName,
                    shardId, AT_SEQUENCE_NUMBER,
                    sequenceNumber);
        }
        return kinesisClient.getShardIterator(streamName,
                shardId, shardIteratorType,
                sequenceNumber);
    }

    /***
     * Used to advance checkpoint mark to position after given {@link Record}.
     *
     * @param record
     * @return new checkpoint object pointing directly after given {@link Record}
     */
    public ShardCheckpoint moveAfter(UserRecord record) {
        return new ShardCheckpoint(
                streamName, shardId,
                AFTER_SEQUENCE_NUMBER,
                record.getSequenceNumber(),
                record.getSubSequenceNumber());
    }
}
