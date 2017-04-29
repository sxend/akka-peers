package registerd

import java.nio.file.{ Files, Paths }

trait FileSystem {
  def readBinary(file: String): Array[Byte] = {
    mkdirp(dirname(file))
    Files.readAllBytes(Paths.get(file))
  }

  def writeBinary(file: String, data: Array[Byte]): Unit = {
    mkdirp(dirname(file))
    Files.write(Paths.get(file), data)
  }

  def readString(file: String): String =
    new String(readBinary(file), "UTF-8")

  def writeString(file: String, data: String): Unit =
    writeBinary(file, data.getBytes("UTF-8"))

  private def dirname(file: String): String = file.take(file.lastIndexOf("/"))

  private def mkdirp(path: String) {
    var prepath = ""
    for (dir <- path.split("/")) {
      prepath += (dir + "/")
      val file = new java.io.File(prepath)
      if (!file.exists()) {
        file.mkdir()
      }
    }
  }
}

object FileSystem extends FileSystem