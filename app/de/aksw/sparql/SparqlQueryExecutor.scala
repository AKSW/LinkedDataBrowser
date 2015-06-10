package de.aksw.sparql

import akka.actor._
import akka.util.Timeout
import com.hp.hpl.jena.query.{QueryFactory, QueryExecutionFactory, QueryExecution, Query}
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.concurrent.duration._
import scala.util.{Success, Try, Failure}
import de.aksw.Constants._

/**
 * Created by dhaeb on 01.06.15.
 */

object SparqlSubjectQueryRequest {
  def querystring(uri: String) =
    s"""|
        |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        |PREFIX owl: <http://www.w3.org/2002/07/owl#>
        |
        |CONSTRUCT{ <${uri}> ?p ?o}
        |WHERE {
        |    <${uri}> ?p ?o .
        |    FILTER(!isLiteral(?o) || lang(?o) = "" || langMatches(lang(?o), "EN"))
        |    FILTER NOT EXISTS {
        |        <${uri}> rdf:type ?o .
        |        ?o rdfs:subClassOf ?directType .
        |        FILTER NOT EXISTS {
        |            ?o owl:equivalentClass ?directType .
        |        }
        |    }
        |}""".stripMargin
}

case class SparqlSubjectQueryRequest(endpoint: String, uri: String) {

  import SparqlSubjectQueryRequest._

  def constructSubjectSparqlQueryTemplate = QueryFactory.create(querystring(uri))

  val query = constructSubjectSparqlQueryTemplate
}

class SparqlQueryExecutor extends Actor {

  override def receive: Receive = {
    case r: SparqlSubjectQueryRequest => {
      blocking {
        sender !(r.uri, Try(QueryExecutionFactory.sparqlService(r.endpoint, r.query)).map(_.execConstruct()))
        context.stop(self)
      }
    }
  }

}

import akka.pattern.ask

object SparqlQueryCache {
  implicit val timeout = Timeout(10 seconds)
  val system = ActorSystem()
  val sparqlQueryExecutor = system.actorOf(Props[SparqlQueryCache])

  /**
   * This is the function to execute a sparql query.
   * In the moment, due to the way the SparqlSubhectQueryRequest is implemented,
   * only one specific query is supported.
   *
   * @param req The request which contains a SPARQL construct query for all triples of one subject
   * @return a scala.util.Try with the Model or an exception in the unsucessful case
   */
  def executeSparqlSubjectQuery(req: SparqlSubjectQueryRequest) = {
    blocking {
      Await.result(sparqlQueryExecutor ? req, timeout.duration).asInstanceOf[Try[Model]]
    }
  }

}

class SparqlQueryCache extends Actor {

  val maxTimeInCacheInNanos = (1 hour).toNanos

  type CacheEntry = (Model, Long)
  type Cache = Map[String, CacheEntry]
  type Interested = Map[String, List[ActorRef]]

  override def receive: Actor.Receive = {
    case e: SparqlSubjectQueryRequest => {
      perfromFreshQuery(Map(), Map(), e)
    }
  }

  def receive(cache: Cache, interested: Interested): Actor.Receive = {
    case e: SparqlSubjectQueryRequest => {
      val uri: String = e.uri
      if (cache.contains(uri)) {
        handleCacheHit(cache, interested, e)
      } else if (interested.contains(uri)) {
        val newInteressted: List[ActorRef] = sender :: interested(uri)
        context.become(receive(cache, interested.updated(uri, newInteressted)))
      } else {
        perfromFreshQuery(cache, interested, e)
      }
    }
    case (uri: String, s@Success(m: Model)) => {
      interested(uri).foreach(_ ! s)
      val newInterested: Interested = interested - uri
      val newCache: Cache = cache.updated(uri, (m, System.nanoTime()))
      context.become(receive(newCache, newInterested))
    }
    case (uri: String, f @ Failure(_)) => {
      interested(uri).foreach(_ ! f)
      val newInterested: Interested = interested - uri
      context.become(receive(cache, newInterested))
    }

  }

  def handleCacheHit(cache: Cache, interested: Interested, e: SparqlSubjectQueryRequest): Unit = {
    val uri: String = e.uri
    val entry: CacheEntry = cache(uri)
    val now = System.nanoTime()
    if (now - entry._2 > maxTimeInCacheInNanos) {
      val newCache: Map[String, (Model, Long)] = cache - uri
      perfromFreshQuery(newCache, interested, e)
    } else {
      sender ! Try(entry._1)
    }
  }

  def perfromFreshQuery(cache: Cache, interested: Interested, e: SparqlSubjectQueryRequest): Unit = {
    startWorker(e)
    context.become(newUriReceiver(e.uri, cache, interested))
  }

  def startWorker(e: SparqlSubjectQueryRequest): Unit = {
    context.actorOf(Props[SparqlQueryExecutor]) ! e
  }

  def newUriReceiver(uri: String, cache: Cache, interessted: Interested): Actor.Receive = {
    receive(cache, interessted.updated(uri, List(sender)))
  }
}
