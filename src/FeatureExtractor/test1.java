package FeatureExtractor;

import java.util.Arrays;
import java.util.Properties;

public class test1 {
    public static void main(String[] args) {
        int[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] newData;
        newData = Arrays.copyOfRange(data, 2, 7);
        for (int i : newData)
            System.out.print(i + " ");

    }

    public void kafka() {

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
    }
}
