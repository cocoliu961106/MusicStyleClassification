package ClassificationModule.OfflineTraining

import FeatureExtractor.MFCC.Model.MFCCProcecure
import FeatureExtractor.MFCC.Util.WaveFileReader
import Classifier.NN.Model.NeuralNet
import Classifier.NN.Model.NeuralNetModel
import java.io.File

import Classifier.NN.Util.Serialization
import org.apache.spark.{SparkConf, SparkContext}
import breeze.linalg.{CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM, SparseVector => BSV, Vector => BV, axpy => brzAxpy, max => Bmax, min => Bmin, sum => Bsum, svd => brzSvd}

import scala.collection.SortedMap
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/*// 离线训练，读取GTZAN数据集，特征提取，放入spark下的NN进行分类训练
// 有时候训练时会出现所有节点的连接权重变为NaN，目前还不知道原因，多试两次之后就可以了(调整学习率与momentum)
class OfflineTraining(musicFile: Array[File]) {

}*/

object OfflineTraining {
  def main(args: Array[String]): Unit = {
    //    var musicData = new ArrayBuffer[Array[Int]]()
    val musicPath = "src/data/genres"
    val musicList = new File(musicPath)
    val musicFile = read(musicList)

    // 提取每一首歌的特征
    val MFCCresultArr = featureExtract(musicFile) // Array[(fileName: String, feature: Array[Double])]

    // [-1, 1]归一化
    var max = Array.fill(MFCCresultArr(0)._2.length)(0.0)
    var min = Array.fill(MFCCresultArr(0)._2.length)(0.0)
    for (i <- 0 until MFCCresultArr(0)._2.length) {
      for (j <- 0 until MFCCresultArr.length) {
        if (MFCCresultArr(j)._2(i) > max(i))
          max(i) = MFCCresultArr(j)._2(i)
        if (MFCCresultArr(j)._2(i) < min(i))
          min(i) = MFCCresultArr(j)._2(i)
      }
    }
    for (i <- 0 until MFCCresultArr.length) {
      for (j <- 0 until MFCCresultArr(0)._2.length) {
        MFCCresultArr(i)._2(j) = (max(j) - MFCCresultArr(i)._2(j)) / (max(j) - min(j)) * 2 - 1
      }
    }

    /*// 零均值标准化

    val xMean = new Array[Double](MFCCresultArr(0)._2.length)
    val standardDeviation = new Array[Double](MFCCresultArr(0)._2.length)
    for (i <- 0 until MFCCresultArr(0)._2.length) {
      var sum1 = 0.0 // 和
      var sum2 = 0.0 // 平方和
      var variance = 0.0 //方差
      for (j <- 0 until MFCCresultArr.length) {
        sum1 += MFCCresultArr(j)._2(i)
        sum2 += MFCCresultArr(j)._2(i) * MFCCresultArr(j)._2(i)
      }
      variance = sum2 / MFCCresultArr.length - (sum1 / MFCCresultArr.length) * (sum1 / MFCCresultArr.length)
      xMean(i) = sum1 / MFCCresultArr.length
      standardDeviation(i) = Math.sqrt(variance)
    }

    for (i <- 0 until MFCCresultArr.length) {
      for (j <- 0 until MFCCresultArr(0)._2.length) {
        MFCCresultArr(i)._2(j) = (MFCCresultArr(i)._2(j) - xMean(j)) /  standardDeviation(j)
      }
    }*/
    println(MFCCresultArr.length)
    println(MFCCresultArr(0)._1)
    for (i <- 0 until MFCCresultArr(0)._2.length) {
      print(MFCCresultArr(0)._2(i) + "  ")
      println()
    }
    println(MFCCresultArr(1)._1)
    for (i <- 0 until MFCCresultArr(0)._2.length) {
      print(MFCCresultArr(1)._2(i) + "  ")
      println()
    }

    // 得到训练模型并保存
    val trainingModel = classify(MFCCresultArr, max, min)
    val save_path = "src/ClassificationModule/MusicClassificationNNModel.obj"
    Serialization.serialize_file(trainingModel, save_path)
    println("模型保存成功!")

  }

