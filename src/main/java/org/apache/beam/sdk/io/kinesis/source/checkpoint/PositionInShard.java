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
package org.apache.beam.sdk.io.kinesis.source.checkpoint;

import static org.apache.beam.sdk.repackaged.com.google.common.base.Preconditions
        .checkArgument;
import static org.apache.beam.sdk.repackaged.com.google.common.base.Preconditions
        .checkNotNull;
import org.apache.beam.sdk.io.kinesis.client.SimplifiedKinesisClient;
import org.apache.beam.sdk.io.kinesis.client.response.KinesisRecord;
import org.apache.beam.sdk.io.kinesis.source.ShardRecordsIterator;

import static com.amazonaws.services.kinesis.model.ShardIteratorType.AFTER_SEQUENCE_NUMBER;
import static com.amazonaws.services.kinesis.model.ShardIteratorType.AT_SEQUENCE_NUMBER;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.types.ExtendedSequenceNumber;
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
public class PositionInShard implements Serializable {
    private final String streamName;
    private final String shardId;
    private final String sequenceNumber;
    private final ShardIteratorType shardIteratorType;
    private final Long subSequenceNumber;

    public PositionInShard(String streamName, String shardId, InitialPositionInStream
            initialPositionInStream) {

        this(streamName, shardId, ShardIteratorType.fromValue(initialPositionInStream.name()),
                null);
    }

    public PositionInShard(String streamName, String shardId, ShardIteratorType
            shardIteratorType, String sequenceNumber) {
        this(streamName, shardId, shardIteratorType, sequenceNumber, null);
    }

    public PositionInShard(String streamName, String shardId, ShardIteratorType
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
                    "Sequence number must be null for LATEST or TRIM_HORIZON");
        }

        this.subSequenceNumber = subSequenceNumber;
        this.shardIteratorType = shardIteratorType;
        this.streamName = streamName;
        this.shardId = shardId;
        this.sequenceNumber = sequenceNumber;
    }

    /***
     * Used to compare {@link PositionInShard} object to {@link ExtendedSequenceNumber}.
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
        return String.format("Position %s for stream %s, shard %s: %s", shardIteratorType,
                streamName, shardId,
                sequenceNumber);
    }

    public String obtainShardIterator(SimplifiedKinesisClient kinesisClient) throws IOException {
        if (wasInsideBatchedRecord()) {
            return kinesisClient.getShardIterator(streamName,
                    shardId, AT_SEQUENCE_NUMBER,
                    sequenceNumber);
        }
        return kinesisClient.getShardIterator(streamName,
                shardId, shardIteratorType,
                sequenceNumber);
    }

    private boolean wasInsideBatchedRecord() {
        return shardIteratorType == AFTER_SEQUENCE_NUMBER && subSequenceNumber != null;
    }

    /***
     * Used to advance checkpoint mark to position after given {@link Record}.
     *
     * @param record
     * @return new checkpoint object pointing directly after given {@link Record}
     */
    public PositionInShard moveAfter(KinesisRecord record) {
        return new PositionInShard(
                streamName, shardId,
                AFTER_SEQUENCE_NUMBER,
                record.getSequenceNumber(),
                record.getSubSequenceNumber());
    }
}
