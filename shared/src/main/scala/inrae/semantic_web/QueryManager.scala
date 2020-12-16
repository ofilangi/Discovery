package inrae.semantic_web
import wvlet.log.Logger.rootLogger._
import inrae.semantic_web.internal._
import inrae.semantic_web.rdf._
import inrae.semantic_web.sparql.{QueryResult, _}
import inrae.semantic_web.sparql.QueryRunner

import scala.concurrent.{Future, Promise}
import scala.util._


case class QueryManager(config : StatementConfiguration) {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
   *
   * @param root : root request
   * @return Sparql query string
   */
  def sparql_string(root: Root): String = {
    val (refToIdentifier,_) = pm.SparqlGenerator.correspondenceVariablesIdentifier(root)
    SparqlQueryBuilder.selectQueryString(root,refToIdentifier,refToIdentifier.values.toSeq,0,0)
  }

  /**
   * queryAll
   * Apply a select on all variables
   * @param rootRequest : root request
   * @param limit : upper bound on the number of solutions returned
   * @param offset : solution are generated after this offset
   * @return Solutions embedded in QueryResult object as Future
   */
  def queryAll(rootRequest : Root,limit : Int, offset : Int) : Future[QueryResult] = {
    debug(" -- queryAll -- ")
    queryVariables(rootRequest,Node.references(rootRequest),limit,offset)
  }

  def countNbSolutions(root : Root) : Future[Int] = {
    debug(" -- countNbSolutions -- ")

    if (config.sources().length == 0) {
      Future { throw new DiscoveryException(" ** None sources available ** ") }
    } else if (config.sources().length == 1) {
      val source = config.sources()(0)
      val (refToIdentifier, _) = pm.SparqlGenerator.correspondenceVariablesIdentifier(root)
      val varCount = "count"

      val query = SparqlQueryBuilder.countQueryString(root,refToIdentifier,varCount)
      val res: Future[QueryResult] = QueryRunner(source,config.conf.settings).query(query)
      res.map(v => {
        SparqlBuilder.createLiteral(v.json("results")("bindings")(0)(varCount)).toInt()
      })
    } else {
      // todo query planner
      Future { throw new DiscoveryException("QueryPlanner is not available .") }
    }
  }

  /**
   *  queryVariables
   *
   * @param root : first node to build the sparql query
   * @param listVariables : slected variables
   * @param limit : upper bound on the number of solutions returned
   * @param offset : solution are generated after this offset
   * @return Solutions embedded in QueryResult object as Future
   */
  def queryVariables(root: Root,
                     listVariables : Seq[String],limit : Int, offset : Int) : Future[QueryResult] = {
    debug(" -- queryVariables -- ")

    trace( pm.SimpleConsole.get(root) )

    config.sources().length match {
      case 0 => {
        Future {
          throw DiscoveryException(" ** No sources available ** ")
        }
      }
      case 1 => {
        val (refToIdentifier, _) = pm.SparqlGenerator.correspondenceVariablesIdentifier(root)
        trace(refToIdentifier.toString())
        val query: String = SparqlQueryBuilder.selectQueryString(root, refToIdentifier, listVariables,limit,offset)
        QueryRunner(config.sources()(0),config.conf.settings).query(query).map( qr => {
          qr
        })
      }
      case _ => {
        val plan = QueryPlanner.buildPlanning(root) //,listVariables,config)
        val plan_results_set = QueryPlanner.ordonnanceBySource(plan, root)
        QueryPlannerExecutor.executePlanning(root, plan_results_set, listVariables, config, root.prefixes)
      }
    }
  }

  /**
   * Assign a list of source (existence of a remote persistence) if possible
   * @param n
   * @param source
   * @return
   */
  def setUpSourcesNode(n: Node,
                       prefixes : Map[String,IRI]): Future[Option[SourcesNode]] = {

    n match {
      case s : Something =>
        /* Something is Everywhere !! */
        Future {
          Some(SourcesNode(s.reference(),config.sources().map( _.id )))
        }
      case r : RdfNode =>
        val (refToIdentifier,_) = pm.SparqlGenerator.correspondenceVariablesIdentifier(n)
        val query = pm.SparqlGenerator.prefixes(prefixes) + "\n" +
          pm.SparqlGenerator.queryFormSelect() + "\n" +
          pm.SparqlGenerator.start_where() + "\n" +
          pm.SparqlGenerator.sparqlNode(r,refToIdentifier,"varUp","varCur") +
          pm.SparqlGenerator.solutionModifier(1,0)

        trace(query)

        val nbRowResultsBySource : Seq[Future[Boolean]] = {
          config.sources().map(
              source => QueryRunner(source,config.conf.settings).query(query)
            ).map( {
              ( _.map(rr => rr.json("results")("bindings").arr.length>0))
            })
          }

        val y2 = Future.sequence(nbRowResultsBySource)
        val y3 = Promise[Option[SourcesNode]]()

        y2.onComplete {
          case Success(lCheck) => {
            y3 success Some(SourcesNode(r.reference(), lCheck.zip(config.sources()).filter( _._1).map( _._2.id)))
          }
          case msg => {
           error(msg)
            y3 success (None)
          }
        }

        y3.future

      case _ => Future(None)
    }
  }

  def process_datatypes(qr : QueryResult,
                        datatypeNode : DatatypeNode,
                        lUris : Seq[SparqlDefinition]) = {
    debug(" -- process_datatypes --")
    val labelProperty = datatypeNode.property.reference()

    lUris.grouped(config.conf.settings.sizeBatchProcessing).toList.map(
      lSubUris => {
        trace( " datatypes:"+lSubUris.toString )
        /* request using api */
        SW(config).something("val_uri")
          .setList(lSubUris.map(_ match { case uri: URI => uri }))
          .setupnode(datatypeNode.property, false, false)
          .select(List("val_uri", labelProperty))
          .map(json => {
            qr.setDatatype(labelProperty, json("results")("bindings").arr.map(rec => {
              rec("val_uri")("value").value.toString -> rec(labelProperty)
            }).toMap)
          })
      })
  }

}