  def read(musicList: File): Array[File] = {
    val musicClassification = musicList.listFiles.filter(_.isDirectory)
    val musicFile = musicClassification.map(f => {
      f.listFiles
    })

    // 取每个类别前90首作为训练集，后10首作为测试集
    val trainingMusicFile = musicFile.map(f => {
      f.slice(0, 90)
    })

    // 将音乐文件二维数组转换为一维数组
    val musicFileArr = trainingMusicFile.reduce((f1, f2) => {
      Array.concat(f1, f2)
    })

    // 打乱样本的顺序(没必要，因为在spark下的NN进行训练是分布式的，样本本来就是随机分配的)
    val musicFileBuffer = musicFileArr.toBuffer
    val disorderMusicFileBuffer = new ArrayBuffer[File]()
    for (i <- musicFileBuffer.length - 1 to 0 by -1) {
      val index = (Math.random() * i).round.toInt
      disorderMusicFileBuffer += musicFileBuffer(index)
      musicFileBuffer.remove(index)
    }
    disorderMusicFileBuffer.toArray

    // 这种方法有时候会报错
    /*musicFileArr.sortWith((f1, f2) => {
      if(Math.random() - 0.5 > 0)
        true
      else
        false
    })*/
  }

  // 特征提取
  private def featureExtract(musicFile: Array[File]): Array[(String, Array[Double])] = {
    var num = 1
    val MFCCresult = musicFile.map(mf => {
      val fileName = mf.getName

      // 1.将原语音文件数字化表示
      val wfr: WaveFileReader = new WaveFileReader(mf.getPath)
      // 获取真实数据部分，也就是我们拿来特征提取的部分
      val data = new Array[Double](wfr.getDataLen)
      for (i <- 0 until wfr.getDataLen) {
        data(i) = wfr.getData()(0)(i)
      }

      // 2.进行特征提取
      val result = new MFCCProcecure().processingData(data).getParameter
      printf("第%d个文件特征提取完毕\n", num)
      num += 1
      (fileName, result)
    })

    MFCCresult
  }

  // 分类
  private def classify(musicFeature: Array[(String, Array[Double])], max: Array[Double], min: Array[Double]): NeuralNetModel = {
    val labelMap = SortedMap( "classical" -> 1, "country" -> 2, "hiphop" -> 3, "jazz" -> 4, "metal" -> 5, "pop" -> 6)
    // 1.构造spark对象
    val conf = new SparkConf().setMaster("local[2]").setAppName("MusicClassify")
    val sc = new SparkContext(conf)

    // 2.并行化音乐数据并进行训练
    val musicFeatureRDD = sc.parallelize(musicFeature).cache()

    val trainMusicRDD = musicFeatureRDD.map(mf => {
      val fileName = mf._1
      val feature = mf._2
      val classificationIndex = labelMap(fileName.split('.')(0))
      val label = Array.fill(6)(0.0) // 标签 1×10
      label(classificationIndex - 1) = 1.0
      val labelBDM = new BDM[Double](1, label.length, label)
      val featureBDM = new BDM[Double](1, feature.length, feature)
      (labelBDM, featureBDM)
    })

    // 设置训练参数，训练模型
    val opts = Array(10.0, 200.0, 0.0) // (batch大小， epoach循环训练次数，交叉验证比例)
    val NNmodel = new NeuralNet().
      setSize(Array(78, 15, 6)).
      setLayer(3).
      setActivation_function("lrelu").
      setLearningRate(0.05).
      setScaling_learningRate(1.0).
      setWeightPenaltyL2(0.5).
      setNonSparsityPenalty(0.0).
      setSparsityTarget(0.0).
      setDropoutFraction(0.0).
      setMomentum(0.9).
      setOutput_function("softmax").
      NNtrain(trainMusicRDD, opts, max, min)

    NNmodel
  }
}
