package websocket.http

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import websocket.http.routes.WebSocketRoute

object Application extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpWebSocketApp(wsb => Router("/" -> new WebSocketRoute[IO].routes(wsb)).orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

}
