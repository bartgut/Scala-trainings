package websocket.http.routes

import cats.effect.Async
import cats.effect.kernel.Sync
import cats.implicits._
import io.circe.generic.extras.auto._
import org.http4s.HttpRoutes
import org.http4s.server.websocket.WebSocketBuilder2
import websocket.http.logic.WebSocketLogic
import websocket.http.protocol.WebSocketProtocol._
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir._

class WebSocketRoute[F[_]: Sync : Async] {

  private val simpleEndpointDefinition =
    endpoint.get
      .in("easy")
      .errorOut(stringBody)
      .out(webSocketBody[WebSocketRequest, CodecFormat.Json, WebSocketResponse, CodecFormat.Json](Fs2Streams[F]))


  private val complexEndpointDefinition =
    endpoint.get
      .in("complex")
      .errorOut(stringBody)
      .out(webSocketBodyRaw(Fs2Streams[F]))

  private val simpleEndpointServerLogic =
    simpleEndpointDefinition.serverLogic(_ => new WebSocketLogic[F].pipe.map(_.asRight[String]))

  private val complexEndpointServerLogic =
    complexEndpointDefinition.serverLogic(_ => new WebSocketLogic[F].framePipe.map(_.asRight[String]))

  val routes: WebSocketBuilder2[F] => HttpRoutes[F] =
    Http4sServerInterpreter[F]().toWebSocketRoutes(List(simpleEndpointServerLogic, complexEndpointServerLogic))


}
