package controllers

import javax.inject._

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl._
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class Application @Inject()(cc: ControllerComponents)(implicit mat: Materializer) extends AbstractController(cc) {

  /** Central hub for distributing chat messages */
  //val (chatOut, chatChannel) = Concurrent.broadcast[JsValue]

  private[this] val (chatSink, chatSource) =
  MergeHub.source[JsValue]
    .toMat(BroadcastHub.sink[JsValue])(Keep.both)
    .run()

  def index = Action { implicit req =>
    Ok(views.html.index(routes.Application.chatFeed, routes.Application.postMessage))
  }

  /** Enumeratee for detecting disconnect of SSE stream */
  //  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] =
  //    Enumeratee.onIterateeDone{ () => println(addr + " - SSE disconnected") }

  def welcome: Source[JsValue, NotUsed] = Source.single[JsValue](Json.obj(
    "user" -> "Bot",
    "message" -> "Welcome! Write a message and hit ENTER."
  ))

  /** Controller action serving activity */
  def chatFeed = Action { req =>
    val userAddress = req.remoteAddress
    Logger.info(s"$userAddress - connected")
    val combinedSource: Source[JsValue, NotUsed] = Source.combine(welcome, chatSource)(Concat(_))
    val watchFlow = EventSource.flow[JsValue].watchTermination()((_, termination) => termination.onComplete(_ => {
      Logger.info(s"$userAddress - SSE disconnected")
    }))
    Ok.chunked(combinedSource via watchFlow).as(ContentTypes.EVENT_STREAM)
  }

  /** Controller action for POSTing chat messages */
  def postMessage = Action(parse.json) { req =>
    Source.single(Json.toJson(req.body)).runWith(chatSink)
    Ok
  }
}