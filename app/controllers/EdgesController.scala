package controllers

import com.google.inject.Inject
import daos.EdgeDao
import models.Domain._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.Future



class EdgesController @Inject()(edgeDao: EdgeDao) extends Controller {
  import EdgesController._
  import models.DTO._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  def find(fromId: ID, toId: ID) = Action.async {
    edgeDao.find(fromId, toId).map({
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    })
  }

  def create() = Action.async(parse.json) { implicit request =>
    request.body.asOpt[Edge].map({ edge =>
      edgeDao.create(edge).map({
        case Right(x) => Ok(Json.toJson(x))
        case Left(error) => representError(error)
      })
    }).getOrElse(Future.successful(BadRequest(makeError(s"Invalid body: ${request.body}"))))
  }

  def update(fromId: ID, toId: ID) = Action.async(parse.json) { implicit request =>
    request.body.asOpt[Edge].map({ edge =>
      if (edge.from != fromId || edge.to != toId) {
        Future.successful(BadRequest(makeError("Invalid try to change nodes in already created edge")))
      } else {
        edgeDao.update(edge).map({
          case Right(x) => Ok(Json.toJson(x))
          case Left(error) => representError(error)
        })
      }
    }).getOrElse(Future.successful(BadRequest(makeError(s"Invalid body: ${request.body}"))))
  }

  def delete(fromId: ID, toId: ID) = Action.async {
    edgeDao.delete(fromId, toId).map({
      case Right(_) => Ok
      case Left(error) => representError(error)
    })
  }

  def allEdgesFrom(nodeId: ID) = Action.async {
    edgeDao.findAllEdgesFrom(nodeId).map({
      edges => Ok(Json.toJson(edges))
    })
  }
}

object EdgesController {
  import EdgeDao._

  def makeError(msg: String): JsValue = {
    Json.obj("message" -> msg)
  }

  def representError(error: EdgeDaoError) = {
    error match {
      case NodeNotFound(nodeId: ID) => Results.NotFound(makeError(s"Node not found $nodeId"))
      case NotFound(from: ID, to : ID) => Results.NotFound(makeError(s"Edge between $from and $to not found"))
      case InvalidData(from: ID, to: ID) => Results.BadRequest(makeError(s"Invalid data $from -> $to"))
      case EdgeAlreadyExists(existed: Edge) =>
        Results.BadRequest(makeError(s"Edge already exists ${existed.from} -> ${existed.to}, weight = ${existed.weight}"))
      case StorageError(id: ID, ex: Exception) => Results.InternalServerError(makeError("Some error occurred"))
    }
  }
}