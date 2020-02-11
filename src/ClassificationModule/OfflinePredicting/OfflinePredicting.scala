package ClassificationModule.OfflinePredicting

import breeze.linalg.{CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM, SparseVector => BSV, Vector => BV, axpy => brzAxpy, max => Bmax, min => Bmin, sum => Bsum, svd => brzSvd}
import org.apache.spark.{SparkConf, SparkContext}
import Classifier.NN.Model.NeuralNetModel
import Classifier.NN.Util.Serialization
import FeatureExtractor.MFCC.Model.MFCCProcecure
import FeatureExtractor.MFCC.Util.WaveFileReader

import scala.collection.SortedMap

// 计算某首歌的预测类别
object OfflinePredicting {
  def main(args: Array[String]): Unit = {
    val musicPaths = Array("src/data/genres/metal/metal.00098.wav", "src/data/genres/metal/metal.00099.wav")
    val MFCCresultArr = featureExtract(musicPaths)
    predect(MFCCresultArr)

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

  // 预测分类
  private def predect(musicFeature: Array[(String, Array[Double])]): Unit = {
    val labelMap = SortedMap( "classical" -> 1, "country" -> 2, "hiphop" -> 3, "jazz" -> 4, "metal" -> 5, "pop" -> 6)
    val conf = new SparkConf().setMaster("local[2]").setAppName("MusicClassificationTest")
    val sc = new SparkContext(conf)

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

    println("————预测的分类结果————")
    NNforecast.collect().foreach(pm => {
      val classificationIterator = labelMap.keysIterator
      val labelArr = pm.predict_label.toArray
      for (i <- 0 until labelArr.length) {
        println(classificationIterator.next() + "  " + labelArr(i))
      }
    })
    /*NNforecast.foreach(pm => {
      val classificationIterator = labelMap.keysIterator
      val labelArr = pm.predict_label.toArray
      for (i <- 0 until labelArr.length) {
        println(classificationIterator.next() + "  " + labelArr(i))
      }
    })*/
  }
}
