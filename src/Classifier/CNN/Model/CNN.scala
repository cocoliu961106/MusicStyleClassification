package Classifier.CNN.Model

import breeze.linalg.{DenseMatrix => BDM, DenseVector => BDV, accumulate => Accumulate, rot90 => Rot90, sum => Bsum}
import breeze.numerics.{exp => Bexp, tanh => Btanh}
import org.apache.spark.Logging
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import scala.math._

/**
  * types：网络层类别
  * outputmaps：特征map数量
  * kernelsize：卷积核k大小
  * k: 卷积核
  * b: 偏置
  * dk: 卷积核的偏导
  * db: 偏置的偏导
  * scale: pooling大小
  */
case class CNNLayers(
                      types: String,
                      outputmaps: Double,
                      kernelsize: Double,
                      scale: Double,
                      k: Array[Array[BDM[Double]]],
                      b: Array[Double],
                      dk: Array[Array[BDM[Double]]],
                      db: Array[Double]) extends Serializable

/**
  * CNN(convolution neural network)卷积神经网络
  */

class CNN(
           private var mapsize: BDM[Double], // 输入层大小
           private var types: Array[String], // 网络层类型
           private var layer: Int, // 层数
           private var onum: Int, // 输出维度
           private var outputmaps: Array[Double], // 特征map数量
           private var kernelsize: Array[Double], // 卷积核大小
           private var scale: Array[Double], // 池化层尺寸
           private var alpha: Double) extends Serializable with Logging {
  //                var mapsize = new BDM(1, 2, Array(28.0, 28.0))
  //                var types = Array("i", "c", "s", "c", "s")
  //                var layer = 5
  //                var onum = 10
  //                var outputmaps = Array(0.0, 6.0, 0.0, 12.0, 0.0)
  //                var kernelsize = Array(0.0, 5.0, 0.0, 5.0, 0.0)
  //                var scale = Array(0.0, 0.0, 2.0, 0.0, 2.0)
  //                var alpha = 1.0

  def this() = this(new BDM(1, 2, Array(28.0, 28.0)),
    Array("i", "c", "s", "c", "s"), 5, 10,
    Array(0.0, 6.0, 0.0, 12.0, 0.0),
    Array(0.0, 5.0, 0.0, 5.0, 0.0),
    Array(0.0, 0.0, 2.0, 0.0, 2.0),
    1.0)

  /** 设置输入层大小. Default: [28, 28]. */
  def setMapsize(mapsize: BDM[Double]): this.type = {
    this.mapsize = mapsize
    this
  }

  /** 设置网络层类别. Default: [1"i", "c", "s", "c", "s"]. */
  def setTypes(types: Array[String]): this.type = {
    this.types = types
    this
  }

  /** 设置网络层数. Default: 5. */
  def setLayer(layer: Int): this.type = {
    this.layer = layer
    this
  }

  /** 设置输出维度. Default: 10. */
  def setOnum(onum: Int): this.type = {
    this.onum = onum
    this
  }

  /** 设置特征map数量. Default: [0.0, 6.0, 0.0, 12.0, 0.0]. */
  def setOutputmaps(outputmaps: Array[Double]): this.type = {
    this.outputmaps = outputmaps
    this
  }

  /** 设置卷积核k大小. Default: [0.0, 5.0, 0.0, 5.0, 0.0]. */
  def setKernelsize(kernelsize: Array[Double]): this.type = {
    this.kernelsize = kernelsize
    this
  }

  /** 设置scale大小. Default: [0.0, 0.0, 2.0, 0.0, 2.0]. */
  def setScale(scale: Array[Double]): this.type = {
    this.scale = scale
    this
  }

  /** 设置学习因子. Default: 1. */
  def setAlpha(alpha: Double): this.type = {
    this.alpha = alpha
    this
  }

  /** 卷积神经网络层参数初始化. */
  def CnnSetup: (Array[CNNLayers], BDM[Double], BDM[Double], Double) = { // 每一层的参数配置  输出层偏置  输出层权重  学习率
    var inputmaps1 = 1.0 // 输入的层数（相对于本层来说的输入）
  var mapsize1 = mapsize // 输出的规模（先定义为输入层的尺寸，后面根据网络层类型再进行更改）
  var confinit = ArrayBuffer[CNNLayers]()
    for (l <- 0 to layer - 1) { // layer
      val type1 = types(l)
      val outputmap1 = outputmaps(l)
      val kernelsize1 = kernelsize(l)
      val scale1 = scale(l)
      val layersconf = if (type1 == "s") { // 每一层参数初始化
        mapsize1 = mapsize1 / scale1
        val b1 = Array.fill(inputmaps1.toInt)(0.0) // 池化层不需要偏置和卷积核
        val ki = Array(Array(BDM.zeros[Double](1, 1)))
        new CNNLayers(type1, outputmap1, kernelsize1, scale1, ki, b1, ki, b1)
      } else if (type1 == "c") {
        mapsize1 = mapsize1 - kernelsize1 + 1.0 // 因为输出尺寸 = 输入尺寸 - 卷积核尺寸 + 1
        val fan_out = outputmap1 * math.pow(kernelsize1, 2)
        val fan_in = inputmaps1 * math.pow(kernelsize1, 2)
        val ki = ArrayBuffer[Array[BDM[Double]]]()
        for (i <- 0 to inputmaps1.toInt - 1) { // input map
          val kj = ArrayBuffer[BDM[Double]]()
          for (j <- 0 to outputmap1.toInt - 1) { // output map    卷积核矩阵随机初始化
            val kk = (BDM.rand[Double](kernelsize1.toInt, kernelsize1.toInt) - 0.5) * 2.0 * sqrt(6.0 / (fan_in + fan_out))
            kj += kk
          }
          ki += kj.toArray
        }
        val b1 = Array.fill(outputmap1.toInt)(0.0)
        inputmaps1 = outputmap1
        new CNNLayers(type1, outputmap1, kernelsize1, scale1, ki.toArray, b1, ki.toArray, b1)
      } else {
        val ki = Array(Array(BDM.zeros[Double](1, 1)))
        val b1 = Array(0.0)
        new CNNLayers(type1, outputmap1, kernelsize1, scale1, ki, b1, ki, b1)
      }
      confinit += layersconf
    }
    val fvnum = mapsize1(0, 0) * mapsize1(0, 1) * inputmaps1 // 行数 ×列数 × 深度
    val ffb = BDM.zeros[Double](onum, 1)
    val ffW = (BDM.rand[Double](onum, fvnum.toInt) - 0.5) * 2.0 * sqrt(6.0 / (onum + fvnum))
    (confinit.toArray, ffb, ffW, alpha)
  }

  /**
    * 运行卷积神经网络算法.
    */
  def CNNtrain(train_d: RDD[(BDM[Double], BDM[Double])], opts: Array[Double]): CNNModel = {
    val sc = train_d.sparkContext
    var initStartTime = System.currentTimeMillis()
    var initEndTime = System.currentTimeMillis()
    // 参数初始化配置
    var (cnn_layers, cnn_ffb, cnn_ffW, cnn_alpha) = CnnSetup
    // 样本数据划分：训练数据、交叉检验数据
    val validation = opts(2)
    val splitW1 = Array(1.0 - validation, validation)
    val train_split1 = train_d.randomSplit(splitW1, System.nanoTime())
    val train_t = train_split1(0) // 训练集
    val train_v = train_split1(1) // 交叉验证集
    // m:训练样本的数量
    val m = train_t.count
    // 计算batch的数量
    val batchsize = opts(0).toInt // batch大小
    val numepochs = opts(1).toInt // 迭代次数
    val numbatches = (m / batchsize).toInt // batch数
    var rL = Array.fill(numepochs * numbatches.toInt)(0.0)
    var n = 0
    // numepochs是循环的次数
    for (i <- 1 to numepochs) {
      initStartTime = System.currentTimeMillis()
      val splitW2 = Array.fill(numbatches)(1.0 / numbatches)
      // 根据分组权重，随机划分每组样本数据
      for (l <- 1 to numbatches) {
        // 权重
        val bc_cnn_layers = sc.broadcast(cnn_layers) // 每一层网络的配置信息
        val bc_cnn_ffb = sc.broadcast(cnn_ffb) // 输出层偏置
        val bc_cnn_ffW = sc.broadcast(cnn_ffW) // 输出层权重

        // 样本划分
        val train_split2 = train_t.randomSplit(splitW2, System.nanoTime())
        val batch_xy1 = train_split2(l - 1)

        // CNNff是进行前向传播
        // net = cnnff(net, batch_x);
        val train_cnnff = CNN.CNNff(batch_xy1, bc_cnn_layers, bc_cnn_ffb, bc_cnn_ffW)

        // CNNbp是后向传播
        // net = cnnbp(net, batch_y);
        val train_cnnbp = CNN.CNNbp(train_cnnff, bc_cnn_layers, bc_cnn_ffb, bc_cnn_ffW)

        // 权重更新
        //  net = cnnapplygrads(net, opts);
        val train_nnapplygrads = CNN.CNNapplygrads(train_cnnbp, bc_cnn_ffb, bc_cnn_ffW, cnn_alpha)
        cnn_ffW = train_nnapplygrads._1 // 输出层权重
        cnn_ffb = train_nnapplygrads._2 // 输出层偏置
        cnn_layers = train_nnapplygrads._3 // 每一层的配置

        // error and loss
        // 输出误差计算
        // net.L = 1/2* sum(net.e(:) .^ 2) / size(net.e, 2);
        val rdd_loss1 = train_cnnbp._1.map(f => f._5) // 实际输出与真实标签之差
        val (loss2, counte) = rdd_loss1.treeAggregate((0.0, 0L))(
          seqOp = (c, v) => {
            // c: (e, count), v: (m)
            val e1 = c._1
            val e2 = (v :* v).sum
            val esum = e1 + e2
            (esum, c._2 + 1)
          },
          combOp = (c1, c2) => {
            // c: (e, count)
            val e1 = c1._1
            val e2 = c2._1
            val esum = e1 + e2
            (esum, c1._2 + c2._2)
          })
        val Loss = (loss2 / counte.toDouble) * 0.5 // 平均每个batch误差的均值
        if (n == 0) {
          rL(n) = Loss
        } else {
          rL(n) = 0.09 * rL(n - 1) + 0.01 * Loss
        }
        n = n + 1
      }
      initEndTime = System.currentTimeMillis()
      // 打印输出结果
      printf("epoch: numepochs = %d , Took = %d seconds; batch train mse = %f.\n", i, scala.math.ceil((initEndTime - initStartTime).toDouble / 1000).toLong, rL(n - 1))
    }
    // 计算训练误差及交叉检验误差
    // Full-batch train mse
    var loss_train_e = 0.0
    var loss_val_e = 0.0
    loss_train_e = CNN.CNNeval(train_t, sc.broadcast(cnn_layers), sc.broadcast(cnn_ffb), sc.broadcast(cnn_ffW))
    if (validation > 0) loss_val_e = CNN.CNNeval(train_v, sc.broadcast(cnn_layers), sc.broadcast(cnn_ffb), sc.broadcast(cnn_ffW))
    printf("epoch: Full-batch train mse = %f, val mse = %f.\n", loss_train_e, loss_val_e)
    new CNNModel(cnn_layers, cnn_ffW, cnn_ffb)
  }

}

