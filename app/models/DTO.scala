package models

import models.Domain.{Edge, Meta, Node, Weight}
import play.api.libs.json.Json


object DTO {

  implicit val metaFormat = Json.format[Meta]
  implicit val weightFormat = Json.format[Weight]
  implicit val nodeFormat = Json.format[Node]
  implicit val edgeFormat = Json.format[Edge]
}
