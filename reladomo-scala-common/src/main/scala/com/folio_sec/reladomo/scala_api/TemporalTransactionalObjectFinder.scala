/*
 * Copyright 2017 FOLIO Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.folio_sec.reladomo.scala_api

import com.folio_sec.reladomo.scala_api.aggregation.AggregateDataWrapper
import com.folio_sec.reladomo.scala_api.exception.ReladomoException
import com.gs.fw.common.mithra.attribute.{ Attribute, NumericAttribute }
import com.gs.fw.common.mithra.{ AggregateList, MithraDatedTransactionalList, MithraDatedTransactionalObject }
import com.gs.fw.finder.{ Navigation, OrderBy }

/**
  * A Scala wrapper interface of Reladomo transactional object finder utility.
  *
  * @tparam TxObject the type of Scala interface which holds an underlying Reladomo Java object.
  */
trait TemporalTransactionalObjectFinder[TxObject <: TemporalTransactionalObject,
                                        TxObjectList <: TemporalTransactionalList[TxObject, MithraTxObject],
                                        MithraTxObject <: MithraDatedTransactionalObject] { self =>

  @throws[ReladomoException]("findOne returned more than one result")
  def findOne(operation: FinderOperation): Option[TxObject]

  def findOneWith(
      operation: (self.type) => FinderOperation
  ): Option[TxObject] = {
    findOne(operation.apply(this))
  }

  @throws[ReladomoException]("findOne returned more than one result")
  def findOneBypassCache(operation: FinderOperation): Option[TxObject]

  def findOneBypassCacheWith(
      operation: (self.type) => FinderOperation
  ): Option[TxObject] = {
    findOneBypassCache(operation.apply(this))
  }

  // never throws exception because #findMany just returns List object
  def findMany(operation: FinderOperation): ListFinder[TxObject, TxObjectList, MithraTxObject]

  def findManyWith(
      operation: (self.type) => FinderOperation
  ): ListFinder[TxObject, TxObjectList, MithraTxObject] = {
    findMany(operation.apply(this))
  }

  // never throws exception because #findMany just returns List object
  def findManyBypassCache(
      operation: FinderOperation
  ): ListFinder[TxObject, TxObjectList, MithraTxObject]

  def findManyBypassCacheWith(
      operation: (self.type) => FinderOperation
  ): ListFinder[TxObject, TxObjectList, MithraTxObject] = {
    findManyBypassCache(operation.apply(this))
  }

  def aggregate(
      operation: com.gs.fw.common.mithra.finder.Operation
  ): AggregationBuilder[TxObject, TxObjectList, MithraTxObject] = {
    new AggregationBuilder[TxObject, TxObjectList, MithraTxObject](new AggregateList(operation))
  }

  def aggregateWith(
      operation: (self.type) => com.gs.fw.common.mithra.finder.Operation
  ): AggregationBuilder[TxObject, TxObjectList, MithraTxObject] = {
    new AggregationBuilder[TxObject, TxObjectList, MithraTxObject](new AggregateList(operation.apply(self)))
  }

  case class ListFinder[
      TxObject <: TemporalTransactionalObject,
      TxObjectList <: TemporalTransactionalList[TxObject, MithraTxObject],
      MithraTxObject <: MithraDatedTransactionalObject
  ](
      currentList: TxObjectList
  ) extends TemporalTransactionalList[TxObject, MithraTxObject] {

    override def underlying: MithraDatedTransactionalList[MithraTxObject] = currentList.underlying
    override def toScalaObject(mithraTxObject: MithraTxObject) =
      currentList.toScalaObject(mithraTxObject)
    override def newValueAppliers: Seq[() => Unit] = currentList.newValueAppliers

    // DomainList[MithraTxObject]

    override def count(): Int = currentList.count()
    override def length: Int  = count()

    override def deepFetch(
        navigation: Navigation[MithraTxObject]
    ): ListFinder.this.type = {
      ListFinder[TxObject, TxObjectList, MithraTxObject](currentList.deepFetch(navigation))
        .asInstanceOf[ListFinder.this.type]
    }
    def deepFetchWith(
        navigationProvider: (self.type) => Navigation[MithraTxObject]
    ): ListFinder.this.type = {
      ListFinder[TxObject, TxObjectList, MithraTxObject](currentList.deepFetch(navigationProvider.apply(self)))
        .asInstanceOf[ListFinder.this.type]
    }

    // NOTE: The reason we gave up to have the generic type here is
    // the List class code generated by MithraGenerator doesn't have the type...
    // (as of version 16.3.0)
    override def orderBy(orderBy: OrderBy[_]): ListFinder.this.type = {
      ListFinder[TxObject, TxObjectList, MithraTxObject](currentList.orderBy(orderBy))
        .asInstanceOf[ListFinder.this.type]
    }
    def orderByWith(
        orderByProvider: (self.type) => OrderBy[_]
    ): ListFinder[TxObject, TxObjectList, MithraTxObject] = {
      orderBy(orderByProvider.apply(self))
    }

    override def limit(size: Int): ListFinder.this.type = {
      ListFinder[TxObject, TxObjectList, MithraTxObject](currentList.limit(size))
        .asInstanceOf[ListFinder.this.type]
    }

    override def apply(idx: Int): TxObject    = currentList.apply(idx)
    override def iterator: Iterator[TxObject] = currentList.iterator

    override def updateAll()(implicit tx: Transaction): Unit    = currentList.updateAll()
    override def terminateAll()(implicit tx: Transaction): Unit = currentList.terminateAll()
    override def purgeAll()(implicit tx: Transaction): Unit     = currentList.purgeAll()

    //override def cascadeInsertAll()(implicit tx: Transaction): Unit = currentList.cascadeInsertAll()
    //override def bulkInsertAll()(implicit tx: Transaction): Unit    = currentList.bulkInsertAll()
    //override def insertAll()(implicit tx: Transaction): Unit        = currentList.insertAll()

  }

  case class AggregationBuilder[
      TxObject <: TemporalTransactionalObject,
      TxObjectList <: TemporalTransactionalList[TxObject, MithraTxObject],
      MithraTxObject <: MithraDatedTransactionalObject
  ](aggregation: AggregateList)
      extends Iterator[AggregateDataWrapper] {

    def groupBy(attribute: (self.type) => Attribute[MithraTxObject, _],
                name: String): AggregationBuilder[TxObject, TxObjectList, MithraTxObject] = {
      aggregation.addGroupBy(name, attribute.apply(self))
      this
    }

    def count(attribute: (self.type) => Attribute[MithraTxObject, _],
              name: String): AggregationBuilder[TxObject, TxObjectList, MithraTxObject] = {
      aggregation.addAggregateAttribute(name, attribute.apply(self).count())
      this
    }

    def max(attribute: (self.type) => NumericAttribute[MithraTxObject, _],
            name: String): AggregationBuilder[TxObject, TxObjectList, MithraTxObject] = {
      aggregation.addAggregateAttribute(name, attribute.apply(self).max())
      this
    }

    def sum(attribute: (self.type) => NumericAttribute[MithraTxObject, _],
            name: String): AggregationBuilder[TxObject, TxObjectList, MithraTxObject] = {
      aggregation.addAggregateAttribute(name, attribute.apply(self).sum())
      this
    }

    private[this] lazy val iterator = aggregation.iterator()

    override def hasNext: Boolean = iterator.hasNext

    override def next(): AggregateDataWrapper = AggregateDataWrapper(iterator.next)
  }

}