/**
  * NN(neural network)
  */
object CNN extends Serializable {

  // Initialization mode names

  /**
    * sigm激活函数
    * X = 1./(1+exp(-P));
    */
  def sigm(matrix: BDM[Double]): BDM[Double] = {
    val s1 = 1.0 / (Bexp(matrix * (-1.0)) + 1.0)
    s1
  }

  /**
    * relu激活函数
    */
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

  /**
    * softmax激活函数
    */
  def softmax(matrix: BDM[Double]): BDM[Double] = {
    val result = new BDM[Double](matrix.rows, matrix.cols)
    val ematrix = Bexp(matrix)
    for (i <- 0 until result.cols) {
      val sum = ematrix(::, i).sum
      for (j <- 0 until result.rows) {
        result(j, i) = ematrix(j, i) / sum
      }
    }
    result
  }

  /**
    * tanh激活函数
    * f=1.7159*tanh(2/3.*A);
    */
  def tanh_opt(matrix: BDM[Double]): BDM[Double] = {
    val s1 = Btanh(matrix * (2.0 / 3.0)) * 1.7159
    s1
  }

  /**
    * 克罗内克积
    *
    */
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

  /**
    * convn卷积计算
    */
  def convn(m0: BDM[Double], k0: BDM[Double], shape: String): BDM[Double] = {
    //val m0 = BDM((1.0, 1.0, 1.0, 1.0), (0.0, 0.0, 1.0, 1.0), (0.0, 1.0, 1.0, 0.0), (0.0, 1.0, 1.0, 0.0))
    //val k0 = BDM((1.0, 1.0), (0.0, 1.0))
    //val m0 = BDM((1.0, 5.0, 9.0), (3.0, 6.0, 12.0), (7.0, 2.0, 11.0))
    //val k0 = BDM((1.0, 2.0, 0.0), (0.0, 5.0, 6.0), (7.0, 0.0, 9.0))
    val out1 = shape match {
      case "valid" => // // "valid"模式，当filter全部在image里面的时候，进行卷积运算
        val m1 = m0 // 输入矩阵
      val k1 = Rot90(Rot90(k0)) // 对于m × n阶矩阵： aij => b(m - i + 1)(n - j + 1)  卷积核
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
      case "full" => // "full"模式，从filter和image刚相交开始做卷积
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

  /**
    * cnnff是进行前向传播
    * 计算神经网络中的每个节点的输出值;
    */
  def CNNff(
             batch_xy1: RDD[(BDM[Double], BDM[Double])],
             bc_cnn_layers: org.apache.spark.broadcast.Broadcast[Array[CNNLayers]],
             bc_cnn_ffb: org.apache.spark.broadcast.Broadcast[BDM[Double]],
             bc_cnn_ffW: org.apache.spark.broadcast.Broadcast[BDM[Double]]): RDD[(BDM[Double], Array[Array[BDM[Double]]], BDM[Double], BDM[Double])] = {
    // 第1层:a(1)=[x]
    val train_data1 = batch_xy1.map { f =>
      val lable = f._1 // 标签
    val features = f._2 // 特征
    val nna1 = Array(features)
      val nna = ArrayBuffer[Array[BDM[Double]]]()
      nna += nna1
      (lable, nna)
    }
    // 第2至n层计算
    val train_data2 = train_data1.map { f =>
      val lable = f._1 // 标签
    val nn_a = f._2 // 第一层的输出
    var inputmaps1 = 1.0
      val n = bc_cnn_layers.value.length
      // for each layer
      for (l <- 1 to n - 1) {
        val type1 = bc_cnn_layers.value(l).types // 网络层类型
        val outputmap1 = bc_cnn_layers.value(l).outputmaps // 特征map数量
        val kernelsize1 = bc_cnn_layers.value(l).kernelsize // 卷积核大小
        val scale1 = bc_cnn_layers.value(l).scale // pooling大小
        val k1 = bc_cnn_layers.value(l).k // 卷积核
        val b1 = bc_cnn_layers.value(l).b // 偏置
        val nna1 = ArrayBuffer[BDM[Double]]()
        if (type1 == "c") {
          for (j <- 0 to outputmap1.toInt - 1) { // output map
            // create temp output map
            var z = BDM.zeros[Double](nn_a(l - 1)(0).rows - kernelsize1.toInt + 1, nn_a(l - 1)(0).cols - kernelsize1.toInt + 1)
            for (i <- 0 to inputmaps1.toInt - 1) { // input map
              // convolve with corresponding kernel and add to temp output map
              // z = z + convn(net.layers{l - 1}.a{i}, net.layers{l}.k{i}{j}, 'valid');
              z = z + convn(nn_a(l - 1)(i), k1(i)(j), "valid")
            }
            // add bias, pass through nonlinearity
            // net.layers{l}.a{j} = relu(z + net.layers{l}.b{j})
            val nna0 = relu(z + b1(j))
            nna1 += nna0
          }
          nn_a += nna1.toArray
          inputmaps1 = outputmap1
        } else if (type1 == "s") {
          for (j <- 0 to inputmaps1.toInt - 1) {
            // z = convn(net.layers{l - 1}.a{j}, ones(net.layers{l}.scale) / (net.layers{l}.scale ^ 2), 'valid'); replace with variable
            // net.layers{l}.a{j} = z(1 : net.layers{l}.scale : end, 1 : net.layers{l}.scale : end, :);
            val z = convn(nn_a(l - 1)(j), BDM.ones[Double](scale1.toInt, scale1.toInt) / (scale1 * scale1), "valid") // 平均池化操作
            val zs1 = z(::, 0 to -1 by scale1.toInt).t + 0.0
            val zs2 = zs1(::, 0 to -1 by scale1.toInt).t + 0.0
            val nna0 = zs2
            nna1 += nna0
          }
          nn_a += nna1.toArray
        }
      }
      // concatenate all end layer feature maps into vector
      val nn_fv1 = ArrayBuffer[Double]()
      for (j <- 0 to nn_a(n - 1).length - 1) {
        nn_fv1 ++= nn_a(n - 1)(j).data // 将输出特征矩阵转化成向量放至arraybuffer中
      }
      val nn_fv = new BDM[Double](nn_fv1.length, 1, nn_fv1.toArray) // n×m矩阵 → 1×mn列向量
    // feedforward into output perceptrons
    // net.o = soft(net.ffW * net.fv + repmat(net.ffb, 1, size(net.fv, 2)));
    val nn_o = softmax(bc_cnn_ffW.value * nn_fv + bc_cnn_ffb.value)
      (lable, nn_a.toArray, nn_fv, nn_o) // (标签，每一层输出，输出结果，经权重、偏置与softmax处理后的输出值)
    }
    //val printf1 = train_data2.map(f => f._4.data).take(100)
    //train_data2.map(f => f._2(4)(0)).take(1)
    train_data2
  }

  /**
    * CNNbp是后向传播
    * 计算权重的平均偏导数
    */
  def CNNbp(
             train_cnnff: RDD[(BDM[Double], Array[Array[BDM[Double]]], BDM[Double], BDM[Double])], // f_2: Array(第一层卷积核/池化层矩阵(矩阵1，矩阵2，...,矩阵k1),第二层卷积核/池化层矩阵(...)，...，第n层卷积核/池化层矩阵(...))    其中n为网络层数，ki为第i层卷积核/池化层的深度
             bc_cnn_layers: org.apache.spark.broadcast.Broadcast[Array[CNNLayers]],
             bc_cnn_ffb: org.apache.spark.broadcast.Broadcast[BDM[Double]],
             bc_cnn_ffW: org.apache.spark.broadcast.Broadcast[BDM[Double]]): (RDD[(BDM[Double], Array[Array[BDM[Double]]], BDM[Double], BDM[Double], BDM[Double], BDM[Double], BDM[Double], Array[Array[BDM[Double]]])], BDM[Double], BDM[Double], Array[CNNLayers]) = {
    // error : net.e = net.o - y
    val n = bc_cnn_layers.value.length // 网络层数
    val train_data3 = train_cnnff.map { f =>
      val nn_e = f._4 - f._1.t // 实际输出与真实标签之差
      (f._1, f._2, f._3, f._4, nn_e) // (标签，每一层输出，输出结果，经权重、偏置与sigmoid处理后的输出值，实际输出与真实标签之差)
    }
    // backprop deltas
    // 输出层的 灵敏度 或者 残差
    // net.od = net.e .* (net.o .* (1 - net.o))
    // net.fvd = (net.ffW' * net.od)
    val train_data4 = train_data3.map { f =>
      val nn_e = f._5 // 实际输出与真实标签偏差
    val nn_o = f._4 // 实际输出
    val nn_fv = f._3 // 经权重、偏置与softmax处理之前的输出
    val nn_od = f._5 // d(n)=-(y-a(n))*f'(z)，sigmoid函数f'(z)表达式:f'(z)=f(z)*[1-f(z)]    softmax激活函数和交叉熵损失函数的残差为 ai - yi
    val nn_fvd = if (bc_cnn_layers.value(n - 1).types == "c") {
      // net.fvd = net.fvd .* (net.fv .* (1 - net.fv));
      val nn_fvd1 = bc_cnn_ffW.value.t * nn_od
      val tmp_bdm = new BDM[Double](nn_fv.rows, nn_fv.cols)
      for (i <- 0 until tmp_bdm.rows) {
        for (j <- 0 until tmp_bdm.cols) {
          if (nn_fv(i, j) > 0)
            tmp_bdm(i, j) = 1
          else
            tmp_bdm(i, j) = 0
        }
      }
      val tmp1 = tmp_bdm // relu偏导
      val nn_fvd2 = nn_fvd1 :* tmp1 // 进行全连接的前一层的残差   ∑(w*nn_od*f(z)*[1-f(z)]     改为了relu
      nn_fvd2
    } else {
      val nn_fvd1 = bc_cnn_ffW.value.t * nn_od
      nn_fvd1
    }
      (f._1, f._2, f._3, f._4, f._5, nn_od, nn_fvd) // (标签，每一层输出，输出结果，经权重、偏置与sigmoid处理后的输出值，实际输出与真实标签之差，输出层残差，输出层上一层残差)
    }

    // reshape feature vector deltas into output map style
    // e.g.  输出矩阵 256×1×1
    val sa1 = train_data4.map(f => f._2(n - 1)(1)).take(1)(0).rows // 输出矩阵卷积核/池化层矩阵规模
    val sa2 = train_data4.map(f => f._2(n - 1)(1)).take(1)(0).cols
    val sa3 = 1
    val fvnum = sa1 * sa2 // 1×1
    var trainedSampleNums = 0 // 要用累加器实现，直接定义变量，会在每一个task生成一个副本，
                              // 这里定义的实际上是driver端的变量，副本在worker节点上运行完后并不会返回到driver端打印，所以看到的是每一个task的副本的值
   val train_data5 = train_data4.map { f =>
      trainedSampleNums += 1
      val nn_a = f._2
      val nn_fvd = f._7
      val nn_od = f._6
      val nn_fv = f._3
      var nnd = new Array[Array[BDM[Double]]](n) // 每一层残差
    val nnd1 = ArrayBuffer[BDM[Double]]() // 输出层转化回来的卷积核/池化层残差
      for (j <- 0 to nn_a(n - 1).length - 1) { // j == 0 to 255
        val tmp1 = nn_fvd((j * fvnum) to ((j + 1) * fvnum - 1), 0) // nn_fvd(0 to 0, 0)  nn_fvd(1 to 1, 0)   找到原本残差向量对应的那一部分卷积核/池化矩阵残差
      val tmp2 = new BDM(sa1, sa2, tmp1.toArray)
        nnd1 += tmp2
      }
      nnd(n - 1) = nnd1.toArray
      for (l <- (n - 2) to 0 by -1) { // 从 n - 2层反向传播
        val type1 = bc_cnn_layers.value(l).types
        var nnd2 = ArrayBuffer[BDM[Double]]()
        // ********************？？？是不是池化和卷积的反向传播写反了？？？*****************************或者是这种写法只能是卷积层、池化层间隔的方式
        if (type1 == "c") {
          for (j <- 0 to nn_a(l).length - 1) { // 第l层第j个卷积核
            val tmp_a = nn_a(l)(j) // 第l层第j个卷积核输出
            val tmp_d = nnd(l + 1)(j) // 第l+1层第j个卷积核残差
            val tmp_scale = bc_cnn_layers.value(l + 1).scale.toInt // 池化大小
            val tmp_bdm = new BDM[Double](tmp_a.rows, tmp_a.cols)
            for (i <- 0 until tmp_bdm.rows) {
              for (j <- 0 until tmp_bdm.cols) {
                if (tmp_a(i, j) > 0)
                  tmp_bdm(i, j) = 1
                else
                  tmp_bdm(i, j) = 0
              }
            }
            val tmp1 = tmp_bdm // relu偏导
            val tmp2 = expand(tmp_d, Array(tmp_scale, tmp_scale)) / (tmp_scale.toDouble * tmp_scale) // 平均池化反向传播
            printf("第%d个样本第%d层由下一层池化层第%d个矩阵求残差完毕\n", trainedSampleNums, l + 1, j)
            nnd2 += (tmp1 :* tmp2)
          }
        } else if (type1 == "s") {
          for (i <- 0 to nn_a(l).length - 1) { // 第l层第i个池化结果
            var z = BDM.zeros[Double](nn_a(l)(0).rows, nn_a(l)(0).cols)
            for (j <- 0 to nn_a(l + 1).length - 1) { // 第l+1层第j个卷积结果
              // z = z + convn(net.layers{l + 1}.d{j}, rot180(net.layers{l + 1}.k{i}{j}), 'full');
              z = z + convn(nnd(l + 1)(j), Rot90(Rot90(bc_cnn_layers.value(l + 1).k(i)(j))), "full")
              printf("第%d个样本第%d层第%d个矩阵由下一层卷积层第%d个卷积核求残差完毕\n", trainedSampleNums, l + 1, i, j)
            }
            nnd2 += z
          }
        }
        nnd(l) = nnd2.toArray
      }
      (f._1, f._2, f._3, f._4, f._5, f._6, f._7, nnd) // (标签，每一层输出，输出结果，经权重、偏置与sigmoid处理后的输出值，实际输出与真实标签之差，输出层残差，输出层上一层残差，每一层残差)
    }
    // train_data5.map(f => f._8(4)(0)).take(1)

    // dk db calc gradients
    var layers = bc_cnn_layers.value
    for (l <- 1 to n - 1) { // 第二层到最后一层
      val type1 = bc_cnn_layers.value(l).types
      val lena1 = train_data5.map(f => f._2(l).length).take(1)(0) // 本层卷积核/池化层深度
      val lena2 = train_data5.map(f => f._2(l - 1).length).take(1)(0) // 上层卷积核/池化层深度
      if (type1 == "c") {
        // nndk:
        // Array(
        // 上层卷积核/池化1(本层卷积核/池化1[偏导矩阵]，本层卷积核/池化2[偏导矩阵]，...，本层卷积核/池化n[偏导矩阵])，
        // ...，
        // 上层卷积核/池化m(本层卷积核/池化1[偏导矩阵]，本层卷积核/池化2[偏导矩阵]，...，本层卷积核/池化n[偏导矩阵])
        // )        其中n为本层卷积核、池化层深度；m为上层卷积核、池化层深度
        var nndk = new Array[Array[BDM[Double]]](lena2)
        for (i <- 0 to lena2 - 1) {
          for (j <- 0 to lena1 - 1) {
            nndk(i) = new Array[BDM[Double]](lena1) // 为何多次初始化？？？？？？？？？？？？？
          }
        }
        //        for (i <- 0 to lena2 - 1) {
        //          nndk(i) = new Array[BDM[Double]](lena1) // 修改后的 nndk(i) 为上层与本层的连接的卷积核的偏导
        //        }
        var nndb = new Array[Double](lena1) // 本层偏置
        for (j <- 0 to lena1 - 1) {
          for (i <- 0 to lena2 - 1) {
            val rdd_dk_ij = train_data5.map { f => // 每一层、每个卷积核的dk
              val nn_a = f._2
              val nn_d = f._8
              val tmp_d = nn_d(l)(j) // l层第j个卷积核的残差
            val tmp_a = nn_a(l - 1)(i) // l-1层第i个卷积核/池化层输出
              convn(Rot90(Rot90(tmp_a)), tmp_d, "valid") // dk = 本层残差 （卷积）上层输出   求dk公式
            }
            val initdk = BDM.zeros[Double](rdd_dk_ij.take(1)(0).rows, rdd_dk_ij.take(1)(0).cols)
            val (dk_ij, count_dk) = rdd_dk_ij.treeAggregate((initdk, 0L))(
              seqOp = (c, v) => {
                // c: (m, count), v: (m)    m:dk  count:样本数量
                val m1 = c._1
                val m2 = m1 + v
                (m2, c._2 + 1)
              },
              combOp = (c1, c2) => {
                // c: (m, count)
                val m1 = c1._1
                val m2 = c2._1
                val m3 = m1 + m2
                (m3, c1._2 + c2._2)
              })
            val dk = dk_ij / count_dk.toDouble // 求该batch所有样本dk的平均值
            nndk(i)(j) = dk
          }
          val rdd_db_j = train_data5.map { f =>
            val nn_d = f._8
            val tmp_d = nn_d(l)(j) // 第l层第j个卷积核残差
            Bsum(tmp_d)
          }
          val db_j = rdd_db_j.reduce(_ + _)
          val count_db = rdd_db_j.count
          val db = db_j / count_db.toDouble // 求该batch中所有样本db的平均值
          nndb(j) = db
        }
        layers(l) = new CNNLayers(layers(l).types, layers(l).outputmaps, layers(l).kernelsize, layers(l).scale, layers(l).k, layers(l).b, nndk, nndb)
      }
    }

    // net.dffW = net.od * (net.fv)' / size(net.od, 2);
    // net.dffb = mean(net.od, 2);
    val train_data6 = train_data5.map { f =>
      val nn_od = f._6 // 输出层残差
    val nn_fv = f._3 // 输出结果
      nn_od * nn_fv.t
    }
    val train_data7 = train_data5.map { f =>
      val nn_od = f._6
      nn_od
    }
    val initffW = BDM.zeros[Double](bc_cnn_ffW.value.rows, bc_cnn_ffW.value.cols)
    val (ffw2, countfffw2) = train_data6.treeAggregate((initffW, 0L))( // 输出层w偏导:本层输出 * 下一层残差
      seqOp = (c, v) => {
        // c: (m, count), v: (m)
        val m1 = c._1
        val m2 = m1 + v
        (m2, c._2 + 1)
      },
      combOp = (c1, c2) => {
        // c: (m, count)
        val m1 = c1._1
        val m2 = c2._1
        val m3 = m1 + m2
        (m3, c1._2 + c2._2)
      })
    val cnn_dffw = ffw2 / countfffw2.toDouble // 该batch所有样本的w偏导平均值
    val initffb = BDM.zeros[Double](bc_cnn_ffb.value.rows, bc_cnn_ffb.value.cols)
    val (ffb2, countfffb2) = train_data7.treeAggregate((initffb, 0L))( // 输出层b偏导:下一层残差
      seqOp = (c, v) => {
        // c: (m, count), v: (m)
        val m1 = c._1
        val m2 = m1 + v
        (m2, c._2 + 1)
      },
      combOp = (c1, c2) => {
        // c: (m, count)
        val m1 = c1._1
        val m2 = c2._1
        val m3 = m1 + m2
        (m3, c1._2 + c2._2)
      })
    val cnn_dffb = ffb2 / countfffb2.toDouble // 该batch所有样本的b偏导平均值
    (train_data5, cnn_dffw, cnn_dffb, layers) // (训练数据直到求出输出层上一层残差为止的结果，输出层权重偏导，输出层偏置偏导，每一层网络的配置)
  }

  /**
    * NNapplygrads是权重更新
    * 权重更新
    */
  def CNNapplygrads(
                     train_cnnbp: (RDD[(BDM[Double], Array[Array[BDM[Double]]], BDM[Double], BDM[Double], BDM[Double], BDM[Double], BDM[Double], Array[Array[BDM[Double]]])], BDM[Double], BDM[Double], Array[CNNLayers]),
                     bc_cnn_ffb: org.apache.spark.broadcast.Broadcast[BDM[Double]],
                     bc_cnn_ffW: org.apache.spark.broadcast.Broadcast[BDM[Double]],
                     alpha: Double): (BDM[Double], BDM[Double], Array[CNNLayers]) = {
    val train_data5 = train_cnnbp._1
    val cnn_dffw = train_cnnbp._2
    val cnn_dffb = train_cnnbp._3
    var cnn_layers2 = train_cnnbp._4 // 网络层配置信息
    var cnn_ffb2 = bc_cnn_ffb.value
    var cnn_ffW2 = bc_cnn_ffW.value
    val n = cnn_layers2.length

    for (l <- 1 to n - 1) { // 网络层数
      val type1 = cnn_layers2(l).types
      val lena1 = train_data5.map(f => f._2(l).length).take(1)(0) // 第l层深度
      val lena2 = train_data5.map(f => f._2(l - 1).length).take(1)(0) // 第l-1层深度
      if (type1 == "c") {
        for (j <- 0 to lena1 - 1) {
          for (ii <- 0 to lena2 - 1) {
            cnn_layers2(l).k(ii)(j) = cnn_layers2(l).k(ii)(j) - cnn_layers2(l).dk(ii)(j) * alpha // 权重更新公式
          }
          cnn_layers2(l).b(j) = cnn_layers2(l).b(j) - cnn_layers2(l).db(j) * alpha // 偏置更新公式
        }
      }
    }
    cnn_ffW2 = cnn_ffW2 - cnn_dffw * alpha
    cnn_ffb2 = cnn_ffb2 - cnn_dffb * alpha
    (cnn_ffW2, cnn_ffb2, cnn_layers2)
  }

  /**
    * nneval是进行前向传播并计算输出误差
    * 计算神经网络中的每个节点的输出值，并计算平均误差;
    */
  def CNNeval(
               batch_xy1: RDD[(BDM[Double], BDM[Double])],
               bc_cnn_layers: org.apache.spark.broadcast.Broadcast[Array[CNNLayers]],
               bc_cnn_ffb: org.apache.spark.broadcast.Broadcast[BDM[Double]],
               bc_cnn_ffW: org.apache.spark.broadcast.Broadcast[BDM[Double]]): Double = {
    // CNNff是进行前向传播
    val train_cnnff = CNN.CNNff(batch_xy1, bc_cnn_layers, bc_cnn_ffb, bc_cnn_ffW) // (标签，每一层输出，输出结果，经权重、偏置与sigmoid处理后的输出值)
    // error and loss
    // 输出误差计算
    val rdd_loss1 = train_cnnff.map { f =>
      val nn_e = f._4 - f._1.t
      nn_e
    }
    val (loss2, counte) = rdd_loss1.treeAggregate((0.0, 0L))(
      seqOp = (c, v) => {
        // c: (e, count), v: (m)
        val e1 = c._1
        val e2 = (v :* v).sum
        val esum = e1 + e2
        (esum, c._2 + 1)
      },
      combOp = (c1, c2) => {
        // c: (e, count)
        val e1 = c1._1
        val e2 = c2._1
        val esum = e1 + e2
        (esum, c1._2 + c2._2)
      })
    val Loss = (loss2 / counte.toDouble) * 0.5 // 一个batch平均每个样本的误差
    Loss
  }
}