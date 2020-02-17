package Classifier.CNN.Experiment

import Classifier.CNN.Model.CNNModel
import Classifier.NN.Util.Serialization
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import breeze.linalg.{DenseMatrix => BDM}

object TestExampleCNN {
  def main(args: Array[String]): Unit = {
    val data_path = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\train_d5.txt"
    val model_path = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\CNNmodel.obj"

    val conf = new SparkConf().setMaster("local[2]").setAppName("MnistTest")
    val sc = new SparkContext(conf)
    Logger.getRootLogger.setLevel(Level.WARN)

    val allExamples = sc.textFile(data_path)
    val examples = sc.makeRDD(allExamples.take(10000)).cache()
    val test_d = examples.map { line =>
      val f1 = line.split("  ")
      val f = f1.map(f => f.toDouble)
      val y = f.slice(0, 10) // 标签
    val x = f.slice(10, f.length) // 特征
      (new BDM(1, y.length, y), (new BDM(1, x.length, x)).reshape(28, 28) / 255.0)
    }

    val CNNmodel = Serialization.deserialize_file[CNNModel](model_path)
    val CNNforecast = CNNmodel.predict(test_d)
    val classifierResult = CNNforecast.map(f => (f.label, f.predict_label))

    val resultEachLabel = Array.fill[Array[Int]](10)(Array(0,0,0)) // (TP,FP,FN)
    classifierResult.collect().foreach(f => {
      val label = f._1.toArray
      val predict_label = f._2.toArray
      val labelNum = label.indexOf(1)
      var max = 0.0
      predict_label.foreach(ele => {
        if (ele > max)
          max = ele
      })
      val predict_label_num = predict_label.indexOf(max)
      if (labelNum == predict_label_num) {
        resultEachLabel(labelNum)(0) = resultEachLabel(labelNum)(0) + 1    // TP
      } else {
        resultEachLabel(predict_label_num)(1) = resultEachLabel(predict_label_num)(1) + 1    // FP
        resultEachLabel(labelNum)(2) = resultEachLabel(labelNum)(2) + 1    // FN
      }
    })


    for (result <- resultEachLabel) {
      val precision = result(0) / (result(0) + result(1))
      val recall = result(0) / (result(0) + result(2))
      println(precision + " " + recall)
    }
  }
}
