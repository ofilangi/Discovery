package inrae.data

import com.dimafeng.testcontainers.GenericContainer
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.Method.POST
import fr.hmil.roshttp.exceptions.HttpException
import fr.hmil.roshttp.response.SimpleHttpResponse
import inrae.semantic_web.StatementConfiguration
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.PullPolicy
import wvlet.log.Logger.rootLogger.{debug, error, info}

import java.io.IOException
import java.time.Duration
import java.time.temporal.ChronoUnit
import scala.util.{Failure, Success}

final case class DataTestFactoryException(private val message: String = "",
                                          private val cause: Throwable = None.orNull) extends Exception(message,cause)

object DataTestFactory  {
  import monix.execution.Scheduler.Implicits.global

  val virtuoso_cont = GenericContainer("tenforce/virtuoso:virtuoso7.2.5",
    exposedPorts=List(8890),
    env=Map(
      "DBA_PASSWORD" -> "dba",
      "SPARQL_UPDATE" -> "true",
      "DEFAULT_GRAPH" -> "graph:test:discovery:default:",
      "VIRT_Parameters_NumberOfBuffers" -> "5100",
      "VIRT_Parameters_MaxDirtyBuffers" -> "3750",
      "VIRT_Parameters_TN_MAX_memory" -> "4000000",
      "VIRT_Parameters_TransactionAfterImageLimit" -> "50000",
      "VIRT_SPARQL_ResultSetMaxRows" -> "100",
      "VIRT_SPARQL_MaxDataSourceSize" -> "10000",
      "VIRT_SPARQL_MaxQueryCostEstimationTime" -> "0",
      "VIRT_SPARQL_MaxQueryExecutionTime" -> "5",
    ),
    waitStrategy=Wait.forHttp("/sparql")
      .withStartupTimeout(Duration.of(100,ChronoUnit.SECONDS)),
    imagePullPolicy=PullPolicy.alwaysPull()
  )
  //Wait.forLogMessage("Server online at 1111", 1)
  //
  virtuoso_cont.container.start()
  val url_endpoint ="http://"+virtuoso_cont.container.getHost() + ":" + virtuoso_cont.container.getFirstMappedPort()+"/sparql"



  def put(stringQuery : String, url_endpoint : String) = {
    HttpRequest(url_endpoint)
      //  .withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString("dba:dba".getBytes))
      .withMethod(POST)
      .withQueryParameter("query",stringQuery)
      .send()
      .recover {
        case HttpException(e: SimpleHttpResponse) =>
          // Here we may have some detailed application-level insight about the error
          error("There was an issue with your request." +
            " Here is what the application server says: " )
        case e: IOException =>
          error(s"${url_endpoint} is not reachable. ")
      }
  }

  def graph1(classname: String) = "graph:test:discovery:virtuoso1:" + classname.replace("$","")
  def graph2(classname: String) = "graph:test:discovery:virtuoso2:" + classname.replace("$","")

  private def insert(data : String,
                     graph: String,
                     url_endpoint : String=url_endpoint) = {

    put(s"""
        PREFIX owl: <http://www.w3.org/2002/07/owl#>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

        INSERT {
            GRAPH <${graph}>
              {
                ${data}
              }
          }
        """.stripMargin,url_endpoint).onComplete({
      case Success(_) => {
        debug(s"${graph} is loaded !")
      }
      case Failure(_) => throw new Error(s"Can not load graph :${graph}")
    })
  }

  def insert_virtuoso1(data : String,
                       classname: String,
                       url_endpoint : String=url_endpoint) = insert(data,graph1(classname),url_endpoint)

  def insert_virtuoso2(data : String,
                       classname: String,
                       url_endpoint : String=url_endpoint)= insert(data,graph2(classname),url_endpoint)

  private def delete(graph: String,
                     url_endpoint : String=url_endpoint) = {
    put(s"DROP SILENT GRAPH <${graph}>",url_endpoint)

  }

  def delete_virtuoso1(classname: String,
                       url_endpoint : String=url_endpoint) = delete(graph1(classname),url_endpoint)

  def delete_virtuoso2(classname: String,
                       url_endpoint : String=url_endpoint) = delete(graph2(classname),url_endpoint)


  def getConfigVirtuoso1() : StatementConfiguration = {
    StatementConfiguration().setConfigString(
      s"""
        {
         "sources" : [{
           "id"       : "local",
           "url"      : "${DataTestFactory.url_endpoint}",
           "type"     : "tps",
           "method"   : "POST",
           "mimetype" : "json"
         }],
         "settings" : {
            "logLevel" : "info",
            "sizeBatchProcessing" : 100
          }
         }
        """.stripMargin)
  }

  def getConfigVirtuoso2() : StatementConfiguration = {
    StatementConfiguration().setConfigString(
      s"""
        {
         "sources" : [{
           "id"       : "local",
           "url"      : "${DataTestFactory.url_endpoint}",
           "type"     : "tps",
           "method"   : "POST",
           "mimetype" : "json"
         }],
         "settings" : {
            "logLevel" : "info",
            "sizeBatchProcessing" : 100
          }
         }
        """.stripMargin)
  }

  def getDbpediaConfig() : StatementConfiguration = {
    StatementConfiguration().setConfigString(
      s"""
            {
             "sources" : [{
               "id"  : "dbpedia",
               "url" : "https://dbpedia.org/sparql",
               "type" : "tps",
               "method" : "POST"
             }],
            "settings" : {
              "driver" : "inrae.semantic_web.driver.JenaRequestDriver",
              "logLevel" : "trace",
              "sizeBatchProcessing" : 100,
              "cache" : false
             }
            }
            """.stripMargin.stripMargin)
  }
  //   "driver" : "inrae.semantic_web.driver.JenaRequestDriver",
}
