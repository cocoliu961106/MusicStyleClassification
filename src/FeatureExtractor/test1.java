package FeatureExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class test1 {
    public static void main(String[] args) {
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        BeforeFFT preSteps = new BeforeFFT();
        double[] preStepsResult = preSteps.preEnhance(data);
        for(double value : preStepsResult) {
            System.out.println(value);
        }
    }

    /*public void kafka() {

        // 创建生产者
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "broker1:9092,broker2:9092");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<String, String>(properties);
        // 简单消息发送
        ProducerRecord<String, String> record = new ProducerRecord<String, String>("CustomerCountry", "West", "France");
        producer.send(record);

        // 同步发送消息
        ProducerRecord<String, String> record = new ProducerRecord<String, String>("CustomerCountry", "West", "France");
        try {
            RecordMetadata recordMetadata = producer.send(record).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 异步发送消息
        ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>("CustomerCountry", "Huston", "America");
        producer.send(producerRecord, new DemoProducerCallBack());
        class DemoProducerCallBack implements Callback {
            public void onCompletion(RecordMetadata metadata, Exceptionexception) {
                if (exception != null) {
                    exception.printStackTrace();
                }
            }
        }

        // producer中启用压缩  压缩算法使用的是 GZIP
        private Properties properties = new Properties();
        properties.put("bootstrap.servers", "192.168.1.9:9092");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("compression.type", "gzip");
        Producer<String, String> producer = new KafkaProducer<String, String>(properties);
        ProducerRecord<String, String> record = new ProducerRecord<String, String>("CustomerCountry", "Precision Products", "France");

        // 创建消费者
        Properties properties = new Properties();
        properties.put("bootstrap.server", "192.168.1.9:9092");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        // 主题订阅（参数可以是正则表达式）
        consumer.subscribe(Collections.singletonList("customerTopic"));

        // 轮询的方式定期去kafka broker中进行数据检索，有数据就进行消费
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(100));
                for (ConsumerRecord<String, String> record : records) {
                    int updateCount = 1;
                    if (map.containsKey(record.value())) {
                        updateCount = (int) map.get(record.value() + 1);
                    }
                    map.put(record.value(), updateCount);
                }
            }
        } finally {
            consumer.close();
        }
    }*/
}

class BeforeFFT {
    // 预加重
    public double[] preEnhance(double data[]) {
        // 将信号值域先置于-1到1之间
        double max = 0;
        for (double i : data) {
            if (Math.abs(i) > max)
                max = Math.abs(i);
        }
        double[] result = new double[data.length];
        data[0] = data[0] / max;
        result[0] = data[0];
        for (int i = 1; i < data.length; i++) {
            data[i] = data[i] / max;
            result[i] = data[i] - 0.957 * data[i - 1];
        }
        return result;
    }

    // 分帧
    public double[][] framing(double[] data, int fLength) {
        ArrayList<double[]> frameData = new ArrayList<>();
        int start = 0, step = fLength / 2;
        // 最后一帧时间不够直接舍弃
        double[] currentFrameData;
        while (start < data.length) {
            if (start + fLength > data.length)
                break;
            currentFrameData = Arrays.copyOfRange(data, start, start + fLength);
            frameData.add(currentFrameData);
            start = start + step;
        }
        double[][] result = new double[frameData.size()][];
        return frameData.toArray(result);
    }

    // 加窗
    public void HammingWindow(double[][] frameData) {
        for (int i = 0; i < frameData.length; i++) {
            for (int n = 0; n < frameData[0].length; n++) {
                double currentWindowValue = 0.5 - 0.5 * Math.cos((2 * Math.PI * n) / (frameData[0].length - 1));
                frameData[i][n] = frameData[i][n] * currentWindowValue;
            }
        }
    }
}