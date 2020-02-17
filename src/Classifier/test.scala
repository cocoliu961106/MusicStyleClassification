package Classifier

import breeze.linalg.{DenseMatrix => BDM, DenseVector => BDV, accumulate => Accumulate, rot90 => Rot90, sum => Bsum}
import org.apache.spark.{SparkConf, SparkContext}

object Test {
  def main(args: Array[String]): Unit = {
    val a = (0,0,0)
  }

  def softmax(matrix: BDM[Double]): BDM[Double] = {
    val result = new BDM[Double](matrix.rows, matrix.cols)
    for (i <- 0 until result.rows) {
      val sum = Bsum(matrix(i, ::))
      for (j <- 0 until result.cols) {
        result(i, j) = matrix(i, j) / sum
      }
    }
    result
  }

  def relu(matrix: BDM[Double]): BDM[Double] = {
    val s1 = new BDM[Double](matrix.rows, matrix.cols)
    for (i <- 0 until matrix.rows) {
      for (j <- 0 until matrix.cols) {
        if (matrix(i, j) > 0)
          s1(i, j) = matrix(i, j)
        else
          s1(i, j) = 0
      }
    }
    s1
  }

  def d_lRelu(matrix: BDM[Double]): BDM[Double] = {
    val tmp_bdm = new BDM[Double](matrix.rows, matrix.cols)
    for (i <- 0 until tmp_bdm.rows) {
      for (j <- 0 until tmp_bdm.cols) {
        if (matrix(i, j) > 0)
          tmp_bdm(i, j) = 1.0
        else
          tmp_bdm(i, j) = 0.1
      }
    }
    val dact = tmp_bdm // relu偏导
    dact
  }

  def expand(a: BDM[Double], s: Array[Int]): BDM[Double] = {
    // val a = BDM((1.0, 2.0), (3.0, 4.0), (5.0, 6.0))
    // val s = Array(3, 2)
    val sa = Array(a.rows, a.cols) // Array(3, 2)
    var tt = new Array[Array[Int]](sa.length)
    for (ii <- sa.length - 1 to 0 by -1) {
      var h = BDV.zeros[Int](sa(ii) * s(ii)) // (3 × 3)  (2 × 2)
      h(0 to sa(ii) * s(ii) - 1 by s(ii)) := 1
      tt(ii) = Accumulate(h).data // Array(1, 1, 1, 2, 2, 2, 3, 3, 3)
    }
    var b = BDM.zeros[Double](tt(0).length, tt(1).length) // 9 × 4
    for (j1 <- 0 to b.rows - 1) {
      for (j2 <- 0 to b.cols - 1) {
        b(j1, j2) = a(tt(0)(j1) - 1, tt(1)(j2) - 1)
      }
    }
    b
  }

  def convn(m0: BDM[Double], k0: BDM[Double], shape: String): BDM[Double] = {
    //val m0 = BDM((1.0, 1.0, 1.0, 1.0), (0.0, 0.0, 1.0, 1.0), (0.0, 1.0, 1.0, 0.0), (0.0, 1.0, 1.0, 0.0))
    //val k0 = BDM((1.0, 1.0), (0.0, 1.0))
    //val m0 = BDM((1.0, 5.0, 9.0), (3.0, 6.0, 12.0), (7.0, 2.0, 11.0))
    //val k0 = BDM((1.0, 2.0, 0.0), (0.0, 5.0, 6.0), (7.0, 0.0, 9.0))
    val out1 = shape match {
      case "valid" =>
        val m1 = m0 // 输入矩阵
      val k1 = Rot90(Rot90(k0)) // 对于m × n阶矩阵： aij => b(m - i + 1)(n - j + 1)
      val row1 = m1.rows - k1.rows + 1
        val col1 = m1.cols - k1.cols + 1
        var m2 = BDM.zeros[Double](row1, col1)
        for (i <- 0 to row1 - 1) {
          for (j <- 0 to col1 - 1) {
            val r1 = i // 进行卷积计算的区域（对应位置相乘）
            val r2 = r1 + k1.rows - 1
            val c1 = j
            val c2 = c1 + k1.cols - 1
            val mi = m1(r1 to r2, c1 to c2) // 子矩阵 r1-r2 × c1-c2
            m2(i, j) = (mi :* k1).sum
          }
        }
        m2
      case "full" =>
        var m1 = BDM.zeros[Double](m0.rows + 2 * (k0.rows - 1), m0.cols + 2 * (k0.cols - 1))
        for (i <- 0 to m0.rows - 1) {
          for (j <- 0 to m0.cols - 1) {
            m1((k0.rows - 1) + i, (k0.cols - 1) + j) = m0(i, j)
          }
        }
        val k1 = Rot90(Rot90(k0))
        val row1 = m1.rows - k1.rows + 1
        val col1 = m1.cols - k1.cols + 1
        var m2 = BDM.zeros[Double](row1, col1)
        for (i <- 0 to row1 - 1) {
          for (j <- 0 to col1 - 1) {
            val r1 = i
            val r2 = r1 + k1.rows - 1
            val c1 = j
            val c2 = c1 + k1.cols - 1
            val mi = m1(r1 to r2, c1 to c2)
            m2(i, j) = (mi :* k1).sum
          }
        }
        m2
    }
    out1
  }
}

class Person extends Serializable {
  val name = "Nick"
  val age = 18

  override def toString: String = {
    name + "," + age
  }
}
