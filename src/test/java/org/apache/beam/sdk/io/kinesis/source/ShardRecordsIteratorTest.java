///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.apache.beam.sdk.io.kinesis.source;
//
//import org.apache.beam.sdk.io.kinesis.client.SimplifiedKinesisClient;
//import org.apache.beam.sdk.io.kinesis.client.response.GetKinesisRecordsResult;
//import org.apache.beam.sdk.io.kinesis.client.response.KinesisRecord;
//import org.apache.beam.sdk.io.kinesis.source.checkpoint.PositionInShard;
//import org.apache.beam.sdk.repackaged.com.google.common.base.CustomOptional;
//import org.apache.beam.sdk.repackaged.com.google.common.base.Optional;
//
//import com.amazonaws.services.kinesis.model.ExpiredIteratorException;
//import static org.fest.assertions.Assertions.assertThat;
//import static org.mockito.Matchers.any;
//import static org.mockito.Matchers.anyListOf;
//import static org.mockito.Mockito.when;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.runners.MockitoJUnitRunner;
//import org.mockito.stubbing.Answer;
//import static java.util.Arrays.asList;
//import java.io.IOException;
//import java.util.Collections;
//
///**
// * Created by ppastuszka on 12.12.15.
// */
//@RunWith(MockitoJUnitRunner.class)
//public class ShardRecordsIteratorTest {
//    public static final String INITIAL_ITERATOR = "INITIAL_ITERATOR";
//    public static final String SECOND_ITERATOR = "SECOND_ITERATOR";
//    public static final String SECOND_REFRESHED_ITERATOR = "SECOND_REFRESHED_ITERATOR";
//    public static final String THIRD_ITERATOR = "THIRD_ITERATOR";
//
//    @Mock
//    private SimplifiedKinesisClient kinesisClient;
//    @Mock
//    private PositionInShard firstCheckpoint, aCheckpoint, bCheckpoint, cCheckpoint,
//            dCheckpoint;
//    @Mock
//    private GetKinesisRecordsResult firstResult, secondResult, thirdResult;
//    @Mock
//    private KinesisRecord a, b, c, d;
//    @Mock
//    private RecordFilter recordFilter;
//
//    private ShardRecordsIterator iterator;
//
//    @Before
//    public void setUp() throws IOException {
//        when(firstCheckpoint.obtainShardIterator(kinesisClient)).thenReturn(INITIAL_ITERATOR);
//
//        when(firstCheckpoint.moveAfter(a)).thenReturn(aCheckpoint);
//        when(aCheckpoint.moveAfter(b)).thenReturn(bCheckpoint);
//        when(bCheckpoint.moveAfter(c)).thenReturn(cCheckpoint);
//        when(cCheckpoint.moveAfter(d)).thenReturn(dCheckpoint);
//
//        when(kinesisClient.getRecords(INITIAL_ITERATOR)).thenReturn(firstResult);
//        when(kinesisClient.getRecords(SECOND_ITERATOR)).thenReturn(secondResult);
//        when(kinesisClient.getRecords(THIRD_ITERATOR)).thenReturn(thirdResult);
//
//        when(firstResult.getNextShardIterator()).thenReturn(SECOND_ITERATOR);
//        when(secondResult.getNextShardIterator()).thenReturn(THIRD_ITERATOR);
//        when(thirdResult.getNextShardIterator()).thenReturn(THIRD_ITERATOR);
//
//        when(firstResult.getRecords()).thenReturn(Collections.<KinesisRecord>emptyList());
//        when(secondResult.getRecords()).thenReturn(Collections.<KinesisRecord>emptyList());
//        when(thirdResult.getRecords()).thenReturn(Collections.<KinesisRecord>emptyList());
//
//        when(recordFilter.apply(anyListOf(KinesisRecord.class), any(PositionInShard
//                .class))).thenAnswer(new IdentityAnswer());
//
//        iterator = new ShardRecordsIterator(firstCheckpoint, kinesisClient, recordFilter);
//    }
//
//    @Test
//    public void returnsAbsentIfNoRecordsPresent() throws IOException {
//        assertThat(iterator.next()).isEqualTo(CustomOptional.absent());
//        assertThat(iterator.next()).isEqualTo(CustomOptional.absent());
//        assertThat(iterator.next()).isEqualTo(CustomOptional.absent());
//    }
//
//    @Test
//    public void goesThroughAvailableRecords() throws IOException {
//        when(firstResult.getRecords()).thenReturn(asList(a, b, c));
//        when(secondResult.getRecords()).thenReturn(asList(d));
//
//        assertThat(iterator.getCheckpoint()).isEqualTo(firstCheckpoint);
//        assertThat(iterator.next()).isEqualTo(Optional.of(a));
//        assertThat(iterator.getCheckpoint()).isEqualTo(aCheckpoint);
//        assertThat(iterator.next()).isEqualTo(Optional.of(b));
//        assertThat(iterator.getCheckpoint()).isEqualTo(bCheckpoint);
//        assertThat(iterator.next()).isEqualTo(Optional.of(c));
//        assertThat(iterator.getCheckpoint()).isEqualTo(cCheckpoint);
//        assertThat(iterator.next()).isEqualTo(Optional.of(d));
//        assertThat(iterator.getCheckpoint()).isEqualTo(dCheckpoint);
//        assertThat(iterator.next()).isEqualTo(CustomOptional.absent());
//        assertThat(iterator.getCheckpoint()).isEqualTo(dCheckpoint);
//    }
//
//    @Test
//    public void refreshesExpiredIterator() throws IOException {
//        when(firstResult.getRecords()).thenReturn(asList(a));
//        when(secondResult.getRecords()).thenReturn(asList(b));
//
//        when(kinesisClient.getRecords(SECOND_ITERATOR)).thenThrow(ExpiredIteratorException.class);
//        when(aCheckpoint.obtainShardIterator(kinesisClient)).thenReturn(SECOND_REFRESHED_ITERATOR);
//        when(kinesisClient.getRecords(SECOND_REFRESHED_ITERATOR)).thenReturn(secondResult);
//
//        assertThat(iterator.next()).isEqualTo(Optional.of(a));
//        assertThat(iterator.next()).isEqualTo(Optional.of(b));
//        assertThat(iterator.next()).isEqualTo(CustomOptional.absent());
//    }
//
//    private static class IdentityAnswer implements Answer<Object> {
//        @Override
//        public Object answer(InvocationOnMock invocation) throws Throwable {
//            return invocation.getArguments()[0];
//        }
//    }
//}
