package com.softwaremill.macwire.dependencyLookup

import com.softwaremill.macwire.Logger
import com.softwaremill.macwire.Util._
import com.softwaremill.macwire.dependencyLookup.EligibleValuesFinder.Scope.LocalForward

import scala.reflect.macros.blackbox

private[macwire] class DependencyResolver[C <: blackbox.Context](val c: C, debug: Logger) {

  import c.universe._

  private val eligibleValuesFinder = new EligibleValuesFinder[c.type](c, debug)

  private lazy val eligibleValues = eligibleValuesFinder.find()

  /** Look for a single instance of type `t`.
    * If either no instance or multiple instances are found,
    * a compilation error is reported and `None` is returned.
    */
  def resolve(param: Symbol, t: Type): Option[Tree] = {

    eligibleValues.findInFirstScope(t).toList match {
      case Nil =>
        c.error(c.enclosingPosition, s"Cannot find a value of type: [$t]")
        None

      case value :: Nil =>
        val forwardValues = eligibleValues.findInScope(t, LocalForward)
        if (forwardValues.nonEmpty) {
          c.warning(c.enclosingPosition, s"Found [$value] for parameter [${param.name}], " +
            s"but a forward reference [${forwardValues.mkString(", ")}] was also eligible")
        }
        Some(value)

      case values =>
        c.error(c.enclosingPosition, s"Found multiple values of type [$t]: [$values]")
        None
    }
  }

  /** @return all the instances of type `t` that are accessible.
    */
  def resolveAll(t: Type): Set[Tree] = {
    eligibleValues.findInAllScope(t)
  }
}
