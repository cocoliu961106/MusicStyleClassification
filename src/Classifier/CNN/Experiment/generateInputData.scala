package Classifier.CNN.Experiment

import java.io.{BufferedWriter, PrintWriter}

import breeze.linalg.{DenseMatrix => BDM}

import scala.io.Source
import scala.util.Random

// 模拟单标签分类的数据集，十个类别，1代表属于该分类，0代表不属于，特征为28×28，通过随机数的方法生成标签和特征
object generateInputData {
    def main (args: Array[String]): Unit = {
      val writer = new PrintWriter("C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\train_d3.txt")
      val bufferedwriter = new BufferedWriter(writer)
      for (i <- 0 until 100) {   // 一行代表一个样本
        val randl = Math.floor(Random.nextDouble() * 10).toInt
        val label = Array.fill(10)(0.0)   // 标签 1×10
        label(randl) = 1.0
        val labelBDM = new BDM[Double](1, label.length, label)
        bufferedwriter.write(labelBDM.toString())
        val randf = Array.fill(28 * 28)((Random.nextDouble().formatted("%.2f").toDouble * 255).toInt)    // 特征 1×28×28
        for (num <- randf) {
          bufferedwriter.write(num + "  ")
        }
        bufferedwriter.newLine()
      }
      bufferedwriter.close()
      writer.close()

      val source = Source.fromFile("C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\train_d3.txt")
      val lines = source.getLines()
      println(lines.length)
      source.close()
    }
}
