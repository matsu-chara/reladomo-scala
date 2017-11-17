package com.folio_sec.reladomo.scala_api.lib.task

import com.twitter.util.Future

trait TaskRunner[Resource] {
  def run[A](task: Task[Resource, A]): Future[A]
}

// object SampleTaskRunner {
//  val reladomoFuturePool: FuturePool = FuturePool(new ForkJoinPool(50))
//
//  implicit val taskRunner: TaskRunner[MithraTransaction] = new TaskRunner[MithraTransaction] {
//    override def run[A](task: Task[MithraTransaction, A]): Future[A] = {
//      reladomoFuturePool {
//        TransactionProvider.withTransaction { txn =>
//          task.action(txn)
//        }
//      }
//    }
//  }
//}
