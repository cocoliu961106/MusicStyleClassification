package Classifier.CNN.Experiment

import java.io.{BufferedWriter, PrintWriter}
import breeze.linalg.{CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM, SparseVector => BSV, Vector => BV, axpy => brzAxpy, max => Bmax, min => Bmin, sum => Bsum, svd => brzSvd}

import Classifier.CNN.Util.GetMnistData

import scala.io.Source

// 使用Mnist测试集测试准确度
object MnistTestRead {
  val TEST_IMAGES_FILE = "src/data/mnist/t10k-images.idx3-ubyte"
  val TEST_LABELS_FILE = "src/data/mnist/t10k-labels.idx1-ubyte"
  def main(args: Array[String]): Unit = {

    val te_images = GetMnistData.getImages(TEST_IMAGES_FILE)
    val te_labels = GetMnistData.getLabels(TEST_LABELS_FILE)

    // 写入文件，前十位代表十个类别，1代表属于该分类，0代表不属于，特征为28×28，
    val writer = new PrintWriter("C:\\Users\\1-49\\Desktop\\spark实验测试数据\\train_d5.txt")
    val bufferedwriter = new BufferedWriter(writer)
    for (i <- 0 until te_labels.length) {
      val label = Array.fill(10)(0.0)   // 标签 1×10
      label(te_labels(i).toInt) = 1.0
      val labelBDM = new BDM[Double](1, label.length, label)
      bufferedwriter.write(labelBDM.toString())
      val feature = te_images(i).clone()
      for (f <- feature) {
        bufferedwriter.write(f + "  ")
      }
      bufferedwriter.newLine()
    }
    bufferedwriter.close()
    writer.close()

    val source = Source.fromFile("C:\\Users\\1-49\\Desktop\\spark实验测试数据\\train_d5.txt")
    val lines = source.getLines()
    println(lines.length)
    source.close()
  }
}
