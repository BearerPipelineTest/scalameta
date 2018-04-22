package scala.meta.internal.semanticdb.scalac

import scala.meta.internal.io.PathIO
import scala.meta.io._
import scala.util.matching.Regex

case class SemanticdbConfig(
    sourceroot: AbsolutePath,
    targetroot: AbsolutePath,
    mode: SemanticdbMode,
    failures: FailureMode,
    denotations: DenotationMode,
    signatures: SignatureMode,
    overrides: OverrideMode,
    profiling: ProfilingMode,
    fileFilter: FileFilter,
    messages: MessageMode,
    synthetics: SyntheticMode,
    owners: OwnerMode) {
  def syntax: String = {
    val p = SemanticdbPlugin.name
    List(
      "sourceroot" -> sourceroot,
      "targetroot" -> targetroot,
      "mode" -> mode.name,
      "failures" -> failures.name,
      "signatures" -> signatures.name,
      "denotations" -> denotations.name,
      "overrides" -> overrides.name,
      "profiling" -> profiling.name,
      "include" -> fileFilter.include,
      "exclude" -> fileFilter.exclude,
      "messages" -> messages.name,
      "synthetics" -> synthetics.name,
      "owners" -> owners.name
    ).map { case (k, v) => s"-P:$p:$k:$v" }.mkString(" ")
  }

}
object SemanticdbConfig {
  def default = SemanticdbConfig(
    PathIO.workingDirectory,
    PathIO.workingDirectory,
    SemanticdbMode.Fat,
    FailureMode.Warning,
    DenotationMode.Definitions,
    SignatureMode.New,
    OverrideMode.None,
    ProfilingMode.Off,
    FileFilter.matchEverything,
    MessageMode.All,
    SyntheticMode.All,
    OwnerMode.All
  )

  private val SetSourceroot = "sourceroot:(.*)".r
  private val SetMode = "mode:(.*)".r
  private val SetFailures = "failures:(.*)".r
  private val SetDenotations = "denotations:(.*)".r
  private val SetSignatures = "signatures:(.*)".r
  private val SetMembers = "members:(.*)".r
  private val SetOverrides = "overrides:(.*)".r
  private val SetProfiling = "profiling:(.*)".r
  private val SetInclude = "include:(.*)".r
  private val SetExclude = "exclude:(.*)".r
  private val SetMessages = "messages:(.*)".r
  private val SetSynthetics = "synthetics:(.*)".r
  private val SetOwners = "owners:(.*)".r

  def parse(scalacOptions: List[String], errFn: String => Unit): SemanticdbConfig = {
    var config = default
    val relevantOptions = scalacOptions.filter(_.startsWith("-P:semanticdb:"))
    val strippedOptions = relevantOptions.map(_.stripPrefix("-P:semanticdb:"))
    strippedOptions.foreach {
      case SetSourceroot(path) =>
        config = config.copy(sourceroot = AbsolutePath(path))
      case SetMode(SemanticdbMode(mode)) =>
        config = config.copy(mode = mode)
      case SetFailures(FailureMode(severity)) =>
        config = config.copy(failures = severity)
      case SetDenotations(DenotationMode(denotations)) =>
        config = config.copy(denotations = denotations)
      case option @ SetSignatures(SignatureMode(signatures)) =>
        signatures match {
          case SignatureMode.All | SignatureMode.Old =>
            errFn(s"$option is no longer supported. Use signatures:{new,none} instead.")
          case _ =>
            config = config.copy(signatures = signatures)
        }
      case option @ SetMembers(_) =>
        errFn(s"$option is no longer supported.")
      case option @ SetOverrides(OverrideMode(_)) =>
        errFn(s"$option is no longer supported")
      case SetProfiling(ProfilingMode(profiling)) =>
        config = config.copy(profiling = profiling)
      case SetInclude(include) =>
        config = config.copy(fileFilter = config.fileFilter.copy(include = include.r))
      case SetExclude(exclude) =>
        config = config.copy(fileFilter = config.fileFilter.copy(exclude = exclude.r))
      case SetMessages(MessageMode(messages)) =>
        config = config.copy(messages = messages)
      case SetSynthetics(SyntheticMode(synthetics)) =>
        config = config.copy(synthetics = synthetics)
      case SetOwners(OwnerMode(owners)) =>
        config = config.copy(owners = owners)
      case els =>
        errFn(s"Ignoring unknown option $els")
    }
    config
  }
}

