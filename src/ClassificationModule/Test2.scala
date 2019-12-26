package ClassificationModule

import FeatureExtractor.MFCC.Util.OnlineWaveFileReader
import org.apache.spark.{SparkConf, SparkContext}

import scala.io.Source

// kafka监听hdfs文件目录，一有新文件上传，获取文件名
object Test2 {
  def main(args: Array[String]): Unit = {

  }
}

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