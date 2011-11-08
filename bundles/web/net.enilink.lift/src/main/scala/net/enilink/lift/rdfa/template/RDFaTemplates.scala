package net.enilink.lift.rdfa.template

import scala.collection.mutable
import scala.xml.Elem
import scala.xml.NodeSeq
import net.enilink.komma.core.IBindings
import net.enilink.komma.core.IReference
import net.enilink.lift.snippet.CurrentContext
import net.enilink.lift.snippet.RdfContext
import net.liftweb.common.Full
import net.liftweb.http.PageName
import net.liftweb.http.S
import net.liftweb.util.Helpers.pairToUnprefixed
import net.liftweb.util.Helpers.strToSuperArrowAssoc
import scala.xml.UnprefixedAttribute

trait RDFaTemplates {
  /**
   * Process lift templates while changing the RDFa context resources
   */
  def processSurroundAndInclude(ns: NodeSeq): NodeSeq = {
    def processNode(n: xml.Node): NodeSeq = {
      n match {
        case e: ElemWithRdfa => {
          val result = e.copy(child = processSurroundAndInclude(e.child))
          CurrentContext.withValue(Full(e.context)) {
            S.session.get.processSurroundAndInclude(PageName.get, result)
          }
        }
        case e: Elem => e.copy(child = processSurroundAndInclude(e.child))
        case other => other
      }
    }

    ns.flatMap(processNode _)
  }

  private val attNames = Set("about", "src", "rel", "rev", "property", "href", "resource", "content", "clear-content")
  private val Variable = "^\\?(.*)".r

  // isTemplate is required for disambiguation because xml.Node extends Seq[xml.Node]
  class Key(val ctxs: Seq[RdfContext], val nodeOrNodeSeq: AnyRef, val isTemplate : Boolean = false) {
    val hashCodeVal = 41 * System.identityHashCode(nodeOrNodeSeq) + ctxs.hashCode + (if (isTemplate) 1 else 0) 
   
    override def hashCode = hashCodeVal

    override def equals(other: Any) = {
      other match {
        case k: Key => (nodeOrNodeSeq eq k.nodeOrNodeSeq) && isTemplate == k.isTemplate && ctxs == k.ctxs
        case _ => false
      }
    }
  }

  class ReplacementMap extends mutable.HashMap[xml.Node, xml.Node] {
    override def elemHashCode(e: xml.Node) = System.identityHashCode(e)
    override def elemEquals(a: xml.Node, b: xml.Node) = a eq b
  }

  def transform(ctx: RdfContext, template: Seq[xml.Node])(implicit bindings: IBindings[_], existing: mutable.Map[Key, Seq[xml.Node]]): Seq[xml.Node] = {
    def internalTransform(ctxs: Seq[RdfContext], template: Seq[xml.Node]): Seq[xml.Node] = {
      var replacedNodes: mutable.Map[xml.Node, xml.Node] = null

      val ctx = ctxs.last
      val newNodesForTemplate = template.flatMap(tNode => {
        var currentCtx = ctx

        tNode match {
          case tElem: Elem => {
            val result = if (tElem.attributes.isEmpty) tElem.asInstanceOf[Elem] else {
              var attributes = tElem.attributes
              tElem.attributes.foreach(meta =>
                if (!meta.isPrefixed && attNames.contains(meta.key)) {
                  meta.value.text match {
                    case Variable(v) => {
                      val rdfValue = if (v == "this") ctx.subject else bindings.get(v)

                      if (rdfValue != null) {
                        // check if context needs to be changed for children
                        meta.key match {
                          case "rel" | "rev" | "property" =>
                            currentCtx = new RdfContext(currentCtx.subject, rdfValue)
                          case _ =>
                            currentCtx = new RdfContext(rdfValue, currentCtx.predicate)
                        }
                      }

                      val attValue = rdfValue match {
                        case ref: IReference => {
                          val uri = ref.getURI()
                          if (uri == null) ref else {
                            val namespace = uri.namespace.toString
                            lazy val uriStr = uri.toString
                            val prefix = tElem.scope.getPrefix(namespace)
                            if (prefix == null) uri else prefix + ":" + uriStr.substring(Math.min(namespace.length, uriStr.length))
                          }
                        }
                        case other => other
                      }

                      if (attValue == null) attributes = null
                      else if (meta.key.startsWith("clear-")) attributes = attributes.remove(meta.key)
                      else attributes = attributes.append(new UnprefixedAttribute(meta.key, attValue.toString, meta.next))
                    }
                    case _ =>
                  }
                })
              if (attributes == null) null else tElem.asInstanceOf[Elem].copy(attributes = attributes)
            }

            val currentCtxs = if (currentCtx == ctx) ctxs else ctxs ++ List(currentCtx)

            var newNodes: Seq[xml.Node] = Nil
            val key = new Key(currentCtxs, tNode)
            val nodesForContext = existing.get(key) match {
              case Some(nodes) => {
                if (replacedNodes == null) replacedNodes = new ReplacementMap
                nodes.map(
                  _ match {
                    case e: Elem => {
                      var newE = e.copy(child = internalTransform(currentCtxs, tElem.child))
                      replacedNodes.put(e, newE)
                      newE
                    }
                    case other => other
                  })
              }
              case None => if (result == null) Nil else {
                // create new node with transformed children
                val newChild = internalTransform(currentCtxs, tElem.child)
                if (currentCtx == ctx) {
                  newNodes = result.copy(child = newChild)
                } else {
                  newNodes = new ElemWithRdfa(currentCtx, result.prefix, result.label, result.attributes,
                    result.scope, newChild: _*)
                }
                newNodes
              }
            }

            existing(key) = nodesForContext
            newNodes
          }
          case other => existing.get(new Key(ctxs, tNode)) match {
            case Some(nodes) => Nil
            case None => {
              existing(new Key(ctxs, tNode)) = other
              other
            }
          }
        }
      })

      val key = new Key(ctxs, template, true)
      val nodesForTemplate = existing.getOrElse(key, Nil).map {
        n =>
          (if (replacedNodes == null) None else replacedNodes.get(n)) match {
            case Some(replacement) => replacement
            case None => n
          }
      } ++ newNodesForTemplate
      existing(key) = nodesForTemplate

      nodesForTemplate
    }

    internalTransform(List(ctx), template)
  }
}