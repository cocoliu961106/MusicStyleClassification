package ClassificationModule.OnlinePredicting

import Classifier.NN.Model.NeuralNetModel
import Classifier.NN.Util.Serialization
import FeatureExtractor.MFCC.Model.MFCCProcecure
import FeatureExtractor.MFCC.Util.OnlineWaveFileReader
import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import breeze.linalg.{CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM, SparseVector => BSV, Vector => BV, axpy => brzAxpy, max => Bmax, min => Bmin, sum => Bsum, svd => brzSvd}
import org.apache.spark.rdd.RDD

import scala.collection.SortedMap

/**
  * 2.spark-streaming消费数据，匹配应用层是否含有制定关键字，
  * 如果包含就存储下来，不包含就丢弃
  */

// Spark Streaming作为消费者，根据上传的文件名读取HDFS中的文件，并进行计算
object KafkaConsumer {
  def main(args: Array[String]): Unit = {
    //    创建sparksession
    val conf = new SparkConf().setMaster("local[2]").setAppName("MusicClassification")
    val ssc = new StreamingContext(conf, Seconds(15))
    //    设置中间存储的检查点，可以进行累计计算
    //    ssc.checkpoint("hdfs://master:9000/xxx")
    //    读取kafka数据
    val kafkaParam = Map("metadata.broker.list" -> "spark1:9092,spark2:9092,spark3:9092")
    val topic = "kafka".split(",").toSet
    //    获取新上传的文件的文件名
    val fileNameStream: DStream[String] = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParam, topic).map(_._2)

    // 获取音乐文件的数据部分
    val fileDataStream = fileNameStream.map(f => {
      val fileName = f
      val owfr = new OnlineWaveFileReader(f)
      val sampleRate = owfr.getSampleRate
      val data = new Array[Double](owfr.getDataLen)
      for (i <- 0 until owfr.getDataLen) {
        data(i) = owfr.getData(0)(i)
      }
      (fileName, data, sampleRate)
    })

    // 获取特征
    val MFCCParameterStream = fileDataStream.map(f => {
      val fileName = f._1
      val data = f._2
      val sampleRate = f._3
      val result = new MFCCProcecure().processingData(data).getParameter
      (fileName, result)
    })

    // 进行分类
    val classificationResultStream = MFCCParameterStream.foreachRDD(rdd => {
      if (rdd.isEmpty())
        println("None Music Appear.")
      else {
        val modelPath = "src/ClassificationModule/MusicClassificationNNModel.obj"
        val NNmodel = Serialization.deserialize_file[NeuralNetModel](modelPath)
        val labelMap = SortedMap("blues" -> 1, "classical" -> 2, "country" -> 3, "disco" -> 4, "hiphop" -> 5, "jazz" -> 6, "metal" -> 7, "pop" -> 8, "reggae" -> 9, "rock" -> 10)

        val predictMusicRDD = rdd.map(mf => {
          val fileName = mf._1
          val feature = mf._2
          val classificationIndex = labelMap(fileName.split('.')(0))
          val label = Array.fill(10)(0.0)   // 标签 1×10
          label(classificationIndex - 1) = 1.0
          val labelBDM = new BDM[Double](1, label.length, label)
          val featureBDM = new BDM[Double](1, feature.length, feature)
          (fileName, labelBDM, featureBDM)
        })
        val musicNameRDD = predictMusicRDD.map(f => f._1)
        val musicFeatureRDD = predictMusicRDD.map(f => (f._2, f._3))
        val NNforecast = NNmodel.predict(musicFeatureRDD)
        val NNerror = NNmodel.Loss(NNforecast)
        println(s"NNerror = $NNerror.")

        println("————预测的分类结果————")
        val classificationResultRDD = musicNameRDD.zip(NNforecast)
        classificationResultRDD.foreach(result => {
          val fileName = result._1
          println(fileName + " 的风格分类结果：")
          val forecastResult = result._2
          val classificationIterator = labelMap.keysIterator
          val labelArr = forecastResult.predict_label.toArray
          for (i <- 0 until labelArr.length) {
            println(classificationIterator.next() + "  " + labelArr(i))
          }
          println()
        })
      }
    })

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
  }

}
