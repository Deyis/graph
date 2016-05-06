package models

import java.util.UUID


object Domain {

  type ID = UUID
  type Distance = Int

  case class Weight(distance: Distance)
  case class Meta(name: String)
  case class Node(id: ID, meta: Meta)
  case class Edge(from: ID, to: ID, weight: Weight)
}
