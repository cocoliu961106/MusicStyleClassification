package Classifier.NN.Experiment

import java.io.{BufferedWriter, PrintWriter}

import Classifier.NN.Util.RandSampleData
import breeze.linalg.{DenseMatrix => BDM, max => Bmax, min => Bmin}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

// 生成随机数样本，并保存在文件中
object generateInputData {
  def main(args: Array[String]): Unit = {
    val sample_n1 = 1000 // 样本数
    val sample_n2 = 5 // 特征维数
    val randsamp1 = RandSampleData.RandM(sample_n1, sample_n2, -10, 10, "sphere") // BDM(Label, 样本数据(一行代表一个样本， 一列代表同一个特征))
    // 归一化[0 1]
    val normmax = Bmax(randsamp1(::, breeze.linalg.*)) // 最大行向量
    val normmin = Bmin(randsamp1(::, breeze.linalg.*)) // 最小行向量
    val norm1 = randsamp1 - (BDM.ones[Double](randsamp1.rows, 1)) * normmin // norm1 ∈ （0，normax - normin）
    val norm2 = norm1 :/ ((BDM.ones[Double](norm1.rows, 1)) * (normmax - normmin)) // norm2 ∈ （0，1）
    // 转换样本train_d
    val randsamp2 = ArrayBuffer[BDM[Double]]() // 每个样本的(label, （特征）)
    val writer = new PrintWriter("C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\train_d2.txt")
    val bufferedWriter = new BufferedWriter(writer)
    for (i <- 0 to sample_n1 - 1) {
      val mi = norm2(i, ::) // 取第i行
      val mi1 = mi.inner // 转置矩阵
      val mi2 = mi1.toArray // 转换成数组
      val mi3 = new BDM(1, mi2.length, mi2) // 1 × (1+特征维数)
      for (j <- 0 to mi3.cols - 1)
        bufferedWriter.write(mi3(0, j) + "  ")
      bufferedWriter.newLine()
    }

    bufferedWriter.close()
    writer.close()

    val source = Source.fromFile("C:\\Users\\1-49\\Desktop\\spark实验测试数据\\MlibTest\\train_d2.txt")
    val lines = source.getLines()
    println(lines.length)
    source.close()
  }
}
