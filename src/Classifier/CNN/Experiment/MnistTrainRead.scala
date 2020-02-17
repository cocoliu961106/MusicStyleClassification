package Classifier.CNN.Experiment

import java.io.{BufferedInputStream, BufferedWriter, FileInputStream, PrintWriter}
import breeze.linalg.{CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM, SparseVector => BSV, Vector => BV, axpy => brzAxpy, max => Bmax, min => Bmin, sum => Bsum, svd => brzSvd}
import Classifier.CNN.Util.GetMnistData
import scala.io.Source

// 读取mnist手写数据集，文件存储格式和train_d3一样
object MnistTrainRead {
  val TRAIN_IMAGES_FILE = "src/data/mnist/train-images.idx3-ubyte";
  val TRAIN_LABELS_FILE = "src/data/mnist/train-labels.idx1-ubyte";

  def main(args: Array[String]): Unit = {
    // 将Mnist训练集转换成可读取的特征、标签格式
    val tr_images = GetMnistData.getImages(TRAIN_IMAGES_FILE)
    val tr_labels = GetMnistData.getLabels(TRAIN_LABELS_FILE)

    println(tr_images(0)(298))
    println(tr_labels(0))

    // 写入文件，前十位代表十个类别，1代表属于该分类，0代表不属于，特征为28×28，
    val writer = new PrintWriter("C:\\Users\\1-49\\Desktop\\spark实验测试数据\\train_d4.txt")
    val bufferedwriter = new BufferedWriter(writer)
    for (i <- 0 until tr_labels.length) {
      val label = Array.fill(10)(0.0)   // 标签 1×10
      label(tr_labels(i).toInt) = 1.0
      val labelBDM = new BDM[Double](1, label.length, label)
      bufferedwriter.write(labelBDM.toString())
      val feature = tr_images(i).clone()
      for (f <- feature) {
        bufferedwriter.write(f + "  ")
      }
      bufferedwriter.newLine()
    }
    bufferedwriter.close()
    writer.close()

    val source = Source.fromFile("C:\\Users\\1-49\\Desktop\\spark实验测试数据\\train_d4.txt")
    val lines = source.getLines()
    println(lines.length)
    source.close()
  }

}
