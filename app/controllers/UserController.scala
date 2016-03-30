package controllers

import akka.actor.ActorRef
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout

import play.api.mvc._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import javax.inject.{Inject, Named}

import backend.UserMetaData._

class UserController @Inject() (
    @Named("userMetaData") userMetaData: ActorRef)(implicit ec: ExecutionContext) extends Controller {

  implicit val userWrites = Json.writes[User]
  def get(id: String) = Action.async {
    implicit val timeout = Timeout(2.seconds)

    (userMetaData ? GetUser(id))
      .mapTo[User]
      .map { user =>
        Ok(Json.toJson(user))
      } recover {
        case _: AskTimeoutException => NotFound
      }
  }
}
