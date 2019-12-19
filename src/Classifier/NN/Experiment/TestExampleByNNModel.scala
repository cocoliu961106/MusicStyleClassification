package Classifier.NN.Experiment

import Classifier.NN.Model.NeuralNetModel
import Classifier.NN.Util.Serialization
import breeze.linalg.{DenseMatrix => BDM}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

object TestExampleByNNModel {
  def main(args: Array[String]): Unit = {
    val data_path = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\train_d2.txt"
    val model_path = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\NNmodel.obj"

    val conf = new SparkConf().setMaster("local[2]").setAppName("NNtest")
    val sc = new SparkContext(conf)
    Logger.getRootLogger.setLevel(Level.WARN)

    val examples = sc.textFile(data_path).cache() // 每一行的数据格式为    id  label 特征1 特征2 ... 特征m
    val train_d1 = examples.map { line =>
      val f1 = line.split("  ")
      val f = f1.map(f => f.toDouble)
      val y = Array(f(0))
      val x = f.slice(1, f.length)
      (new BDM(1, y.length, y), new BDM(1, x.length, x))
    }
    val train_d = train_d1.map(f => (f._1, f._2))
    val NNmodel = Serialization.deserialize_file[NeuralNetModel](model_path)

    // 利用已有模型进行预测
    val NNforecast = NNmodel.predict(train_d)
    val NNerror = NNmodel.Loss(NNforecast)
    println(s"NNerror = $NNerror.")
    val printf1 = NNforecast.map(f => (f.label.data(0), f.predict_label.data(0))).take(20)
    println("预测结果——实际值：预测值：误差")
    for (i <- 0 until printf1.length) {
      println(printf1(i)._1 + "\t" + printf1(i)._2 + "\t" + (printf1(i)._2 - printf1(i)._1))
    }

    println("权重W(1)")
    println(NNmodel.weights(0).rows + "×" + NNmodel.weights(0).cols)
    println("权重W(2)")
    println(NNmodel.weights(1).rows + "×" + NNmodel.weights(1).cols)
  }
}
