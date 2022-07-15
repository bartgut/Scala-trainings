package websocket.http.protocol

import io.circe.generic.extras.Configuration

object WebSocketProtocol {

  implicit val codecConfiguration: Configuration =
    Configuration.default.withDiscriminator("message_type")
      .copy(transformConstructorNames = _.split("(?=\\p{Upper})").map(_.toLowerCase).mkString("."))

  // requests
  sealed trait WebSocketRequest
  case class RequestEcho(value: String) extends WebSocketRequest
  case class RequestBase64(value: String) extends WebSocketRequest

  // response
  sealed trait WebSocketResponse
  case class ResponseEcho(value: String) extends WebSocketResponse
  case class ResponseBase64(source: String, coded: String) extends WebSocketResponse

}
