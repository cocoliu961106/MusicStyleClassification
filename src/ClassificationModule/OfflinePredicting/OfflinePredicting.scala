package ClassificationModule.OfflinePredicting

import java.io.File

import breeze.linalg.{CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM, SparseVector => BSV, Vector => BV, axpy => brzAxpy, max => Bmax, min => Bmin, sum => Bsum, svd => brzSvd}
import org.apache.spark.{SparkConf, SparkContext}
import Classifier.NN.Model.NeuralNetModel
import Classifier.NN.Util.Serialization
import FeatureExtractor.MFCC.Model.MFCCProcecure
import FeatureExtractor.MFCC.Util.WaveFileReader

import scala.collection.SortedMap
import scala.collection.mutable.ArrayBuffer

object OfflinePredicting {
  def main(args: Array[String]): Unit = {
    val musicPaths = Array("src/data/genres/pop/pop.00010.wav")
    val MFCCresultArr = featureExtract1(musicPaths)
    predict1(MFCCresultArr)

  }

  private def featureExtract1(musicPaths: Array[String]): Array[(String, Array[Array[Double]])] = { // 每首歌[文件名， 帧数（特征参数）]
    var num = 1
    val MFCCresult = musicPaths.map(mp => {
      val musicName = mp.split('/').last
      val wfr: WaveFileReader = new WaveFileReader(mp)
      val data = new Array[Double](wfr.getDataLen)
      for (i <- 0 until wfr.getDataLen) {
        data(i) = wfr.getData()(0)(i)
      }
      // 随机抽取100帧每首音乐靠中间的音乐帧片段的特征参数，两头可能没声音，
      val data1 = data.slice((data.length * 0.1).round.toInt, (data.length * 0.9).round.toInt)
      val result = new MFCCProcecure().processingData(data1, wfr.getSampleRate).getParameter
      val buff = result.toBuffer
      val result1 = new ArrayBuffer[Array[Double]]()
      for (i <- result.length - 1 to result.length - 100 by -1) {
        val index = (Math.random() * i).round.toInt
        result1 += buff(index)
        buff.remove(index)
      }
      printf("第%d个文件特征提取完毕\n", num)
      num += 1

      (musicName, result1.toArray)
    })

    MFCCresult
  }

  /* // 特征提取
   private def featureExtract(musicPaths: Array[String]): Array[(String, Array[Double])] = {
     var num = 1
     val MFCCresult = musicPaths.map(mp => {
       val musicName = mp.split('/').last
       val wfr: WaveFileReader = new WaveFileReader(mp)
       val data = new Array[Double](wfr.getDataLen)
       for (i <- 0 until wfr.getDataLen) {
         data(i) = wfr.getData()(0)(i)
       }
       val result = new MFCCProcecure().processingData(data, wfr.getSampleRate).getParameter
       printf("第%d个文件特征提取完毕\n", num)
       num += 1

       (musicName, result)
     })

     MFCCresult
   }*/

  private def predict1(musicFeature: Array[(String, Array[Array[Double]])]): Unit = {
    val labelMap = SortedMap("blues" -> 1, "classical" -> 2, "country" -> 3, "disco" -> 4, "hiphop" -> 5, "jazz" -> 6, "metal" -> 7, "pop" -> 8, "reggae" -> 9, "rock" -> 10)
    val conf = new SparkConf().setMaster("local[2]").setAppName("MusicClassificationTest")
    val sc = new SparkContext(conf)

    val modelPath = "src/ClassificationModule/MusicClassificationNNModel.obj"
    val NNmodel = Serialization.deserialize_file[NeuralNetModel](modelPath)

    val musicRDD = sc.parallelize(musicFeature).cache()

    val musicFeatureRDD = musicRDD.flatMap(mf => { // 文件名， 帧数（特征参数） => 文件名，特征参数
      val result = mf._2.map(g => {
        (mf._1, g)
      })
      result
    })
    val musicNameRDD = musicFeatureRDD.map(f => f._1)
    val predictMusicRDD = musicFeatureRDD.map(mf => {
      val fileName = mf._1
      val feature = mf._2
      val classificationIndex = labelMap(fileName.split('.')(0))
      val label = Array.fill(10)(0.0) // 标签 1×10
      label(classificationIndex - 1) = 1.0
      val labelBDM = new BDM[Double](1, label.length, label)
      val featureBDM = new BDM[Double](1, feature.length, feature)
      (labelBDM, featureBDM)
    })

    val NNforecast = NNmodel.predict(predictMusicRDD)
    val NNerror = NNmodel.Loss(NNforecast)
    println(s"NNerror = $NNerror.")

    val resultRDD = musicNameRDD.zip(NNforecast)
    val resultRDD1 = resultRDD.groupByKey().map(x => { // (文件名，CompactBuffer((label, feature,  predict_label, error)...))
      var num = 0.0
      var sum = BDM.zeros[Double](1, 10)
      for (i <- x._2) {
        sum = sum + i.predict_label
        num = num + 1
      }
      val avg: BDM[Double] = sum / num
      (x._1, avg)
    })

    println("————预测的分类结果————")
    resultRDD1.foreach(res => {
      println(res._1)
      val classificationIterator = labelMap.keysIterator
      val labelArr = res._2.toArray
      for (i <- 0 until labelArr.length) {
        println(classificationIterator.next() + "  " + labelArr(i))
      }
      println()
    })
  }

  /*// 预测分类
  private def predict(musicFeature: Array[(String, Array[Double])]): Unit = {
    val labelMap = SortedMap("blues" -> 1, "classical" -> 2, "country" -> 3, "disco" -> 4, "hiphop" -> 5, "jazz" -> 6, "metal" -> 7, "pop" -> 8, "reggae" -> 9, "rock" -> 10)
    val conf = new SparkConf().setMaster("local[2]").setAppName("MusicClassificationTest")
    val sc = new SparkContext(conf)

    val modelPath = "src/ClassificationModule/MusicClassificationNNModel.obj"
    val NNmodel = Serialization.deserialize_file[NeuralNetModel](modelPath)

    val musicFeatureRDD = sc.parallelize(musicFeature).cache()

    val predictMusicRDD = musicFeatureRDD.map(mf => {
      val fileName = mf._1
      val feature = mf._2
      val classificationIndex = labelMap(fileName.split('.')(0))
      val label = Array.fill(10)(0.0)   // 标签 1×10
      label(classificationIndex - 1) = 1.0
      val labelBDM = new BDM[Double](1, label.length, label)
      val featureBDM = new BDM[Double](1, feature.length, feature)
      (labelBDM, featureBDM)
    })

    val NNforecast = NNmodel.predict(predictMusicRDD)
    val NNerror = NNmodel.Loss(NNforecast)
    println(s"NNerror = $NNerror.")

    println("————预测的分类结果————")
    NNforecast.foreach(pm => {
      val classificationIterator = labelMap.keysIterator
      val labelArr = pm.predict_label.toArray
      for (i <- 0 until labelArr.length) {
        println(classificationIterator.next() + "  " + labelArr(i))
      }
    })
  }*/
}
