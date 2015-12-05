package pl.ppastuszka.google.dataflow.kinesis.client;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;

import java.io.Serializable;

/**
 * Created by ppastuszka on 05.12.15.
 */
public interface KinesisClientProvider extends Serializable {
    AmazonKinesis getKinesisClient();
}
