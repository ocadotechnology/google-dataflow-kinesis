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
package utils;

import static com.google.api.client.repackaged.com.google.common.base.Preconditions.checkNotNull;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.model.Record;
import static org.joda.time.Duration.standardDays;
import static org.joda.time.Duration.standardSeconds;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.BigQueryIO;
import org.apache.beam.sdk.io.KinesisIO;
import org.apache.beam.sdk.io.PubsubIO;
import org.apache.beam.sdk.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.repackaged.com.google.common.base.Charsets;
import org.apache.beam.sdk.repackaged.com.google.common.collect.Lists;
import org.apache.beam.sdk.runners.DataflowPipelineJob;
import org.apache.beam.sdk.runners.DataflowPipelineRunner;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.bridge.SLF4JBridgeHandler;
import static java.util.Arrays.asList;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/***
 *
 */
public class TestUtils {

    public static final SecureRandom RANDOM = new SecureRandom();

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();

        SLF4JBridgeHandler.install();
    }

    public static String randomString() {
        return new BigInteger(130, RANDOM).toString(32);
    }

    public static List<String> randomStrings(int howMany) {
        return randomStrings(howMany, 0);
    }

    public static List<String> randomStrings(int howMany, int startingId) {
        List<String> data = Lists.newArrayList();
        for (int i = startingId; i < startingId + howMany; ++i) {
            data.add(String.format("%s - %s", i, TestUtils.randomString()));
        }
        return data;
    }

    public static TableSchema getTestTableSchema() {
        return new TableSchema().
                setFields(asList(
                        new TableFieldSchema()
                                .setName("a")
                                .setType("STRING")));
    }

    public static TableReference getTestTableReference() {
        return new TableReference().
                setProjectId(TestConfiguration.get().getTestProject()).
                setDatasetId(TestConfiguration.get().getTestDataset()).
                setTableId(getTestTableId());
    }

    public static String getTestTableId() {
        return randomString();
    }

    public static AWSCredentialsProvider getTestAwsCredentialsProvider() {
        return getStaticCredentialsProvider(
                TestConfiguration.get().getAwsAccessKey(),
                TestConfiguration.get().getAwsSecretKey()
        );
    }

    private static AWSCredentialsProvider getStaticCredentialsProvider(String accessKey,
                                                                       String secretKey) {
        return new StaticCredentialsProvider(new BasicAWSCredentials(
                accessKey, secretKey
        ));
    }

    public static DataflowPipelineOptions getTestPipelineOptions(String jobName) {
        DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
        options.setProject(TestConfiguration.get().getTestProject());
        options.setStreaming(true);
        options.setJobName(jobName);
        options.setRunner(DataflowPipelineRunner.class);
        options.setStagingLocation(TestConfiguration.get().getTestStagingLocation());
        options.setTempLocation(TestConfiguration.get().getTestTempLocation());
        return options;
    }

    public static <T> Collection<T> pickNRandom(Collection<T> input, int n) {
        if (input.isEmpty()) {
            return input;
        }

        List<T> list = Lists.newArrayList(input);
        Collections.shuffle(list);

        return Lists.partition(list, Math.min(n, list.size())).get(0);
    }

    public static DataflowPipelineJob runKinesisToBigQueryJob(TableReference targetTable, String
            jobName)
            throws InterruptedException {
        DataflowPipelineOptions options = getTestPipelineOptions(jobName);
        Pipeline p = Pipeline.create(options);
        PCollection<String> input = p.
                apply(KinesisIO.Read.
                        from(
                                TestConfiguration.get().getTestKinesisStream(),
                                InitialPositionInStream.LATEST).
                        using(getTestKinesisClientProvider())).
                apply(ParDo.of(new RecordDataToString()));

        return runBqJob(targetTable, options, p, input);
    }

    public static DataflowPipelineJob runPubSubToBigQueryJob(TableReference targetTable, String
            jobName)
            throws InterruptedException {
        DataflowPipelineOptions options = getTestPipelineOptions(jobName);
        Pipeline p = Pipeline.create(options);
        PCollection<String> input = p.apply(PubsubIO.Read.topic(TestConfiguration.get()
                .getTestPubSubTopic()));

        return runBqJob(targetTable, options, p, input);
    }

    private static DataflowPipelineJob runBqJob(TableReference targetTable,
                                                DataflowPipelineOptions options, Pipeline p,
                                                PCollection<String> input) throws
            InterruptedException {
        input.apply(Window.<String>into(FixedWindows.of(standardSeconds(10))).withAllowedLateness
                (standardDays(1))).
                apply(ParDo.of(new ToTableRow())).
                apply(BigQueryIO.Write.
                        to(targetTable).

                        withSchema(TestUtils.getTestTableSchema()));
        DataflowPipelineJob job = DataflowPipelineRunner.fromOptions(options).run(p);
        while (job.getState() != PipelineResult.State.RUNNING) {
            Thread.sleep(1000);
        }
        Thread.sleep(1000 * 60 * 5);
        return job;
    }

    public static TestKinesisClientProvider getTestKinesisClientProvider() {
        return new TestKinesisClientProvider();
    }

    /***
     *
     */
    public static class ToTableRow extends DoFn<String, TableRow> {
        @Override
        public void processElement(ProcessContext c) throws Exception {
            checkNotNull(c.element());
            c.output(new TableRow().set("a", c.element()));
        }
    }

    /***
     *
     */
    public static class RecordDataToString extends DoFn<Record, String> {
        @Override
        public void processElement(ProcessContext c) throws Exception {
            checkNotNull(c.element());
            c.output(new String(c.element().getData().array(), Charsets.UTF_8));
        }
    }
}
