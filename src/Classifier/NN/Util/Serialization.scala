package Classifier.NN.Util

import java.io._

object Serialization {
  //序列化（将对象传入，变成字节流）
  def serialize[T](o: T): Array[Byte] = {
    val bos = new ByteArrayOutputStream() //内存输出流，和磁盘输出流从操作上讲是一样的
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(o)
    oos.close()
    bos.toByteArray
  }

  //反序列化
  def deserialize[T](bytes: Array[Byte]): T = {
    val bis = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bis)
    ois.readObject.asInstanceOf[T] //进行类型转换，因为你要返回这个类型
  }

  //文件输出流
  def serialize_file[T](o: T, path: String) = {
    val oos = new ObjectOutputStream(new FileOutputStream(path))
    oos.writeObject(o)
    oos.close()
  }

  //文件输入流
  def deserialize_file[T](path: String): T = {
    val ois = new ObjectInputStream(new FileInputStream(path))
    val o = ois.readObject.asInstanceOf[T] //进行类型转换，因为你要返回这个类型
    ois.close()
    o
  }
}
