package com.folio_sec.reladomo.scala_api.lib.task

import com.twitter.conversions.time._
import com.twitter.util.{ Await, Future }

import scala.util.control.NonFatal
import scalaprops.Gen._
import scalaprops._
import scalaz._

object TaskLaws extends Scalaprops {

  implicit def genTask[R, A](implicit gen: Gen[A]): Gen[Task[R, A]] = gen.map(x => Task.value(x))

  implicit def genTaskRunner[R](implicit gen: Gen[R]): Gen[TaskRunner[R]] = {
    gen.map { resource =>
      new TaskRunner[R] {
        override def run[A](task: Task[R, A]): Future[A] = Future.value(task.action(resource))
      }
    }
  }

  implicit val intEqual: Equal[Int] = Equal.equal(_ == _)

  implicit def equalFuture[A](implicit A: Equal[A]): Equal[Future[A]] = {
    Equal.equal(
      (a, b) =>
        try {
          Await.result(a.join(b).map(x => A.equal(x._1, x._2)), 1.seconds)
        } catch {
          case NonFatal(e) => false
      }
    )
  }

  implicit def equalTask[R, A](implicit F: Equal[Future[A]], R: Gen[TaskRunner[R]]): Equal[Task[R, A]] = {
    F.contramap(_.run()(R.sample()))
  }

  implicit def monadTask[R, A] = new Monad[({ type L[B] = Task[R, B] })#L] {
    def point[B](a: => B): Task[R, B]                             = Task.value(a)
    def bind[B, C](a: Task[R, B])(f: B => Task[R, C]): Task[R, C] = a.flatMap(f)
  }

  type IntTask[A] = Task[Int, A]
  val monadLawsTest = Properties.list(
    scalazlaws.monad.all[IntTask]
  )
}
