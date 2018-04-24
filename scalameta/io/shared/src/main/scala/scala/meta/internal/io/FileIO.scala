package scala.meta.internal.io

import java.net.URI
import java.nio.charset.Charset
import scalapb.GeneratedMessage
import scala.meta.internal.semanticdb3._
import scala.meta.io._

object FileIO {

  def readAllBytes(path: AbsolutePath): Array[Byte] =
    PlatformFileIO.readAllBytes(path)

  def readAllBytes(uri: URI): Array[Byte] =
    PlatformFileIO.readAllBytes(uri)

  def readAllDocuments(path: AbsolutePath): Seq[TextDocument] =
    PlatformFileIO.readAllDocuments(path)

  def readIndex(path: AbsolutePath): Index =
    PlatformFileIO.readIndex(path)

  def write(path: AbsolutePath, proto: GeneratedMessage): Unit =
    PlatformFileIO.write(path, proto)

  def slurp(path: AbsolutePath, charset: Charset): String =
    PlatformFileIO.slurp(path, charset)

  def listFiles(path: AbsolutePath): ListFiles =
    PlatformFileIO.listFiles(path)

  def isFile(path: AbsolutePath): Boolean =
    PlatformFileIO.isFile(path)

  def isDirectory(path: AbsolutePath): Boolean =
    PlatformFileIO.isDirectory(path)

  def listAllFilesRecursively(path: AbsolutePath): ListFiles =
    PlatformFileIO.listAllFilesRecursively(path)

  def jarRootPath(jarFile: AbsolutePath): AbsolutePath =
    PlatformFileIO.jarRootPath(jarFile)
}

