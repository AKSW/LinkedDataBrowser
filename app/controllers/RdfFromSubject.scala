package controllers

import controllers.SearchSuggestionController._
import play.api.libs.json.Json
import play.api.mvc.Action

object RdfFromSubject {
    def index = Action(play.api.mvc.BodyParsers.parse.json) { request =>
      val transferable: Map[String, String] = Map("message" -> "This service is under construction")
      Ok(Json.toJson(transferable))
    }
}