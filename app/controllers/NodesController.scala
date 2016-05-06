package controllers


import com.google.inject.Inject
import daos.NodeDao
import daos.NodeDao._
import models.Domain._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.Future

class NodesController @Inject()(nodeDao: NodeDao) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import NodesController._
  import models.DTO._

  def list() = Action.async {
    nodeDao.list().map({ seq => Ok(Json.toJson(seq)) })
  }

  def find(id: ID) = Action.async {
    nodeDao.find(id).map({
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    })
  }

  def create() = Action.async(parse.json) { implicit request =>
    request.body.asOpt[Node].map({ node =>
      nodeDao.create(node).map({
        case Right(x) => Ok(Json.toJson(x))
        case Left(error) => representError(error)
      })
    }).getOrElse(Future.successful(BadRequest(makeError(s"Invalid body: ${request.body}"))))
  }

  def update(id: ID) = Action.async(parse.json) { implicit request =>
    request.body.asOpt[Node].map({ node =>
      if(node.id != id) {
        Future.successful(BadRequest(makeError("Id in body should match with the url")))
      } else {
        nodeDao.update(id, node).map({
          case Right(x) => Ok(Json.toJson(x))
          case Left(error) => representError(error)
        })
      }
    }).getOrElse(Future.successful(BadRequest(makeError(s"Invalid body: ${request.body}"))))
  }

  def delete(id: ID) = Action.async {
    nodeDao.delete(id).map({
      case Right(_) => Ok
      case Left(error) => representError(error)
    })
  }
}


object NodesController {

  def makeError(msg: String): JsValue = {
    Json.obj("message" -> msg)
  }

  def representError(error: NodeDaoError) = {
    error match {
      case NotFound(id: ID) => Results.NotFound(makeError(s"Node with id $id not found"))
      case InvalidData(id: ID) => Results.BadRequest
      case StorageError(id: ID, ex: Exception) => Results.InternalServerError(makeError("Some error occurred"))
    }
  }
}