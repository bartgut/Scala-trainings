package websocket.http.logic

import cats.Monad
import fs2.Pipe
import io.circe.generic.extras.auto._
import io.circe.parser.parse
import io.circe.syntax._

import websocket.http.protocol.WebSocketProtocol._
import sttp.ws.WebSocketFrame
import sttp.ws.WebSocketFrame._


import java.util.Base64

class WebSocketLogic[F[_]: Monad] {

  def pipe: F[Pipe[F, WebSocketRequest, WebSocketResponse]] = {
    Monad[F].pure{ input: fs2.Stream[F, WebSocketRequest] => input.map(handleRequest) }
  }

  def framePipe: F[Pipe[F, WebSocketFrame, WebSocketFrame]] =
    Monad[F].pure( { input: fs2.Stream[F, WebSocketFrame] =>
      input.map {
        case WebSocketFrame.Text(v, _, _) => handleTextFrame(v).fold(_ => close, p =>  text(p.asJson.toString()))
        case WebSocketFrame.Ping(p) => WebSocketFrame.Pong(p)
      }
    })

  private def handleRequest(webSocketRequest: WebSocketRequest): WebSocketResponse =
    webSocketRequest match {
      case RequestEcho(value) => ResponseEcho(value)
      case RequestBase64(value) =>
        ResponseBase64(
          value,
          new String(Base64.getEncoder.encode(value.getBytes))
        )
    }

  private def handleTextFrame(value: String): Either[Throwable, WebSocketResponse] = {
    for {
      json <- parse(value)
      request <- json.as[WebSocketRequest]
    } yield handleRequest(request)
  }


}
