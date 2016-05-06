package repositories

import java.util.UUID

import models.Domain.{ID, Node}
import com.google.inject.Singleton

import scala.concurrent.Future
import java.util.concurrent._
import collection.JavaConverters._

@Singleton
class SqlRepository[T <: Node] {

  val map = new ConcurrentHashMap[String, T]().asScala

  def list(): Future[Seq[T]] = {
    Future.successful(map.values.toSeq)
  }

  def findById(id: ID): Future[Option[T]] = {
    Future.successful(map.get(id.toString))
  }
  
  def save(entity: T): Future[T] = Future.successful {
    val id = generateID()
    val toSave = entity.copy(id = id).asInstanceOf[T]
    map.put(id.toString, toSave)
    toSave
  }
  
  def delete(id: ID): Future[Option[T]] = Future.successful {
    val res = map.get(id.toString)
    map.remove(id.toString)
    res
  }
  
  def update(id: ID, updated: T): Future[Option[T]] = Future.successful {
    map.get(id.toString).map( _ => {
      map.put(id.toString, updated)
      updated
    })
  }
  
  
  def generateID(): ID = UUID.randomUUID()
}
