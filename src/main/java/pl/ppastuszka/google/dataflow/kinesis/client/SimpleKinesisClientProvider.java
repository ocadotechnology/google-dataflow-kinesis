package pl.ppastuszka.google.dataflow.kinesis.client;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;

/**
 * Created by ppastuszka on 05.12.15.
 */
public class SimpleKinesisClientProvider implements KinesisClientProvider {
    @Override
    public AmazonKinesis getKinesisClient() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final AmazonKinesis INSTANCE = new AmazonKinesisClient(new EnvironmentVariableCredentialsProvider())
                .withRegion(Regions.EU_WEST_1);
    }
}