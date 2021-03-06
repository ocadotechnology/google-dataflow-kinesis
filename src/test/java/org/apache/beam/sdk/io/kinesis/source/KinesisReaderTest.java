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
//import org.apache.beam.sdk.io.kinesis.client.response.KinesisRecord;
//import org.apache.beam.sdk.io.kinesis.source.checkpoint.KinesisReaderCheckpoint;
//import org.apache.beam.sdk.io.kinesis.source.checkpoint.PositionInShard;
//import org.apache.beam.sdk.io.kinesis.source.checkpoint.generator.CheckpointGenerator;
//import org.apache.beam.sdk.repackaged.com.google.common.base.CustomOptional;
//import org.apache.beam.sdk.repackaged.com.google.common.base.Optional;
//
//import static org.fest.assertions.Assertions.assertThat;
//import static org.mockito.Mockito.when;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.runners.MockitoJUnitRunner;
//import static java.util.Arrays.asList;
//import java.io.IOException;
//import java.util.NoSuchElementException;
//
///**
// * Created by ppastuszka on 12.12.15.
// */
//@RunWith(MockitoJUnitRunner.class)
//public class KinesisReaderTest {
//    @Mock
//    private SimplifiedKinesisClient kinesis;
//    @Mock
//    private CheckpointGenerator generator;
//    @Mock
//    private PositionInShard firstCheckpoint, secondCheckpoint;
//    @Mock
//    private ShardRecordsIterator firstIterator, secondIterator;
//    @Mock
//    private KinesisRecord a, b, c, d;
//
//    private KinesisReader reader;
//
//    @Before
//    public void setUp() throws IOException {
//        when(generator.generate(kinesis)).thenReturn(new KinesisReaderCheckpoint(
//                asList(firstCheckpoint, secondCheckpoint)
//        ));
//        when(firstCheckpoint.getShardRecordsIterator(kinesis)).thenReturn(firstIterator);
//        when(secondCheckpoint.getShardRecordsIterator(kinesis)).thenReturn(secondIterator);
//        when(firstIterator.next()).thenReturn(CustomOptional.<KinesisRecord>absent());
//        when(secondIterator.next()).thenReturn(CustomOptional.<KinesisRecord>absent());
//
//        reader = new KinesisReader(kinesis, generator, null);
//    }
//
//    @Test
//    public void startReturnsFalseIfNoDataAtTheBeginning() throws IOException {
//        assertThat(reader.start()).isFalse();
//    }
//
//    @Test(expected = NoSuchElementException.class)
//    public void throwsNoSuchElementExceptionIfNoData() throws IOException {
//        reader.start();
//        reader.getCurrent();
//    }
//
//    @Test
//    public void startReturnsTrueIfSomeDataAvailable() throws IOException {
//        when(firstIterator.next()).
//                thenReturn(Optional.of(a)).
//                thenReturn(CustomOptional.<KinesisRecord>absent());
//
//        assertThat(reader.start()).isTrue();
//    }
//
//    @Test
//    public void readsThroughAllDataAvailable() throws IOException {
//        when(firstIterator.next()).
//                thenReturn(CustomOptional.<KinesisRecord>absent()).
//                thenReturn(Optional.of(a)).
//                thenReturn(CustomOptional.<KinesisRecord>absent()).
//                thenReturn(Optional.of(b)).
//                thenReturn(CustomOptional.<KinesisRecord>absent());
//
//        when(secondIterator.next()).
//                thenReturn(Optional.of(c)).
//                thenReturn(CustomOptional.<KinesisRecord>absent()).
//                thenReturn(Optional.of(d)).
//                thenReturn(CustomOptional.<KinesisRecord>absent());
//
//        assertThat(reader.start()).isTrue();
//        assertThat(reader.getCurrent()).isEqualTo(c);
//        assertThat(reader.advance()).isTrue();
//        assertThat(reader.getCurrent()).isEqualTo(a);
//        assertThat(reader.advance()).isTrue();
//        assertThat(reader.getCurrent()).isEqualTo(d);
//        assertThat(reader.advance()).isTrue();
//        assertThat(reader.getCurrent()).isEqualTo(b);
//        assertThat(reader.advance()).isFalse();
//    }
//
//}
