package models

import protobuf.Main._
import org.scalatest._
import org.scalatest.GivenWhenThen._
import org.scalatest.matchers.ShouldMatchers._
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._

class BigTableSpec extends FlatSpec {

  "Putting bytes" should "return ok" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      val bytes: Array[Byte] = "Hello World".getBytes
      Bigtable.put("row", "column", bytes)
      Bigtable.get("row", "column").get
    }
  }

  "Putting protobuf" should "deserizlize ok" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      val buffer0 = Index.newBuilder.setCard("Hello").build().toByteArray()
      Bigtable.put("index0", "index", buffer0)
      val buffer1 = Bigtable.get("index0", "index").get
      Index.parseFrom(buffer1).getCard should equal("Hello")
    }

  }
  
  "Putting multiple items" should "return latest" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      val buffer0 = Index.newBuilder.setCard("Hello").build().toByteArray()
      val buffer1 = Index.newBuilder.setCard("Goodbye").build().toByteArray()
      Bigtable.put("index", "index", buffer0)
      Bigtable.put("index", "index", buffer1)
      
      val bufferX = Bigtable.get("index0", "index").get
      Index.parseFrom(buffer1).getCard should equal("Goodbye")
    }
  }

}
