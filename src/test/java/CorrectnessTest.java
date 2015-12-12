import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.io.Read;
import com.google.cloud.dataflow.sdk.testing.DataflowAssert;
import com.google.cloud.dataflow.sdk.testing.TestPipeline;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.values.PCollection;
import org.junit.Test;

import java.util.List;


public class CorrectnessTest {
    @Test
    public void readerTest() throws Exception {
        final Pipeline p = TestPipeline.create();

        List<String> testData = TestUtils.randomStrings(2000);

        PCollection<String> result = p.
                apply(Read.from(TestUtils.getTestKinesisSource()).withMaxNumRecords(testData.size())).
                apply(ParDo.of(new byteArrayToString()));

        TestUtils.putRecords(testData);
        DataflowAssert.that(result).containsInAnyOrder(testData);
    }

    private static class byteArrayToString extends DoFn<byte[], String> {
        @Override
        public void processElement(ProcessContext c) throws Exception {
            c.output(new String(c.element(), "UTF-8"));
        }
    }
}
