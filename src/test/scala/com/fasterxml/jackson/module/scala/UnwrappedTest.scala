package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.annotation.{JsonProperty, JsonIgnore}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import com.fasterxml.jackson.databind.ObjectMapper


case class Address(address1: Option[String], city: Option[String], state: Option[String])
case class Person(name: String, @JsonIgnore location: Address, alias: Option[String])
{
  private def this() = this("", Address(None, None, None), None)

  def address1 = location.address1
  private def address1_=(value: Option[String]) {
    setAddressField("address1", value)
  }

  def city = location.city
  private def city_=(value: Option[String]) {
    setAddressField("city", value)
  }

  def state = location.state
  private def state_= (value: Option[String]) {
    setAddressField("state", value)
  }

  private def setAddressField(name: String, value: Option[String])
  {
    val f = location.getClass.getDeclaredField(name)
    f.setAccessible(true)
    f.set(location, value)
  }
}

@RunWith(classOf[JUnitRunner])
class UnwrappedTest extends FlatSpec with ShouldMatchers {

  "mapper" should "do stuff" in {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val p = Person("Snoopy", Address(Some("123 Main St"), Some("Anytown"), Some("WA")), Some("Joe Cool"))
    val json = mapper.writeValueAsString(p)

    json should be === ("""{"name":"Snoopy","alias":"Joe Cool","city":"Anytown","address1":"123 Main St","state":"WA"}""")

    val p2 = mapper.readValue(json, classOf[Person])

    p2 should be === p
  }

}
