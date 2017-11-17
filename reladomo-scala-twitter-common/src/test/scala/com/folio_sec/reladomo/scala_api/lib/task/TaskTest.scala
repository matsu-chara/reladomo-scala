package com.folio_sec.reladomo.scala_api.lib.task

import java.util.concurrent.ForkJoinPool

import com.folio_sec.reladomo.scala_api.TransactionProvider
import com.folio_sec.reladomo.scala_api.configuration.DatabaseManager
import com.gs.fw.common.mithra.MithraTransaction
import com.twitter.util.{ Await, Future, FuturePool }
import example.{ NewPerson, PersonFinder }
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }

class TaskTest extends FlatSpec with Matchers with BeforeAndAfterEach {

  def initializePeopleDatabase() = {
    import scalikejdbc._
    Class.forName("org.h2.Driver")
    ConnectionPool.add('people, "jdbc:h2:mem:people;MODE=MySQL", "user", "pass")
    implicit val session = NamedAutoSession('people)
    sql"""
drop table PERSON if exists;
create table PERSON
(
    PERSON_ID int not null,
    FIRST_NAME varchar(64) not null,
    LAST_NAME varchar(64) not null,
    COUNTRY varchar(48) not null
);
alter table PERSON add constraint PERSON_PK primary key (PERSON_ID);
""".execute.apply()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    initializePeopleDatabase()
    DatabaseManager.loadRuntimeConfig("reladomo/ReladomoRuntimeConfig.xml")
  }

  it should "work" in {
    import TaskTest.taskRunner

    val taskInsert = Task { implicit txn: MithraTransaction =>
      NewPerson("Taro", "Yamada", "Japan").insert()
    }

    val taskFind = Task { implicit txn: MithraTransaction =>
      val people = PersonFinder.findManyWith(_.all)
      people.count()
    }

    val f = for {
      _     <- taskInsert
      count <- taskFind
      pure  <- Task.value(2)
      future <- Task.await {
        FuturePool.unboundedPool {
          3
        }
      }
    } yield count + pure + future

    val result = Await.result(f.run())
    assert(result == 6)
  }

  it should "work in same thread" in {
    import TaskTest.taskRunner

    val taskInsert = Task { implicit txn: MithraTransaction =>
      NewPerson("Taro", "Yamada", "Japan").insert()
      Thread.currentThread().getId
    }

    val taskFind = Task { implicit txn: MithraTransaction =>
      PersonFinder.findManyWith(_.all).count()
      Thread.currentThread().getId
    }

    val f = for {
      tid1 <- taskInsert
      tid2 <- Task.await {
        FuturePool.unboundedPool {
          Thread.sleep(500)
          Thread.currentThread().getId
        } // run in another thread
      }
      tid3 <- taskFind
      tid4 <- Task.value {
        Thread.currentThread().getId
      }
      tid5 <- Task.await {
        FuturePool.unboundedPool {
          // run in another thread
          Thread.currentThread().getId
        }
      }
    } yield {
      // run reladomo or value task in same thread
      assert(tid1 == tid3)
      assert(tid1 == tid4)

      // run future task in another thread
      assert(tid1 != tid2)
      assert(tid1 != tid5)
    }

    Await.result(f.run())
  }
}

object TaskTest {
  val reladomoFuturePool: FuturePool = FuturePool(new ForkJoinPool(50))

  implicit val taskRunner: TaskRunner[MithraTransaction] = new TaskRunner[MithraTransaction] {
    override def run[A](task: Task[MithraTransaction, A]): Future[A] = {
      reladomoFuturePool {
        TransactionProvider.withTransaction { txn =>
          task.action(txn)
        }
      }
    }
  }
}
