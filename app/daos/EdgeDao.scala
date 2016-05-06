package daos

import com.google.inject.{Inject, Singleton}
import models.Domain.{Edge, _}
import play.api.libs.json.Json
import repositories.{KeyValueRepository, SqlRepository}

import scala.concurrent.Future
import EdgeDao._


@Singleton
class EdgeDao @Inject()(nodesRepo: SqlRepository[Node], edgesRepo: KeyValueRepository[EdgesHolder]) {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def findAllEdgesFrom(nodeId: ID): Future[Seq[Edge]] = {
    edgesRepo.get(nodeId)(edgesHolderFormat)
      .map(result => result.map({_.edges}).getOrElse(Seq()))
  }

  def deleteAllEdgesFrom(nodeId: ID): Future[Unit] = {
    edgesRepo.delete(nodeId)
  }

  def find(from: ID, to: ID): Future[Option[Edge]] = {
    edgesRepo.get(from)
        .map({ _.flatMap({ _.edges.find({ _.to == to }) }) })
  }

  def update(updated: Edge): Future[Either[EdgeDaoError, Edge]] = {
    for {
      stored <- edgesRepo.get(updated.from)
    } yield {

      val isEdgeExist = stored.flatMap({ _.edges.find({ _.to == updated.to }) }).isDefined
      if(!isEdgeExist) {
        Left(NotFound(updated.from, updated.to))
      } else {
        val edges = stored.get.edges.filter({_.to != updated.to}) :+ updated
        edgesRepo.put(updated.from, stored.get.copy(edges = edges))
        Right(updated)
      }
    }
  }

  def delete(from: ID, to: ID): Future[Either[EdgeDaoError, Unit]] = {
    for {
      stored <- edgesRepo.get(from)
    } yield {

      val isEdgeExist = stored.flatMap({ _.edges.find({ _.to == to }) }).isDefined
      if(!isEdgeExist) {
        Left(NotFound(from, to))
      } else {
        val edges = stored.get.edges.filter({_.to != to})
        edgesRepo.put(from, stored.get.copy(edges = edges))
        Right({})
      }
    }
  }

  def create(edge: Edge): Future[Either[EdgeDaoError, Edge]] = {

    val findNodeFrom = nodesRepo.findById(edge.from)
    val findNodeTo = nodesRepo.findById(edge.to)

    val isNodesExists = for {
      nodeFrom <- findNodeFrom
      nodeTo <- findNodeTo
    } yield {
      if (nodeFrom.isEmpty) {
        Left(NodeNotFound(edge.from))
      } else if (nodeTo.isEmpty) {
        Left(NodeNotFound(edge.to))
      } else {
        Right({})
      }
    }

    for {
      valid <- isNodesExists
      edgesFrom <- edgesRepo.get(edge.from)
    } yield {
      valid.right.flatMap(_ => {

        val stored = edgesFrom.getOrElse(EdgesHolder(Seq()))
        val alreadyExisted = stored.edges.find(_.to == edge.to)

        if(alreadyExisted.isDefined) {
          Left(EdgeAlreadyExists(alreadyExisted.get))
        } else {
          val edges = stored.edges :+ edge
          edgesRepo.put(edge.from, stored.copy(edges = edges))
          Right(edge)
        }
      })
    }
  }
}

object EdgeDao {

  case class EdgesHolder(edges: Seq[Edge])

  implicit val weightFormat = Json.format[Weight]
  implicit val edgeFormat = Json.format[Edge]
  implicit val edgesHolderFormat = Json.format[EdgesHolder]


  sealed trait EdgeDaoError
  case class NodeNotFound(nodeId: ID) extends EdgeDaoError
  case class NotFound(from: ID, to : ID) extends EdgeDaoError
  case class InvalidData(from: ID, to: ID) extends EdgeDaoError
  case class StorageError(id: ID, ex: Exception) extends EdgeDaoError
  case class EdgeAlreadyExists(existed: Edge) extends EdgeDaoError
}