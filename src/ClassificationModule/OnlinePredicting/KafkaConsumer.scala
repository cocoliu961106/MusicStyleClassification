package ClassificationModule.OnlinePredicting

import FeatureExtractor.MFCC.Util.OnlineWaveFileReader
import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * 2.spark-streaming消费数据，匹配应用层是否含有制定关键字，
  * 如果包含就存储下来，不包含就丢弃
  */

// Spark Streaming作为消费者，根据上传的文件名读取HDFS中的文件，并进行计算
object KafkaConsumer {
  def main(args: Array[String]): Unit = {
    //    创建sparksession
    val conf = new SparkConf().setMaster("local[2]").setAppName("Consumer")
    val ssc = new StreamingContext(conf, Seconds(15))
    //    设置中间存储的检查点，可以进行累计计算
    //    ssc.checkpoint("hdfs://master:9000/xxx")
    //    读取kafka数据
    val kafkaParam = Map("metadata.broker.list" -> "spark1:9092,spark2:9092,spark3:9092")
    val topic = "kafka".split(",").toSet
    //    获取数据
    val fileNameStream: DStream[String] = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParam, topic).map(_._2)
    val fileDataStream = fileNameStream.map(f => {
      val owfr = new OnlineWaveFileReader(f)
      val isSuccess = owfr.isSuccess
      val bits = owfr.getBitPerSample
      val rate = owfr.getSampleRate
      val channels = owfr.getNumChannels
      val dataLen = owfr.getDataLen
      val data0 = owfr.getData(0).length
      (isSuccess, bits, rate, channels, dataLen, data0)
    })

    fileDataStream.foreachRDD(rdd => {
      rdd.foreach(t => {
        for (i <- 0 until t.productArity) {
          println(t.productElement(i))
        }
      })
    })

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
  }
}
