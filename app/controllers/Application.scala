package controllers

import play.api.libs.EventSource
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.{Enumerator, Concurrent, Enumeratee}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._

object Application extends Controller {
  
  /** Central hub for distributing chat messages */
  val (chatOut, chatChannel) = Concurrent.broadcast[JsValue]

  def index = Action { implicit req =>
    Ok(views.html.index(routes.Application.chatFeed, routes.Application.postMessage))
  }

  /** Enumeratee for detecting disconnect of SSE stream */
  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] =
    Enumeratee.onIterateeDone{ () => println(addr + " - SSE disconnected") }

  def welcome = Enumerator.apply[JsValue](Json.obj(
    "user" -> "Bot",
    "message" -> "Welcome! Write a message and hit ENTER."
  ))

  /** Controller action serving activity */
  def chatFeed = Action { req =>
    println(req.remoteAddress + " - connected")
    Ok.chunked(welcome >>> chatOut
      &> connDeathWatch(req.remoteAddress)
      &> EventSource()
    ).as("text/event-stream")
  }

  /** Controller action for POSTing chat messages */
  def postMessage = Action(parse.json) { req => chatChannel.push(req.body); Ok }

}