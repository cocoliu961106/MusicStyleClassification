package Classifier.CNN.Experiment

import java.io.{BufferedInputStream, BufferedWriter, FileInputStream, PrintWriter}
import breeze.linalg.{CSCMatrix => BSM, DenseMatrix => BDM, DenseVector => BDV, Matrix => BM, SparseVector => BSV, Vector => BV, axpy => brzAxpy, max => Bmax, min => Bmin, sum => Bsum, svd => brzSvd}

import scala.io.Source

// 读取mnist手写数据集，文件存储格式和train_d3一样
object MnistRead {
  val TRAIN_IMAGES_FILE = "src/data/mnist/train-images.idx3-ubyte";
  val TRAIN_LABELS_FILE = "src/data/mnist/train-labels.idx1-ubyte";
  val TEST_IMAGES_FILE = "src/data/mnist/t10k-images.idx3-ubyte";
  val TEST_LABELS_FILE = "src/data/mnist/t10k-labels.idx1-ubyte";

  def main(args: Array[String]): Unit = {
    // 将Mnist数据集转换成可读取的特征、标签格式
    val tr_images = getImages(TRAIN_IMAGES_FILE)
    val tr_labels = getLabels(TRAIN_LABELS_FILE)
    val te_images = getImages(TEST_IMAGES_FILE)
    val te_labels = getLabels(TEST_LABELS_FILE)
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

  /**
    * change bytes into a hex string.
    *
    * @param bytes bytes
    * @return the returned hex string
    */
  def bytesToHex(bytes: Array[Byte]): String = {
    var sb = new StringBuffer();
    for (i <- 0 to bytes.length - 1) {
      val hex = Integer.toHexString(bytes(i) & 0XFF)
      if (hex.length() < 2) {
        sb.append(0)
      }
      sb.append(hex)
    }
    sb.toString
  }

  /**
    * get images of 'train' or 'test'
    *
    * @param fileName the file of 'train' or 'test' about image
    * @return one row show a `picture`
    */

  def getImages(fileName: String): Array[Array[Double]] = {
    var x = new Array[Array[Double]](0)
    val bin = new BufferedInputStream(new FileInputStream(fileName))
    val bytes = new Array[Byte](4)
    bin.read(bytes, 0, 4)
    if (!"00000803".equals(bytesToHex(bytes))) {
      throw new RuntimeException("Please select the correct file!")
    } else {
      bin.read(bytes, 0, 4)
      val number = Integer.parseInt(bytesToHex(bytes), 16)
      bin.read(bytes, 0, 4)
      val xPixel = Integer.parseInt(bytesToHex(bytes), 16)
      bin.read(bytes, 0, 4)
      val yPixel = Integer.parseInt(bytesToHex(bytes), 16)
      x = Array.ofDim[Double](number, xPixel * yPixel)
      for (i <- 0 to number - 1) {
        val element = new Array[Double](xPixel * yPixel)
        for (j <- 0 to xPixel * yPixel - 1) {
          element(j) = bin.read()
          // normalization
          //                        element[j] = bin.read() / 255.0;
        }
        x(i) = element
      }
    }
    bin.close()
    x
  }

  def getLabels(fileName: String): Array[Double] = {
    var y: Option[Array[Double]] = None
    val bin = new BufferedInputStream(new FileInputStream(fileName))
    val bytes = new Array[Byte](4)
    bin.read(bytes, 0, 4)
    if (!"00000801".equals(bytesToHex(bytes))) {
      throw new RuntimeException("Please select the correct file!")
    } else {
      bin.read(bytes, 0, 4)
      val number = Integer.parseInt(bytesToHex(bytes), 16)
      y = Some(new Array[Double](number))
      for (i <- 0 to number - 1) {
        y.get(i) = bin.read()
      }
    }
    bin.close()
    y.getOrElse(new Array[Double](0))
  }

}
