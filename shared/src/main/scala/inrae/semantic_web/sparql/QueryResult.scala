package inrae.semantic_web.sparql

import inrae.semantic_web.rdf.{SparqlBuilder, SparqlDefinition}
import wvlet.log.Logger.rootLogger.trace

import scala.util.{Failure, Success, Try}

case class QueryResult(results: String, mimetype : String = "json") {

  var json =
    Try(ujson.read(results)) match {
      case Success(json) => json
      case Failure(_) => ujson.Obj(
        "head" -> ujson.Obj(
          "link" -> ujson.Arr(),
          "vars" -> ujson.Arr()
        ),
        "results" -> ujson.Obj(
          "distinct" -> "false",
          "ordered" -> "true",
          "bindings" -> ujson.Arr()
        )
      )
    }

  /**
   * replace all variable name by alias used by the user
   * @param v2k
   * @return
   */
  def v2Ident(v2k : Map[String,String]) = {
    trace(v2k.toString)
    val l = json("head")("vars").arr.map(v => {
      val v2 = v.toString().replace("\"","")
      v2k.find( v2 == _._2 ).map( x => x._1 ) match {
        case Some(s) => s
        case None => v.toString().replace("\"","")
      }})

    json("head")("vars").arr.clear()
    //json("head")("vars").arr.addAll(l.toArray)
    l.map( {
      case a : String => json("head")("vars").arr.append(a)
      case _ => Nil
    })

    val records = json("results")("bindings").arr.map(kv => kv match {
      case o: ujson.Obj => o.obj.map(
        kv2 => {
            v2k.find( _._2 == kv2._1).map( _._1 ) match {
              case Some(s) => (s,kv2._2)
              case _ => (kv2._1,kv2._2)
            }
        })
      case _ => Nil
    })
    json("results")("bindings").arr.clear()
    records.map( r => json("results")("bindings").arr.append(r) )
  }

  /* get column results */
  def getValues( key : String ): Seq[SparqlDefinition] = {
    json("results")("bindings").arr.flatMap(kv => kv match {
      case o: ujson.Obj => {
        Some(SparqlBuilder.create(o(key)))
      }
      case _ => None
    }).toSeq
  }

  def setDatatype( key : String , uri_values : Map[String,ujson.Value] ): Unit = {
    val datatype = json("results").obj.getOrElse("datatypes",ujson.Obj())
    val keyObjet = datatype.obj.getOrElse(key,ujson.Obj())

    uri_values.foreach( {
      case (subkey, value) => {
        val subkeyObjet = keyObjet.obj.getOrElse(subkey,ujson.Arr())
        subkeyObjet.arr.append(value)
        keyObjet.obj.update(subkey,subkeyObjet)
      }
    })

    datatype.obj.update(key,keyObjet)
    json("results").update("datatypes",datatype)
  }
}