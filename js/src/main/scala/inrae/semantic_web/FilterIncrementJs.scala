package inrae.semantic_web

import inrae.semantic_web.rdf.{Literal, SparqlDefinition}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("SWFilterIncrement")
case class FilterIncrementJs(swf: SWDiscoveryJs,negation : Boolean = false) {

  @JSExport
  def isLiteral: SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.isLiteral)
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.isLiteral)
  }

  @JSExport
  def isUri: SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.isUri)
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.isUri)
  }

  @JSExport
  def isBlank: SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.isBlank)
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.isUri)
  }

  @JSExport
  def contains(l: SparqlDefinition): SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.contains(l))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.contains(l))
  }

  @JSExport
  def contains(l: String): SWDiscoveryJs = contains(Literal(l))

  @JSExport
  def strStarts( string : SparqlDefinition ) : SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.strStarts(string))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.strStarts(string))
  }

  @JSExport
  def strStarts(string : String) : SWDiscoveryJs = strStarts(string)

  @JSExport
  def strEnds( string : SparqlDefinition ) : SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.strEnds(string))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.strEnds(string))
  }

  @JSExport
  def strEnds(string : String) : SWDiscoveryJs = strEnds(string)

  @JSExport
  def equal( value : SparqlDefinition ) : SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.equal(value))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.equal(value))
  }

  @JSExport
  def notEqual( value : SparqlDefinition ) : SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.notEqual(value))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.notEqual(value))
  }

  @JSExport
  def inf( value : SparqlDefinition ) : SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.inf(value))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.inf(value))
  }

  @JSExport
  def infEqual( value : SparqlDefinition ) : SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.infEqual(value))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.infEqual(value))
  }

  @JSExport
  def sup( value : SparqlDefinition ) : SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.sup(value))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.sup(value))
  }

  @JSExport
  def supEqual( value : SparqlDefinition ) : SWDiscoveryJs = negation match {
    case true => SWDiscoveryJs(swf.config,swf.sw.filter.not.supEqual(value))
    case false => SWDiscoveryJs(swf.config,swf.sw.filter.supEqual(value))
  }

  @JSExport
  def not: FilterIncrementJs = FilterIncrementJs(swf,!negation)

  //swf.sw.filter.not
}
