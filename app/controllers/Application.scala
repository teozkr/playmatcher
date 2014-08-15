package controllers

import actors.WSClientActor
import models._
import play.api.Logger
import play.api.db.slick.DB
import utils._
import play.api.Play.current
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.libs.openid.SteamOpenID
import play.api.libs.concurrent.Execution.Implicits._
import org.virtuslab.unicorn.LongUnicornPlay.driver.simple._

import scala.concurrent.Future


object Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def connect = Action { implicit request =>
    DB.withSession { implicit session =>
      Users.current.firstOption match {
        case None => Redirect(routes.Application.index().withQueryString(request.queryString))
        case Some(_) => Ok(views.html.matcher())
      }
    }
  }

  def connectWS = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    val userQ = Users.current
    val gamesQ = for {
      userGame <- UserGames
      if userGame.userID in userQ.map(_.id)
      game <- userGame.game
    } yield game
    Future.successful(DB.withSession { implicit session =>
      userQ.firstOption match {
        case None =>
          Left(Redirect(routes.Application.index()))
        case Some(user) =>
          val games = gamesQ.list
          val clientInfo = ClientInfo(user.name.getOrElse("Unknown"), games)
          Right(out => WSClientActor.props(out, clientInfo))
      }
    })
  }

}

object SteamAuthentication extends Controller {
  private def openIDRealm(implicit req: RequestHeader): Option[String] = Some(routes.Application.index().absoluteURL())

  private val steamOpenID = "http://steamcommunity.com/openid"

  def steamLogin(returnTo: Option[String]) = Action.async { implicit request =>
    val realReturnTo = returnTo
      .orElse(request.headers.get("Referer"))
      .getOrElse(routes.Application.index().url)
    SteamOpenID().redirectURL(steamOpenID, routes.SteamAuthentication.steamCallback(realReturnTo).absoluteURL(), realm = openIDRealm)
      .map(TemporaryRedirect)
      .recover { case ex: Throwable =>
        Logger.error("OpenID redirect retrieval failed", ex)
        Redirect(realReturnTo)
          .flashing("error" -> "OpenID authentication failed")
    }
  }

  def steamCallback(returnTo: String) = Action.async { implicit request =>
    SteamOpenID().verifiedId.map(Some.apply).recover { case ex: Throwable =>
        Logger.error(s"OpenID verification failed: ${request.uri}", ex)
        None
    }.flatMap {
      case Some(openID) =>
        val (steamMemberID, uid) = DB.withTransaction { implicit session =>
          val steamMemberID = Users.Steam.openIDtoSteamid(openID.id)
          val uid = Users.getOrRegisterIDBySteamid(steamMemberID)
          (steamMemberID, uid)
        }

        val steamSummaryUpdatedF = Users.updateSteamInfo(steamMemberID)(DB.withTransaction)
        val newGamesF = UserGames.populateFromSteam(steamMemberID, uid)(DB.withTransaction)

        for {
          _ <- steamSummaryUpdatedF
          newGames <- newGamesF
        } yield {
          val msg = "You have been logged in" + (newGames match {
            case 0 => ""
            case x => s", and $x new games have been imported"
          })

          Redirect(returnTo)
            .withUser(uid)
            .flashing("success" -> msg)
        }
      case None =>
        Future(
          Redirect(returnTo)
            .flashing("error" -> "OpenID verification failed")
        )
    }
  }
}
