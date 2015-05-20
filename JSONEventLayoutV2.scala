import java.nio.charset.Charset

import net.minidev.json.JSONObject
import org.apache.logging.log4j.core.config.Node
import org.apache.logging.log4j.core.config.plugins._
import org.apache.logging.log4j.core.layout.AbstractStringLayout
import org.apache.logging.log4j.core.{Layout, LogEvent}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.util.Try

@Plugin(name = "JSONEventLayoutV2", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
class JSONEventLayoutV2(locationInfo: Boolean = true, userFieldsStr: String = "") extends AbstractStringLayout(Charset.forName("UTF-8")) {
  val version = 2

  override def getContentType = "application/json; charset=UTF-8"

  lazy val hostname: String = Try(java.net.InetAddress.getLocalHost.getHostName).getOrElse("unknown-host")
  val userFields = userFieldsStr.split(",").map(_.span(_ != ':')).toMap.filterKeys(_.nonEmpty).mapValues(_.substring(1))

  override def toSerializable(loggingEvent: LogEvent): String = {
    val threadName = loggingEvent.getThreadName
    val timestamp = loggingEvent.getTimeMillis
    val mdc = loggingEvent.getContextMap
    val ndc = loggingEvent.getContextStack

    val logstashEvent = new JSONObject

    logstashEvent.put("@version", Int.box(version))
    logstashEvent.put("@timestamp", tsToDate(timestamp))

    userFields.foreach { case (k, v) =>
      logstashEvent.put(k, v)
    }

    logstashEvent.put("source_host", hostname)
    logstashEvent.put("message", loggingEvent.getMessage.getFormattedMessage)

    Option(loggingEvent.getThrown) match {
      case Some(ex) =>
        val exceptionInfo = new JSONObject()
        Option(ex.getClass.getCanonicalName) match {
          case Some(name) => exceptionInfo.put("exception_class", name)
          case None =>
        }
        Option(ex.getMessage) match {
          case Some(msg) => exceptionInfo.put("exception_message", msg)
          case None =>
        }

        exceptionInfo.put("stacktrace", ex.getStackTraceString)
        logstashEvent.put("exception", exceptionInfo)
      case None =>
    }

    if (locationInfo) {
      Option(loggingEvent.getSource) match {
        case Some(info) =>
          addIfNotNull(logstashEvent, "file", info.getFileName)
          addIfNotNull(logstashEvent, "line_number", Int.box(info.getLineNumber))
          addIfNotNull(logstashEvent, "class", info.getClassName)
          addIfNotNull(logstashEvent, "method", info.getMethodName)
        case None =>
      }
    }

    addIfNotNull(logstashEvent, "logger_name", loggingEvent.getLoggerName)
    addIfNotNull(logstashEvent, "level", loggingEvent.getLevel.toString)
    addIfNotNull(logstashEvent, "thread_name", threadName)

    if (!mdc.isEmpty)
      logstashEvent.put("mdc", mdc)
    if (!ndc.isEmpty)
      logstashEvent.put("ndc", ndc)

    logstashEvent.toString + "\n"
  }

  def addIfNotNull(js: JSONObject, key: String, value: AnyRef) = {
    if (value != null)
      js.put(key, value)
  }

  def tsToDate(timestamp: Long): String = {
    new DateTime(timestamp).toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC())
  }
}

object JSONEventLayoutV2 {
  @PluginFactory
  def createLayout(
    @PluginAttribute(value = "LocationInfo", defaultBoolean = true) locationInfo: Boolean,
    @PluginAttribute(value = "UserFields", defaultString = "") userFieldsStr: String
    ) {
    new JSONEventLayoutV2(locationInfo, userFieldsStr)
  }

  def createDefaultLayout = new JSONEventLayoutV2()

  @PluginBuilderFactory
  def newBuilder = new Builder()

  class Builder extends org.apache.logging.log4j.core.util.Builder[JSONEventLayoutV2] {

    @PluginBuilderAttribute private val LocationInfo: Boolean = true
    @PluginBuilderAttribute private val UserFields: String = ""

    override def build(): JSONEventLayoutV2 = new JSONEventLayoutV2(LocationInfo, UserFields)
  }

}
