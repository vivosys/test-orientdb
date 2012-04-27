package com.vivosys.test.persistence.ops

import org.scalatest.{Suite, BeforeAndAfterAll}
import org.springframework.test.context.{TestContextManager}


/**
 * This trait lets us use the @AutoWired annotation with our ScalaTests, so they get injected correctly.
 */
trait SpringScalaTest extends BeforeAndAfterAll {

  // This construct (self type) states that this trait must only be mixed into subclasses of Suite
  this: Suite =>

  override def beforeAll() {

    new TestContextManager(this.getClass()).prepareTestInstance(this)

  }

}