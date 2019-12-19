package Classifier.CNN.Experiment

import Classifier.CNN.Model.CNN
import breeze.linalg.{DenseMatrix => BDM}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}


// 根据保存样本的文件进行训练，并直接从训练集中进行预测，得到误差，获取分类效果
object TestExampleCNN {

  def main(args: Array[String]) {
    //1 构建Spark对象
    val conf = new SparkConf().setMaster("local[4]").setAppName("CNNtest")
    val sc = new SparkContext(conf)

    //2 测试数据
    Logger.getRootLogger.setLevel(Level.WARN)
    val data_path = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\train_d3.txt"
    val data_path1 = "C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\train_d4.txt"
    val allExamples = sc.textFile(data_path1)
    val examples = sc.makeRDD(allExamples.take(6000)).cache()
    val train_d1 = examples.map { line =>
      val f1 = line.split("  ")
      val f = f1.map(f => f.toDouble)
      val y = f.slice(0, 10) // 标签
    val x = f.slice(10, f.length) // 特征
      (new BDM(1, y.length, y), (new BDM(1, x.length, x)).reshape(28, 28) / 255.0)
    }
    val train_d = train_d1.map(f => (f._1, f._2)) // (标签行向量，特征矩阵)
    //3 设置训练参数，建立模型
    // opts:迭代步长，迭代次数，交叉验证比例
    val opts = Array(10.0, 1.0, 0.0)
    train_d.cache
    val numExamples = train_d.count()
    println(s"numExamples = $numExamples.")
    val CNNmodel = new CNN().
      setMapsize(new BDM(1, 2, Array(28.0, 28.0))).
      setTypes(Array("i", "c", "s", "c", "s")).
      setLayer(5).
      setOnum(10).
      setOutputmaps(Array(0.0, 6.0, 0.0, 12.0, 0.0)).
      setKernelsize(Array(0.0, 5.0, 0.0, 5.0, 0.0)).
      setScale(Array(0.0, 0.0, 2.0, 0.0, 2.0)).
      setAlpha(0.1).
      CNNtrain(train_d, opts)

    //4 模型测试
    val CNNforecast = CNNmodel.predict(train_d) // 预测训练数据
    val CNNerror = CNNmodel.Loss(CNNforecast)
    println(s"NNerror = $CNNerror.")
    val printf1 = CNNforecast.map(f => (f.label.data, f.predict_label.data, f.error.data)).take(20)
    println("预测值")
    for (i <- 0 until printf1.length) {
      val ini = printf1(i)._1.mkString("\t")
      println(ini)
      val outi = printf1(i)._2.mkString("\t")
      println(outi)
      val errori = printf1(i)._3.mkString("\t")
      println(errori)
      println()
    }

  }
}
