package Classifier.CNN.Util

import java.io.{BufferedInputStream, FileInputStream}

object GetMnistData {
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
}
