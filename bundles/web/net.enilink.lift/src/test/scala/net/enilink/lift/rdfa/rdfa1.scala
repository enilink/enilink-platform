package net.enilink.lift.rdfa

import org.scalatest._
import net.enilink.lift.rdf._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RDFaMiscTestSpecs extends FlatSpec {
  val s = new net.enilink.lift.rdf.Scope(Nil)
  "XML variable scope" should "make distinct fresh vars" in {
    assert(s.fresh("x").qual != s.fresh("x").qual)
  }
  it should "should find vars by name" in {
    assert(s.byName("x") == s.byName("x"))
  }

  "RDFa walker" should "should stop chaining on bogus rel values (Test #105) " in {
    val e1 = <div xmlns:dc="http://purl.org/dc/elements/1.1/" about="" rel="dc:creator">
               <a rel="myfoobarrel" href="ben.html">Ben</a>
               created this page.
             </div>

    var addr = "data:"
    val undef = RDFaParser.undef
    var (e, arcs) = RDFaParser.walk(e1, addr, Label(addr), undef, Nil, Nil, null)
    assert(arcs.force.head match {
      case (Label(_), Label(_), Variable(_, _)) => true
      case _ => false
    })
  }
}
