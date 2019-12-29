package ClassificationModule.OfflineTraining

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

// 离线训练，读取GTZAN数据集，特征提取，放入spark下的NN进行分类训练
class OfflineTraining {

  // 特征提取
  private def featureExtract(musicFile: Array[File]): Array[(String, Array[Double])] = {

  }

  // 分类
  private def classify(musicFeature: Array[(String, Array[Double])]): Unit = {

  }
}

object OfflineTraining {
  def main(args: Array[String]): Unit = {
    var musicData = new ArrayBuffer[Array[Int]]()
    var labelMap = Map("blues" -> 1, "classical" -> 2, "country" -> 3, "disco" -> 4, "hiphop" -> 5, "jazz" -> 6, "metal" -> 7, "pop" -> 8, "reggae" -> 9, "rock" -> 10)
    val musicPath = "src/data/genres"
    val musicList = new File(musicPath)
    val musicFile = read(musicList)

  }

  def read(musicList: File): Array[File] = {
    val musicClassification = musicList.listFiles.filter(_.isDirectory)
    val musicFile = musicClassification.map(f => {
      f.listFiles
    })
    val musicFileArr = musicFile.reduce((f1, f2) => {
      Array.concat(f1, f2)
    })
    // 打乱样本的顺序
    musicFileArr.sortWith((f1, f2) => {
      val bool: Boolean = (Math.random() - 0.5 < 0)
      bool
    })
  }
}
