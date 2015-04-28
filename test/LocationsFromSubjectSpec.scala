import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class LocationsFromSubjectSpec extends Specification {

  "LocationsFromSubjectController" should {

    "request json and return stubed json" in new WithApplication {
      val home = route(FakeRequest(POST, "/locations_from_subject").withJsonBody(Json.parse("{}"))).get
      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "application/json")
      private val content = Json.parse(contentAsString(home))
      private val messageValue: String = (content \ ("message")).toString().replace("\"", "")
      messageValue mustEqual ("This service is under construction")
    }
  }

}