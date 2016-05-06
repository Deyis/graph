package repositories

import models.Domain.ID
import play.api.libs.json._
import com.google.inject.Singleton
import scala.concurrent.Future
import java.util.concurrent._
import collection.JavaConverters._

@Singleton
class KeyValueRepository[T] {
  import KeyValueRepository._

  val map = new ConcurrentHashMap[String, Value]().asScala

  def put(key: Key, value: T)(implicit json: Writes[T]): Future[Unit] = Future.successful {
    map.put(key.toString, json.writes(value))
  }
  
  def get(key: Key)(implicit json: Reads[T]): Future[Option[T]] = Future.successful {
    map.get(key.toString).flatMap({ json.reads(_).asOpt })
  }

  def delete(key: Key): Future[Unit] = Future.successful {
    map.remove(key.toString)
  }
}

object  KeyValueRepository {

  type Key = ID
  type Value = JsValue
}