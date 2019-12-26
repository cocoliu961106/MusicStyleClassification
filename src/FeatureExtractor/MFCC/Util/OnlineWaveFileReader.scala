package FeatureExtractor.MFCC.Util

import java.io.{BufferedInputStream, FileInputStream, IOException}
import java.net.URI

import ClassificationModule.Util.HDFSUtil.{hdfsUrl, realUrl}
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataInputStream, FileSystem, Path}

// 从HDFS读取文件，并获取和WaveFileReader一样格式的数据
class OnlineWaveFileReader(var filename: String) extends Serializable {
  val hdfsUrl = "hdfs://spark1:9000"
  private var data:Array[Array[Int]] = _
  private var len = 0
  private var chunkdescriptor: String = _
  private val lenchunkdescriptor = 4
  private var chunksize = 0
  private val lenchunksize = 4
  private var waveflag: String = _
  private val lenwaveflag = 4
  private var fmtubchunk: String = _
  private val lenfmtubchunk = 4
  private var subchunk1size = 0
  private val lensubchunk1size = 4
  private var audioformat = 0
  private val lenaudioformat = 2
  private var numchannels = 0
  private val lennumchannels = 2
  private var samplerate = 0
  private val lensamplerate = 2
  private var byterate = 0
  private val lenbyterate = 4
  private var blockalign = 0
  private val lenblockling = 2
  private var bitspersample = 0
  private val lenbitspersample = 2
  private var datasubchunk: String = _
  private val lendatasubchunk = 4
  private var subchunk2size = 0
  private val lensubchunk2size = 4
  private var fds: FSDataInputStream = _
  private var bis: BufferedInputStream = _
  private var hdfs: FileSystem = _
  private var issuccess = false
  this.initReader(filename)


  // 判断是否创建wav读取器成功
  def isSuccess: Boolean = issuccess

  // 获取每个采样的编码长度，8bit或者16bit
  def getBitPerSample: Int = this.bitspersample

  // 获取采样率
  def getSampleRate: Long = this.samplerate

  // 获取声道个数，1代表单声道 2代表立体声
  def getNumChannels: Int = this.numchannels

  // 获取数据长度，也就是一共采样多少个
  def getDataLen: Int = this.len

  // 获取数据
  // 数据是一个二维数组，[n][m]代表第n个声道的第m个采样值
  def getData: Array[Array[Int]] = this.data

  def getChunkdescriptor: String = this.chunkdescriptor

  def initReader(filename: String): Unit = {
    var result = new Array[Byte](0)
    val buff = new Array[Byte](4)
    if (StringUtils.isNoneBlank(filename)) {
      realUrl = hdfsUrl + filename
      val config = new Configuration()
      try {
      hdfs = FileSystem.get(URI.create(realUrl), config)
      val path = new Path(realUrl)
      if (hdfs.exists(path)) {
          fds = hdfs.open(path)
          bis = new BufferedInputStream(fds)
          this.chunkdescriptor = readString(lenchunkdescriptor)
          if (!chunkdescriptor.endsWith("RIFF")) throw new IllegalArgumentException("RIFF miss, " + filename + " is not a wave file.")
          this.chunksize = readLong
          this.waveflag = readString(lenwaveflag)
          if (!waveflag.endsWith("WAVE")) throw new IllegalArgumentException("WAVE miss, " + filename + " is not a wave file.")
          this.fmtubchunk = readString(lenfmtubchunk)
          if (!fmtubchunk.endsWith("fmt ")) throw new IllegalArgumentException("fmt miss, " + filename + " is not a wave file.")
          this.subchunk1size = readLong
          this.audioformat = readInt
          this.numchannels = readInt
          this.samplerate = readLong
          this.byterate = readLong
          this.blockalign = readInt
          this.bitspersample = readInt
          this.datasubchunk = readString(lendatasubchunk)
          if (!datasubchunk.endsWith("data")) throw new IllegalArgumentException("data miss, " + filename + " is not a wave file.")
          this.subchunk2size = readLong
          this.len = this.subchunk2size / (this.bitspersample / 8) / this.numchannels
          this.data = Array.ofDim[Int](this.numchannels, this.len)

          var i = 0
          while ( {
            i < this.len
          }) {
            var n = 0
            while ( {
              n < this.numchannels
            }) {
              if (this.bitspersample == 8) this.data(n)(i) = bis.read
              else if (this.bitspersample == 16) this.data(n)(i) = this.readInt

              {
                n += 1;
                n
              }
            }

            {
              i += 1;
              i
            }
          }

          this.issuccess = true
        }
      } catch {
        case e1: Exception =>
          e1.printStackTrace()
      }
      finally {
        try {
          if (bis != null) {
            bis.close()
          }
          if (fds != null) {
            fds.close()
          }
          if (hdfs != null) {
            hdfs.close()
          }
        } catch {
          case e1: IOException =>
            e1.printStackTrace()
        }
      }
    }
  }

  private def readString(len: Int) = {
    val buf = new Array[Byte](len)
    try
        if (bis.read(buf) != len) throw new IOException("no more data!!!")
    catch {
      case e: IOException =>
        e.printStackTrace()
    }
    new String(buf)
  }

  private def readInt = {
    val buf = new Array[Byte](2)
    var res = 0
    try {
      if (bis.read(buf) != 2) throw new IOException("no more data!!!")
      res = (buf(0) & 0x000000FF) | (buf(1).toInt << 8)
    } catch {
      case e: IOException =>
        e.printStackTrace()
    }
    res
  }

  private def readLong = {
    var res = 0
    try {
      val l = new Array[Long](4)
      var i = 0
      while ( {
        i < 4
      }) {
        l(i) = bis.read
        if (l(i) == -1) throw new IOException("no more data!!!")

        {
          i += 1; i
        }
      }
      res = (l(0) | (l(1) << 8) | (l(2) << 16) | (l(3) << 24)).toInt
    } catch {
      case e: IOException =>
        e.printStackTrace()
    }
    res
  }
}
