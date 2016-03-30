package backend

import java.net.URLDecoder
import scala.concurrent.duration._
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.contrib.pattern.ShardRegion
import akka.persistence.PersistentActor
import play.extras.geojson.LatLng

object UserMetaData {
  sealed trait Command {
    def id: String
  }
  case class GetUser(id: String) extends Command
  case class User(id: String, distance: Double) extends Command
  case class UpdateUserPosition(id: String, position: LatLng) extends Command

  val idExtractor: ShardRegion.IdExtractor = {
    case cmd: Command => (cmd.id, cmd)
  }

  val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case cmd: Command => (math.abs(cmd.id.hashCode) % 100).toString
  }

  val shardName: String = "UserMetaData"

  val props = Props[UserMetaData]

  sealed trait Event
  case class FirstObservation(position: LatLng) extends Event
  case class Moved(to: LatLng, distance: Double) extends Event

  private case class State(position: Option[LatLng], distance: Double) {
    def updated(evt: Event): State = evt match {
      case Moved(to, d)          => copy(position = Some(to), distance = distance + d)
      case FirstObservation(pos) => copy(position = Some(pos))
    }
  }
}

class UserMetaData extends PersistentActor {

  import UserMetaData._

  val settings = Settings(context.system)
  val userId = URLDecoder.decode(self.path.name, "utf-8")
  private var state = State(None, 0.0)

  // passivate the entity when no activity
  context.setReceiveTimeout(30.seconds)

  override def persistenceId = self.path.toStringWithoutAddress

  override def receiveRecover: Receive = {
    case evt: Event => state = state.updated(evt)
  }

  override def receiveCommand: Receive = {
    case _: GetUser =>
      sender() ! User(userId, state.distance)

    case UpdateUserPosition(_, position) =>
      state match {
        case State(Some(lastPosition), _) =>
          val d = settings.GeoFunctions.distanceBetweenPoints(lastPosition, position)
          persist(Moved(position, d)) { evt =>
            state = state.updated(evt)
          }
        case State(None, _) =>
          persist(FirstObservation(position)) { evt =>
            state = state.updated(evt)
          }
      }
      
    case ReceiveTimeout => 
      context.parent ! ShardRegion.Passivate(stopMessage = PoisonPill)
  }
}
