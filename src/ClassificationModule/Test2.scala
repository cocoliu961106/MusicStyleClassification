package ClassificationModule

import FeatureExtractor.MFCC.Util.OnlineWaveFileReader
import breeze.linalg.{DenseMatrix => BDM}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Random
import scala.collection.SortedMap
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object Test2 {
  def main(args: Array[String]): Unit = {

    val b1 = BDM((1, 2), (3, 4))
    val b2 = BDM((4, 6), (8, 10))
    /*val conf = new SparkConf().setMaster("local[2]").setAppName("test")
    val sc = new SparkContext(conf)
    val a = List(("aaa", 1.0), ("bbb", 2.0), ("aaa", 3.0), ("ccc", 2.0), ("ccc", 1.0), ("bbb", 3.0), ("aaa", 2.0), ("ccc", 6.0), ("bbb", 7.0))

    val rdd1 = sc.parallelize(a).cache()
    val rdd_t = rdd1.map(f => f._1)
    val rdd_g = rdd1.map(f => {
      f._2 + 1
    })
    val rdd_h = rdd_t.zip(rdd_g)
    val rdd2 = rdd_h.groupByKey().map(x => {
      var num = 0
      var sum = 0.0
      for (i <- x._2) {
        //遍历该值
        sum = sum + i
        num = num + 1
      }
      val avg = sum / num
      (x._1, avg)
    })
    rdd2.foreach(f => {
      println(f._1 + " " + f._2)
    })*/
  }
}

// spark读取hdfs音频文件
/*val conf = new SparkConf()
    .setAppName("kafkaTest")
  val sc = new SparkContext(conf)
  val filePaths = Array("/music/blues.00000.wav", "/music/blues.00001.wav", "/music/blues.00002.wav")
  val filePathsRDD = sc.parallelize(filePaths)
  val fileWordCountRDD = filePathsRDD.map(f => {
    val owfr = new OnlineWaveFileReader(f)
    val isSuccess = owfr.isSuccess
    val bits = owfr.getBitPerSample
    val rate = owfr.getSampleRate
    val channels = owfr.getNumChannels
    val dataLen = owfr.getDataLen
    val data0 = owfr.getData(0).length
    (isSuccess, bits, rate, channels, dataLen, data0)
  })
  fileWordCountRDD.foreach(t => {
    for (i <- 0 until t.productArity) {
      println(t.productElement(i))
    }
  })*/

//  val source = Source.fromFile("hdfs://spark1:9000/Hamlet.txt")
//  val lines = source.getLines()