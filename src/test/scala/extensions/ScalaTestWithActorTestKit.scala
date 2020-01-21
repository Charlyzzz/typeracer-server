package extensions

/*
 * Copyright (C) 2018-2019 Lightbend Inc. <https://www.lightbend.com>
 */

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ActorTestKitBase}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, TestSuite}

/**
 * A ScalaTest base class for the [[ActorTestKit]], making it possible to have ScalaTest manage the lifecycle of the testkit.
 * The testkit will be automatically shut down when the test completes or fails using ScalaTest's BeforeAndAfterAll trait. If
 * a spec overrides afterAll it must call super.afterAll.
 *
 * Note that ScalaTest is not provided as a transitive dependency of the testkit module but must be added explicitly
 * to your project to use this.
 */
abstract class ScalaTestWithActorTestKit(testKit: ActorTestKit)
    extends ActorTestKitBase(testKit)
    with TestSuite
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  def this() = this(ActorTestKit(ActorTestKitBase.testNameFromCallStack()))

  /**
   * Use a custom config for the actor system.
   */
  def this(config: String) =
    this(ActorTestKit(ActorTestKitBase.testNameFromCallStack(), ConfigFactory.parseString(config)))

  /**
   * Use a custom config for the actor system.
   */
  def this(config: Config) = this(ActorTestKit(ActorTestKitBase.testNameFromCallStack(), config))

  /**
   * Use a custom config for the actor system, and a custom [[akka.actor.testkit.typed.TestKitSettings]].
   */
  def this(config: Config, settings: TestKitSettings) =
    this(ActorTestKit(ActorTestKitBase.testNameFromCallStack(), config, settings))

  /**
   * `PatienceConfig` from [[akka.actor.testkit.typed.TestKitSettings#DefaultTimeout]]
   */
  implicit val patience: PatienceConfig =
    PatienceConfig(testKit.testKitSettings.DefaultTimeout.duration, Span(100, org.scalatest.time.Millis))

  /**
   * Shuts down the ActorTestKit. If override be sure to call super.afterAll
   * or shut down the testkit explicitly with `testKit.shutdownTestKit()`.
   */
  override protected def afterAll(): Unit = {
    super.afterAll()
    testKit.shutdownTestKit()
  }
}
