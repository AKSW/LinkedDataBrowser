package controllers

import de.aksw.Constants
import play.api.mvc._

object LdbMainController extends Controller {

  def index = Action {
    Ok(views.html.ldb_harness("Linked Open Data Browser")(Constants.DBPEDIA_ENDPOINT))
  }


}