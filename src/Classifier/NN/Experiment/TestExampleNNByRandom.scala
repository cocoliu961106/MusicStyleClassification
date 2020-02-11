package Classifier.NN.Experiment

import Classifier.NN.Model.NeuralNet
import Classifier.NN.Util.RandSampleData
import breeze.linalg.{DenseMatrix => BDM, max => Bmax, min => Bmin}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable.ArrayBuffer


// 直接通过生成随机数的方式训练模型，预测训练集，获得误差
object TestExampleNNByRandom {
  def main (args: Array[String]): Unit = {
    // 1.构造spark对象
    val conf = new SparkConf().setMaster("local[2]").setAppName("NNtest")
    val sc = new SparkContext(conf)

    //*****************************基于经典优化算法测试函数随机生成样本************************//
    // 2.随机生成测试数据
    // 随机数生成
    Logger.getRootLogger.setLevel(Level.WARN)
    val sample_n1 = 1000  // 样本数
    val sample_n2 = 5   // 特征维数
    val randsamp1 = RandSampleData.RandM(sample_n1, sample_n2, -10, 10, "sphere")   // BDM(Label, 样本数据(一行代表一个样本， 一列代表同一个特征))
    // 归一化[0 1]
    val normmax = Bmax(randsamp1(::, breeze.linalg.*))    // 最大行向量
    val normmin = Bmin(randsamp1(::, breeze.linalg.*))    // 最小行向量
    val norm1 = randsamp1 - (BDM.ones[Double](randsamp1.rows, 1)) * normmin   // norm1 ∈ （0，normax - normin）
    val norm2 = norm1 :/ ((BDM.ones[Double](norm1.rows, 1)) * (normmax - normmin))    // norm2 ∈ （0，1）
    // 转换样本train_d
    val randsamp2 = ArrayBuffer[BDM[Double]]()    // 每个样本的(label, （特征）)
    for (i <- 0 to sample_n1 - 1) {
      val mi = norm2(i, ::)   // 取第i行
      val mi1 = mi.inner    // 转置矩阵
      val mi2 = mi1.toArray   // 转换成数组
      val mi3 = new BDM(1, mi2.length, mi2)   // 1 × (1+特征维数)
      randsamp2 += mi3
    }
    val randsamp3 = sc.parallelize(randsamp2, 10)   // 并行化
//    sc.setCheckpointDir("hdfs://spark1:9000/checkpoint")  // 设置检查点
//    randsamp3.checkpoint()
    val train_d = randsamp3.map(f => (new BDM(1, 1, f(::, 0).data), f(::, 1 to -1)))    // 将每个样本划分为(label, (特征))的形式
    // 3.设置训练参数，建立模型
    // opts: 迭代步长，迭代次数，交叉验证比例
    val opts = Array(100.0, 50.0, 0.0)
    train_d.cache()
    val numExamples = train_d.count()   // 样本数
    println(s"numExamples = $numExamples.")
    val NNmodel = new NeuralNet().
      setSize(Array(5, 7, 1)).
      setLayer(3).
      setActivation_function("tanh_opt").
      setLearningRate(2.0).
      setScaling_learningRate(1.0).
      setWeightPenaltyL2(0.0).
      setNonSparsityPenalty(0.0).
      setSparsityTarget(0.05).
      setInputZeroMaskedFraction(0.0).
      setDropoutFraction(0.0).
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
    println("权重W(1)")   // 第一层与第二层连接的权重
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


    //*****************************读取固定样本：来源于经典优化算法测试函数Sphere Model************************//
//    // 2.读取样本数据
//    Logger.getRootLogger.setLevel(Level.WARN)
//    val data_path = ""
//    val examples = sc.textFile(data_path).cache()     // 每一行的数据格式为    id  label 特征1 特征2 ... 特征m
//    val train_d1 = examples.map { line =>
//      val f1 = line.split("\t")
//      val f = f1.map(f => f.toDouble)
//      val id = f(0)
//      val y = Array(f(1))
//      val x = f.slice(2, f.length)
//      (id, new BDM(1, y.length, y), new BDM(1, x.length, x))
//    }
//    val train_d = train_d1                    // 我觉得格式应该是 train_d = train_d.map(f => (f._1, f._2))
//    val opts = Array(100.0, 20.0, 0.0)
//    // 3.设置训练参数，建立模型
//    val NNmodel = new NeuralNet().
//      setSize(Array(5, 7, 1)).
//      setLayer(3).
//      setActivation_function("tanh_opt").
//      setLearningRate(2.0).
//      setScaling_learningRate(1.0).
//      setWeightPenaltyL2(0.0).
//      setNonSparsityPenalty(0.0).
//      setSparsityTarget(0.0).
//      setOutput_function("sigm").
//      NNtrain(train_d, opts)
//
//    // 4.模型测试
//    val NNforecast = NNmodel.predict(train_d.map(f => (f._2, f._3)))
//    val NNerror = NNmodel.Loss(NNforecast)
//    println(s"NNerror = $NNerror.")
//    val printf1 = NNforecast.map(f => (f.label.data(0), f.predict_label.data(0))).take(20)
//    println("预测结果——实际值：预测值：误差")
//    for (i <- 0 until printf1.length) {
//      println(printf1(i)._1 + "\t" + printf1(i)._2 + "t" + (printf1(i)._2 - printf1(i)._1))
//    }
//    println("权重W(1)")   // 第一层与第二层连接的权重
//    val tmpw0 = NNmodel.weights(0)
//    for (i <- 0 to tmpw0.rows - 1) {
//      for (j <- 0 to tmpw0.cols - 1) {
//        println(tmpw0(i, j) + "\t")
//      }
//      println()
//    }
//    println("权重W(2)")
//    val tmpw1 = NNmodel.weights(1)
//    for (i <- 0 to tmpw1.rows - 1) {
//      for (j <- 0 to tmpw1.cols - 1) {
//        println(tmpw0(i, j) + "\t")
//      }
//      println()
  }
}
