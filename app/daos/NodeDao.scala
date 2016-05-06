package daos

import com.google.inject.{Inject, Singleton}
import models.Domain._
import repositories.SqlRepository

import scala.concurrent.Future

@Singleton
class NodeDao @Inject()(nodesRepo: SqlRepository[Node], edgeDao: EdgeDao) {
  import NodeDao._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  def list(): Future[Seq[Node]] = {
    nodesRepo.list()
  }

  def find(id: ID): Future[Option[Node]] = {
    nodesRepo.findById(id)
  }

  def create(create: Node): Future[Either[NodeDaoError, Node]] = {
    nodesRepo.save(create).map(saved => Right(saved))
  }

  def update(id: ID, updated: Node): Future[Either[NodeDaoError, Node]] = {
    nodesRepo.update(id, updated).map({
      case Some(node) => Right(node)
      case None => Left(NotFound(id))
    })
  }

  def delete(id: ID): Future[Either[NodeDaoError, Unit]] = {
    nodesRepo.delete(id).map({
      case Some(node) =>
        edgeDao.deleteAllEdgesFrom(id)
        Right({})
      case None => Left(NotFound(id))
    })
  }
}

object NodeDao {

  sealed trait NodeDaoError
  case class NotFound(id: ID) extends NodeDaoError
  case class InvalidData(id: ID) extends NodeDaoError
  case class StorageError(id: ID, ex: Exception) extends NodeDaoError
}
