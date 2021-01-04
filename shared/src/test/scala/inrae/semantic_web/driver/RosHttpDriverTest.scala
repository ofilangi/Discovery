package inrae.semantic_web.driver

import inrae.data.DataTestFactory
import inrae.semantic_web.sparql.ConfigurationHttpRequest
import utest.{TestSuite, Tests, test}
import wvlet.log.Logger.rootLogger.error

import scala.util.{Failure, Success}
import inrae.semantic_web.driver._
import monix.execution.Scheduler.Implicits.global
import wvlet.log.{LogLevel, Logger}

object RosHttpDriverTest extends TestSuite {

 // DataTestFactory.delete_virtuoso1(this.getClass.getSimpleName)

  DataTestFactory.insert_virtuoso1(
    """
      <aaRosHttpDriverTest> <bb> <cc> .
      """.stripMargin, this.getClass.getSimpleName)

  override def utestAfterAll(): Unit = {
    DataTestFactory.delete_virtuoso1(this.getClass.getSimpleName)
  }

  Logger.setDefaultLogLevel(LogLevel.OFF)
  val query = "select ?b ?c where { <aaRosHttpDriverTest> ?b ?c . } limit 1"

  def tests = Tests {
    test("get") {
      RosHTTPDriver().get(query, ConfigurationHttpRequest(url = DataTestFactory.url_endpoint))
        .onComplete {
          case Success(qr) => {
            assert(qr.json("results")("bindings").arr(0)("b")("value").value=="bb")
            assert(qr.json("results")("bindings").arr(0)("c")("value").value=="cc")
          }
          case Failure(e) => {
            error(e.getMessage())
            assert(false)
          }
        }
    }

    test("get bad request") {
      RosHTTPDriver().get("bad request", ConfigurationHttpRequest(url = DataTestFactory.url_endpoint))
        .onComplete {
          case Success(_) => {
            assert(false)
          }
          case Failure(_) => {
            assert(true)
          }
        }
    }
    test("get malformed endpoint") {
      RosHTTPDriver().get(query, ConfigurationHttpRequest(url = "bidon"))
        .onComplete {
          case Success(_) => {
            assert(false)
          }
          case Failure(_) => {
            assert(true)
          }
        }
    }

    test("get endpoint does not exist") {
      RosHTTPDriver().get(query, ConfigurationHttpRequest(url = "http://bidon.com"))
        .onComplete {
          case Success(_) => {
            /* RosHTTP does not detect an unreachable endpoint */
            assert(true)
          }
          case Failure(_) => {
            assert(true)
          }
        }
    }
    test("post") {
      RosHTTPDriver().post(query, ConfigurationHttpRequest(url = DataTestFactory.url_endpoint))
        .onComplete {
          case Success(qr) => {
            assert(qr.json("results")("bindings").arr(0)("b")("value").value=="bb")
            assert(qr.json("results")("bindings").arr(0)("c")("value").value=="cc")
          }
          case Failure(e) => {
            error(e.getMessage())
            assert(false)
          }
        }
    }

    test("post bad request") {
      RosHTTPDriver().post("bad request", ConfigurationHttpRequest(url = DataTestFactory.url_endpoint))
        .onComplete {
          case Success(_) => {
            assert(false)
          }
          case Failure(_) => {
            assert(true)
          }
        }
    }
    test("post malformed endpoint") {
      RosHTTPDriver().post(query, ConfigurationHttpRequest(url = "bidon"))
        .onComplete {
          case Success(_) => {
            assert(false)
          }
          case Failure(_) => {
            assert(true)
          }
        }
    }

    test("post endpoint does not exist") {
      RosHTTPDriver().post(query, ConfigurationHttpRequest(url = "http://bidon.com"))
        .onComplete {
          case Success(_) => {
            assert(false)
          }
          case Failure(_) => {
            assert(true)
          }
        }
    }
  }
}
