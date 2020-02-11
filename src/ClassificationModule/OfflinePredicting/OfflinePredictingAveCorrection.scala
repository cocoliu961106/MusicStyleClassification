package ClassificationModule.OfflinePredicting

import java.io.File

import breeze.linalg.{CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM, SparseVector => BSV, Vector => BV, axpy => brzAxpy, max => Bmax, min => Bmin, sum => Bsum, svd => brzSvd}
import org.apache.spark.{SparkConf, SparkContext}
import Classifier.NN.Model.NeuralNetModel
import Classifier.NN.Util.Serialization
import FeatureExtractor.MFCC.Model.MFCCProcecure
import FeatureExtractor.MFCC.Util.WaveFileReader

import scala.collection.mutable.ArrayBuffer
import scala.collection.SortedMap

// 计算每个类别后10首歌真实标签预测概率的平均值
object OfflinePredictingAveCorrection {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[2]").setAppName("MusicClassificationWithAverageCorrection")
    val sc = new SparkContext(conf)
    val sourcePath = "src/data/genres/"
    val labelArr = Array("classical", "country", "hiphop", "jazz", "metal", "pop")
    val labelIterator = labelArr.iterator
    val musicPathArr = labelArr.map(f => sourcePath + f)
    val resultBuffer = new ArrayBuffer[Array[Double]]()
    musicPathArr.foreach(musicPath => {
      val musicList = new File(musicPath)
      val music = musicList.listFiles().slice(90, 100).map(f => {
        musicPath + "/" + f.getName
      })
      val MFCCresultArr = featureExtract(music)
      val classifiicationResultArr = predect(MFCCresultArr, sc)
      resultBuffer += classifiicationResultArr
    })

    val resultArr = resultBuffer.toArray
    resultArr.foreach(result => {
      println("类别" + labelIterator.next() + "的各个类别分类概率为：")
      for ( i <- 0 until result.length) {
        println(labelArr(i) + " " + result(i))
      }
      println()
    })
  }

  // 特征提取
  private def featureExtract(musicPaths: Array[String]): Array[(String, Array[Double])] = {
    var num = 1
    val MFCCresult = musicPaths.map(mp => {
      val musicName = mp.split('/').last
      val wfr: WaveFileReader = new WaveFileReader(mp)
      val data = new Array[Double](wfr.getDataLen)
      for (i <- 0 until wfr.getDataLen) {
        data(i) = wfr.getData()(0)(i)
      }
      val result = new MFCCProcecure().processingData(data).getParameter
      printf("第%d个文件特征提取完毕\n", num)
      num += 1

      (musicName, result)
    })

    MFCCresult
  }

  // 预测每个类别的预测概率
  private def predect(musicFeature: Array[(String, Array[Double])], sc: SparkContext): Array[Double] = {
    val labelMap = SortedMap( "classical" -> 1, "country" -> 2, "hiphop" -> 3, "jazz" -> 4, "metal" -> 5, "pop" -> 6)

    val modelPath = "src/ClassificationModule/MusicClassificationNNModel.obj"
    val NNmodel = Serialization.deserialize_file[NeuralNetModel](modelPath)
    val musicFeatureRDD = sc.parallelize(musicFeature).cache()
    val bc_normalization = sc.broadcast(NNmodel.normalization)
    val predictMusicRDD = musicFeatureRDD.map(mf => {
      val fileName = mf._1
      val feature = mf._2
      for (i <- 0 until feature.length) {
        feature(i) = (bc_normalization.value(0)(i) - feature(i)) / (bc_normalization.value(0)(i) - bc_normalization.value(1)(i)) * 2 - 1
      }
      val classificationIndex = labelMap(fileName.split('.')(0))
      val label = Array.fill(6)(0.0)   // 标签 1×6
      label(classificationIndex - 1) = 1.0
      val labelBDM = new BDM[Double](1, label.length, label)
      val featureBDM = new BDM[Double](1, feature.length, feature)
      (labelBDM, featureBDM)
    })

    val NNforecast = NNmodel.predict(predictMusicRDD)

    val resultBDM = NNforecast.collect().map(nn => nn.predict_label).reduce((n1, n2) =>n1+n2)
    val resultBDM1 = resultBDM :/ 10.0
    val resultArr = resultBDM1.toArray
    resultArr
  }
}