sealed abstract class SemanticdbMode {
  def name: String = toString.toLowerCase
  import SemanticdbMode._
  def isSlim: Boolean = this == Slim
  def isFat: Boolean = this == Fat
  def isDisabled: Boolean = this == Disabled
}
object SemanticdbMode {
  def unapply(arg: String): Option[SemanticdbMode] = all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(Fat, Slim, Disabled)
  case object Fat extends SemanticdbMode
  case object Slim extends SemanticdbMode
  case object Disabled extends SemanticdbMode
}

sealed abstract class FailureMode {
  def name: String = toString.toLowerCase
}
object FailureMode {
  def unapply(arg: String): Option[FailureMode] = all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(Error, Warning, Info, Ignore)
  case object Error extends FailureMode
  case object Warning extends FailureMode
  case object Info extends FailureMode
  case object Ignore extends FailureMode
}

sealed abstract class DenotationMode {
  import DenotationMode._
  def name: String = toString.toLowerCase
  def saveDefinitions: Boolean = this == All || this == Definitions
  def saveReferences: Boolean = this == All
}
object DenotationMode {
  def name: String = toString.toLowerCase
  def unapply(arg: String): Option[DenotationMode] = all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(All, Definitions, None)
  case object All extends DenotationMode
  case object Definitions extends DenotationMode
  case object None extends DenotationMode
}

sealed abstract class SignatureMode {
  def name: String = toString.toLowerCase
  import SignatureMode._
  def isAll: Boolean = this == All
  def isNew: Boolean = this == New
  def isOld: Boolean = this == Old
  def isNone: Boolean = this == None
}
object SignatureMode {
  def unapply(arg: String): Option[SignatureMode] = all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(All, New, Old, None)
  case object New extends SignatureMode
  case object None extends SignatureMode
  // Deprecated
  case object Old extends SignatureMode
  case object All extends SignatureMode
}


sealed abstract class OverrideMode {
  import OverrideMode._
  def name: String = toString.toLowerCase
  def isAll: Boolean = this == All
}
object OverrideMode {
  def unapply(arg: String): Option[OverrideMode] =
    all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(All, None)
  case object All extends OverrideMode
  case object None extends OverrideMode
}

sealed abstract class ProfilingMode {
  def name: String = toString.toLowerCase
  import ProfilingMode._
  def isConsole: Boolean = this == Console
  def isOff: Boolean = this == Off
}
object ProfilingMode {
  def unapply(arg: String): Option[ProfilingMode] = all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(Console, Off)
  case object Console extends ProfilingMode
  case object Off extends ProfilingMode
}

case class FileFilter(include: Regex, exclude: Regex) {
  def matches(path: String): Boolean =
    include.findFirstIn(path).isDefined &&
      exclude.findFirstIn(path).isEmpty
}
object FileFilter {
  def apply(include: String, exclude: String): FileFilter =
    FileFilter(include.r, exclude.r)
  val matchEverything = FileFilter(".*", "$a")
}

sealed abstract class MessageMode {
  def name: String = toString.toLowerCase
  import MessageMode._
  def saveMessages: Boolean = this == All
}
object MessageMode {
  def unapply(arg: String): Option[MessageMode] = all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(All, None)
  case object All extends MessageMode
  case object None extends MessageMode
}

sealed abstract class SyntheticMode {
  def name: String = toString.toLowerCase
  import SyntheticMode._
  def saveSynthetics: Boolean = this == All
}
object SyntheticMode {
  def unapply(arg: String): Option[SyntheticMode] = all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(All, None)
  case object All extends SyntheticMode
  case object None extends SyntheticMode
}

sealed abstract class OwnerMode {
  import OwnerMode._
  def name: String = toString.toLowerCase
  def isAll: Boolean = this == All
}
object OwnerMode {
  def unapply(arg: String): Option[OwnerMode] =
    all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(All, None)
  case object All extends OwnerMode
  case object None extends OwnerMode
}
