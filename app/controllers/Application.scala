package controllers

import play.api.libs.EventSource
import play.api.libs.iteratee.{Enumerator, Concurrent}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._

object Application extends Controller {

  val (chatOut, chatChannel) = Concurrent.broadcast[JsValue]

  def index = Action { implicit req =>
    Ok(views.html.index(routes.Application.chatFeed(), routes.Application.postMessage()))
  }

  def welcome = Enumerator.apply[JsValue](Json.obj(
    "user" -> "Bot",
    "message"->"Welcome! Enter a message and hit ENTER."
  ))

  def chatFeed = Action { req =>
    println("Someone connected: "+req.remoteAddress)
    Ok.chunked(welcome >>> chatOut
      &> EventSource()
    ).as("text/event-stream")
  }

  def postMessage = Action(parse.json) { req => chatChannel.push(req.body); Ok }
}