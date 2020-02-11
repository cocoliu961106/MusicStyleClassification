package ClassificationModule.OnlinePredicting

import java.io.IOException
import java.util.Properties

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, LocatedFileStatus, Path, RemoteIterator}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

// 监听HDFS文件目录，将新出现的文件的文件名名作为消息发送给Spark Streaming
class KafkaProduceMsg extends Runnable {

  private val BROKER_LIST = "spark1:9092,spark2:9092,spark3:9092"
  private val TOPIC = "kafka"
  private val DIR = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\catalogue.txt"

  /**
    * 1、配置属性
    * metadata.broker.list : kafka集群的broker
    * serializer.class : 如何序列化发送消息
    * request.required.acks : 1代表需要broker接收到消息后acknowledgment,默认是0
    * producer.type : 默认就是同步sync
    */
  private val props = new Properties()
  props.put("bootstrap.servers", BROKER_LIST)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("request.required.acks", "1")
  props.put("producer.type", "async")

  private val producer = new KafkaProducer[String, String](props)

  def run(): Unit = {
    try {
      val conf: Configuration = new Configuration()
      conf.set("fs.defaultFS", "hdfs://spark1:9000")
      var fs: FileSystem = null
      fs = FileSystem.get(conf)
      // 第一次先遍历所有文件,文件名放入fileNameList
      var lt: RemoteIterator[LocatedFileStatus] = fs.listFiles(new Path("hdfs://spark1:9000/music"), true)
      var fileNameList: List[String] = List()
      while (lt.hasNext()) {
        val file: LocatedFileStatus = lt.next()
        if (file.isFile()) {
          val path: Path = file.getPath()
          val fileName = path.getName
          println("文件名:[" + fileName + "]"); //只是文件名，没有路径信息
          fileNameList = fileNameList :+ fileName
        } else {
          val path: Path = file.getPath()
          println("目录:[" + path.toString() + "]")
        }
      }
      println("开始生产消息！！！！！！！！！！")

      // 将新文件的文件名放入newFileNameList
      while (true) {
        var newFileNameList: List[String] = List()
        var tempFileNameList: List[String] = List()
        var nextlt: RemoteIterator[LocatedFileStatus] = fs.listFiles(new Path("hdfs://spark1:9000/music"), true)
        Thread.sleep(15000)
        while (nextlt.hasNext()) {
          val file: LocatedFileStatus = nextlt.next()
          if (file.isFile()) {
            val path: Path = file.getPath()
            val fileName = path.getName
            //            println(fileName)
            tempFileNameList = tempFileNameList :+ fileName
            if (!(fileNameList.contains(fileName))) {
              newFileNameList = newFileNameList :+ fileName
            }
          }
        }

        //        println("新增的文件名：")
        //        newFileNameList.foreach(println(_))
        //        println()
        lt = nextlt
        fileNameList = tempFileNameList
        newFileNameList.foreach(f => {
          val record = new ProducerRecord[String, String](this.TOPIC, "key", f)
          println(record)
          producer.send(record)
        })
        println("完成一次消息生产")
      }
    } catch {
      case e: IOException => e.printStackTrace()
    }
  }
}

object Msg {
  def main(args: Array[String]): Unit = {
    new Thread(new KafkaProduceMsg()).start()
  }

}