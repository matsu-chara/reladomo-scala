package com.folio_sec.reladomo.scala_api.lib.task

import com.twitter.util.{ Await, Future }

trait Task[-Resource, +A] { self =>

  def action(resource: Resource): A

  /**
    * run action with runner
    */
  def run[SubResource <: Resource]()(implicit taskRunner: TaskRunner[SubResource]): Future[A] = {
    taskRunner.run(this)
  }

  def flatMap[SubResource <: Resource, B](f: A => Task[SubResource, B]): Task[SubResource, B] = {
    Task(r => f(self.action(r)).action(r))
  }

  def map[B](f: A => B): Task[Resource, B] = {
    Task(r => f(self.action(r)))
  }

  // twitter future inspired api
  def unit: Task[Resource, Unit] = {
    map(_ => ())
  }
}

object Task {
  def value[Resource, A](a: => A): Task[Resource, A] = {
    Task(_ => a)
  }

  def exception[Resource, A](e: Throwable): Task[Resource, A] = {
    Task(_ => throw e)
  }

  def apply[Resource, A](f: Resource => A): Task[Resource, A] = {
    new Task[Resource, A] { // no using SAM, for scala 2.11 compatibility
      override def action(resource: Resource): A = {
        f(resource)
      }
    }
  }

  def await[Resource, A](f: => Future[A]): Task[Resource, A] = {
    Task.value(Await.result(f))
  }

  // twitter future inspired api
  val Unit: Task[Any, Unit]            = value(())
  val None: Task[Any, Option[Nothing]] = value(Option.empty)
  val Nil: Task[Any, List[Nothing]]    = value(List.empty)
  val True: Task[Any, Boolean]         = value(true)
  val False: Task[Any, Boolean]        = value(false)

  val ??? : Task[Any, Nothing] = value(Predef.???)

  def traverseSequentially[Resource, A, B](as: Seq[A])(f: A => Task[Resource, B]): Task[Resource, Seq[B]] = {
    as.foldLeft(Task.value[Resource, Seq[B]](Seq())) {
      case (resultsTask, nextItem) =>
        for {
          results    <- resultsTask
          nextResult <- f(nextItem)
        } yield results :+ nextResult
    }
  }
}
