package Classifier.NN.Experiment

import Classifier.NN.Model.NeuralNet
import Classifier.NN.Util.Serialization
import breeze.linalg.{DenseMatrix => BDM}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

// 根据生成的随机数样本，进行训练，并对训练集进行预测，并将训练好的模型通过序列化的方式保存在文件中
object TestExampleByFile {
  def main(args: Array[String]): Unit = {
    // 1.构造spark对象
    val conf = new SparkConf().setMaster("local[2]").setAppName("NNtest")
    val sc = new SparkContext(conf)
    Logger.getRootLogger.setLevel(Level.WARN)
    // *****************************读取固定样本：来源于经典优化算法测试函数Sphere Model************************//
    // 2.读取样本数据
    val data_path = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\train_d2.txt"
    val examples = sc.textFile(data_path).cache() // 每一行的数据格式为    label 特征1 特征2 ... 特征m
    val train_d1 = examples.map { line =>
      val f1 = line.split("  ")
      val f = f1.map(f => f.toDouble)
      val y = Array(f(0))
      val x = f.slice(1, f.length)
      (new BDM(1, y.length, y), new BDM(1, x.length, x))
    }
    val train_d = train_d1.map(f => (f._1, f._2))
    val opts = Array(100.0, 50.0, 0.0)
    // 3.设置训练参数，建立模型
    val NNmodel = new NeuralNet().
      setSize(Array(5, 7, 1)).
      setLayer(3).
      setActivation_function("tanh_opt").
      setLearningRate(2.0).
      setScaling_learningRate(1.0).
      setWeightPenaltyL2(0.0).
      setNonSparsityPenalty(0.0).
      setSparsityTarget(0.0).
      setOutput_function("sigm").
      NNtrain(train_d, opts)

    // 4.模型测试
    val NNforecast = NNmodel.predict(train_d)
    val NNerror = NNmodel.Loss(NNforecast)
    println(s"NNerror = $NNerror.")
    val printf1 = NNforecast.map(f => (f.label.data(0), f.predict_label.data(0))).take(20)
    println("预测结果——实际值：预测值：误差")
    for (i <- 0 until printf1.length) {
      println(printf1(i)._1 + "\t" + printf1(i)._2 + "\t" + (printf1(i)._2 - printf1(i)._1))
    }
    println("权重W(1)") // 第一层与第二层连接的权重
    val tmpw0 = NNmodel.weights(0)
    for (i <- 0 to tmpw0.rows - 1) {
      for (j <- 0 to tmpw0.cols - 1) {
        println(tmpw0(i, j) + "\t")
      }
      println()
    }
    println("权重W(2)")
    val tmpw1 = NNmodel.weights(1)
    for (i <- 0 to tmpw1.rows - 1) {
      for (j <- 0 to tmpw1.cols - 1) {
        println(tmpw1(i, j) + "\t")
      }
      println()
    }

    // 将模型序列化保存在文件中
    val save_path = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\NNmodel.obj"
    Serialization.serialize_file(NNmodel, save_path)
    println("模型保存成功!")
  }


}
