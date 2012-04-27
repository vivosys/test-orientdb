package com.vivosys.test.persistence.ops.orientdb

import scala.collection.JavaConversions._

import org.scalatest._

import org.junit.runner.RunWith
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import java.util.Collection
import com.vivosys.test.persistence.ops.SpringScalaTest

@ContextConfiguration(locations = Array("classpath:com/vivosys/test/persistence/ops/orientdb/test-context.xml"))
@RunWith(classOf[JUnitRunner])
class KeyValueStoreOrientDbImplTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach
  with SpringScalaTest with OrientDbSugar {

  @Autowired
  var dbManager: DatabaseManager = _

  @Autowired
  var keyValueStoreImpl: KeyValueStoreOrientDbImpl = _

  override def afterEach() {
    cleanDb(dbManager, KeyValueStoreOrientDbImpl.COLLECTION)
  }

  test("given I store an entry, I can retrieve the same value") {

    keyValueStoreImpl.put("key1", "value1");
    keyValueStoreImpl.get("key1") should be("value1")

  }

  test("searching for a missing key returns null") {

    keyValueStoreImpl.get("key1") should be(null)

  }

  test("after removing a key, the key is not returned") {

    keyValueStoreImpl.put("key1", "value1");
    keyValueStoreImpl.put("key2", "value2");
    keyValueStoreImpl.remove("key1")
    keyValueStoreImpl.get("key1") should be(null)
    keyValueStoreImpl.get("key2") should be("value2")

  }

  test("removing a non-existing key is a no-op") {

    keyValueStoreImpl.remove("key1")
    keyValueStoreImpl.put("key1", "value1");
    keyValueStoreImpl.put("key2", "value2");
    keyValueStoreImpl.remove("key3")

    keyValueStoreImpl.get("key1") should be("value1")
    keyValueStoreImpl.get("key2") should be("value2")

  }

  test("multiple entries can be added") {

    keyValueStoreImpl.putAll(Map(
      "key1" -> "value1",
      "key2" -> "value2",
      "key3" -> "value3"
    ));
    keyValueStoreImpl.get("key1") should be("value1")
    keyValueStoreImpl.get("key2") should be("value2")
    keyValueStoreImpl.get("key3") should be("value3")

  }

  test("multiple keys can be removed at once") {

    keyValueStoreImpl.putAll(Map(
      "key1" -> "value1",
      "key2" -> "value2",
      "key3" -> "value3"
    ));
    keyValueStoreImpl.removeAll(List("key1", "key3"))
    keyValueStoreImpl.get("key1") should be(null)
    keyValueStoreImpl.get("key2") should be("value2")
    keyValueStoreImpl.get("key3") should be(null)

  }

  test("given I store a Long value, I get back a Long value") {

    keyValueStoreImpl.put("key1", 68l);
    var value: AnyRef = keyValueStoreImpl.get("key1")
    value.getClass should be(classOf[java.lang.Long])
    value should be(new java.lang.Long(68l))

  }

  test("given I store a Integer value, I get back an Integer value") {

    keyValueStoreImpl.put("key1", 67);

    var value: AnyRef = keyValueStoreImpl.get("key1")
    value should equal(67)

    value.getClass should be(classOf[java.lang.Integer])
    value should be(new java.lang.Integer(67))

  }

  test("given I store a Collection, I get back a Collection") {

    var list: Collection[String] = List("a", "b", "c")

    keyValueStoreImpl.put("key1", list);

    var value: AnyRef = keyValueStoreImpl.get("key1")
    value.isInstanceOf[Collection[String]] should be(true)
    value.asInstanceOf[Collection[String]] should be(list)

  }

  test("when I update a key value, I get back the new value") {

    keyValueStoreImpl.put("key1", "foo");
    keyValueStoreImpl.put("key1", "bar");

    keyValueStoreImpl.get("key1") should be("bar")

  }

}
