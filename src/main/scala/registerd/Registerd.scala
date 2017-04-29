package registerd

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.pattern._
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.scaladsl.server.Directives._
import akka.cluster.http.management.ClusterHttpManagement
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.google.protobuf.ByteString
import com.typesafe.config.{ Config, ConfigFactory }
import registerd.entity.Resource

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

object Registerd {

  val config: Config = ConfigFactory.load

  private val roles = config.getStringList("akka.cluster.roles").toList

  def main(args: Array[String]): Unit = {
    val cluster = Cluster(ActorSystem("registerd", config))

    onRole("seed") {
      ClusterHttpManagement(cluster)
    }
    onRole("registerd") {
      startRegisterd(cluster)
    }
  }
  private def onRole(role: String)(fn: => Unit) = if (hasRole(role)) fn

  private def hasRole(role: String): Boolean = roles.contains(role)

  private def startRegisterd(cluster: Cluster): Unit = {
    implicit val system = cluster.system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    implicit val timeout = Timeout(10.seconds)
    val ref = system.actorOf(Props(classOf[Registerd], cluster), config.getString("registerd.hostname"))
    import registerd.entity.JsonProtocol._
    implicit val format = jsonFormat2(Protocol.Value.apply)
    implicit val printer = spray.json.PrettyPrinter
    val route =
      pathPrefix("resources") {
        put {
          Directives.entity(as[Protocol.Value]) { request =>
            ref ! Resource(
              instance = config.getString("registerd.hostname"),
              id = request.id,
              payload = ByteString.copyFrom(request.payload.getBytes)
            )
            complete(StatusCodes.Accepted)
          }
        } ~
          get {
            def resourceComplete: PartialFunction[Try[Option[Resource]], Route] = {
              case Success(Some(resource)) =>
                complete(resource)
              case Success(None) =>
                complete(StatusCodes.NotFound)
              case Failure(t) =>
                system.log.error(t, t.getMessage)
                complete(StatusCodes.NotFound)
            }
            path(Segment) { id =>
              onComplete(ref.ask(id).mapTo[Option[Resource]])(resourceComplete)
            } ~
              path(Segment / Segment) { (instance, id) =>
                onComplete(ref.ask((instance, id)).mapTo[Option[Resource]])(resourceComplete)
              }
          }
      }
    val hostname = system.settings.config.getString("registerd.endpoint.hostname")
    val port = system.settings.config.getInt("registerd.endpoint.port")
    Http().bindAndHandle(route, hostname, port)
  }
  object Protocol {
    case class Value(id: String, payload: String)
  }
}

class Registerd(cluster: Cluster) extends Actor with ActorLogging {
  import java.io.PrintWriter
  import java.nio.file.{ Files, Paths }
  import context.dispatcher
  private implicit val system = context.system
  private val settings = system.settings
  private val config = settings.config
  private val hostname = config.getString("registerd.hostname")
  private val resourcesDir = config.getString("registerd.resources-dir")
  def receive = {
    case id: String =>
      getResource(hostname, id).pipeTo(sender())
    case (instance: String, id: String) =>
      getResource(instance, id).pipeTo(sender())
    case resource: Resource => saveResource(resource)
    case message            => log.info(s"message received: $message")
  }
  private def getResource(instance: String, id: String): Future[Option[Resource]] = {
    Future {
      val checksum = scala.io.Source.fromFile(getFilePath(s"$resourcesDir/$instance/$id", "checksum.txt")).getLines.toList.head
      val resource = Resource.parseFrom(Files.readAllBytes(Paths.get(getFilePath(s"$resourcesDir/$instance/$id", "resource.bin"))))
      if (checksum == resource.digest) {
        Some(resource)
      } else {
        None
      }
    }
  }
  private def saveResource(resource: Resource): Unit = {
    val checksum = resource.digest
    val binary = resource.toByteArray
    val file = new PrintWriter(getFilePath(s"$resourcesDir/${resource.instance}/${resource.id}", "checksum.txt"))
    file.write(checksum)
    file.close()
    Files.write(Paths.get(getFilePath(s"$resourcesDir/${resource.instance}/${resource.id}", "resource.bin")), binary)
  }
  private def getFilePath(dir: String, name: String): String = {
    mkdirp(dir)
    s"$dir/$name"
  }
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
  override def unhandled(message: Any): Unit = log.warning(s"unhandled message: $message")
}
