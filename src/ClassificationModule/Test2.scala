package ClassificationModule

import FeatureExtractor.MFCC.Util.OnlineWaveFileReader
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.SortedMap
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object Test2 {
  def main(args: Array[String]): Unit = {
    val a = ArrayBuffer(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val disorderMusicFileBuffer = new ArrayBuffer[Int]()
    for (i <- a.length - 1 to 0 by -1) {
      val index = (Math.random() * i).round.toInt
      disorderMusicFileBuffer += a(index)
      a.remove(index)
    }
    disorderMusicFileBuffer.toArray.foreach(println(_))
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