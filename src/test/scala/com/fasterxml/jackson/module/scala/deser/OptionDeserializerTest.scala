package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.DefaultTyping
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.annotation.meta.field
import scala.compat.java8.FunctionConverters

case class UnavailableField(foo: Option[String])

case class JavaOptionalWrapper(o: java.util.Optional[String])
case class OptionWrapper(o: Option[String])

object OptionDeserializerTest {
  @JsonSubTypes(Array(new JsonSubTypes.Type(classOf[Impl])))
  trait Base

  @JsonTypeName("impl")
  case class Impl() extends Base

  case class BaseHolder(
    private var _base: Option[Base]
  ) {
    @(JsonTypeInfo @field)(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="$type")
    def base: Option[Base] = _base
    def base_=(base:Option[Base]): Unit = { _base = base }
  }

  case class Defaulted(id: Int, name: String = "") {
    def this() = this(1,"")
  }

  case class Foo(bar: String)
  case class Wrapper[T](t: T)

  trait Marker
  case class AMarker(a: Int) extends Marker
  case class XMarker(x: Option[Marker])
}

class OptionDeserializerTest extends DeserializerTest {
  import OptionDeserializerTest._
  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with OptionDeserializer" should "deserialize an Option[Int]" in {
    deserialize("1", classOf[Option[Int]]) should be (Some(1))
    deserialize("1", classOf[Option[Int]]) should be (Option(1))
    deserialize("null", classOf[Option[Int]]) should be (None)
  }

  it should "deserialize an Option[String]" in {
    deserialize("\"foo\"", classOf[Option[String]]) should be (Some("foo"))
    deserialize("\"foo\"", classOf[Option[String]]) should be (Option("foo"))
    deserialize("null", classOf[Option[String]]) should be (None)
  }

  it should "deserialize an Option[Long] to a long" in {
    deserialize("123456789012345678", classOf[Option[Long]]) should be (Some(123456789012345678L))
    deserialize("123456789012345678", classOf[Option[Long]]).map(java.lang.Long.valueOf(_)) should be (Some(123456789012345678L))
    deserialize("123456789012345678", classOf[Option[Long]]).get.getClass should be (classOf[Long])

    deserialize("1", classOf[Option[Long]], classOf[Long]) should be (Some(1L))
    deserialize("1", classOf[Option[Long]], classOf[Long]).map(java.lang.Long.valueOf(_)) should be (Some(1L))
    deserialize("1", classOf[Option[Long]], classOf[Long]).get.getClass should be (classOf[Long])
  }

  it should "synthesize None for optional fields that are non-existent" in {
    deserialize("{}", classOf[UnavailableField]) should be(UnavailableField(None))
  }

  it should "propagate type information" in {
    val json: String = """{"base":{"$type":"impl"}}"""
    deserialize(json, classOf[BaseHolder]) should be(BaseHolder(Some(Impl())))
  }

  it should "deserialize a polymorphic null as None" in {
    deserialize("""{"base":null}""", classOf[BaseHolder]) should be(BaseHolder(None))
  }

  it should "deserialize defaulted parameters correctly (without defaults)" in {
    val json = newMapper.writeValueAsString(Defaulted(id = 1))
    json shouldBe """{"id":1,"name":""}"""
    val d = deserialize(json, classOf[Defaulted])
    d.name should not be null
  }

  it should "deserialize a type param wrapped option" in {
    val json: String = """{"t": {"bar": "baz"}}"""
    val result = deserialize(json, new TypeReference[Wrapper[Option[Foo]]] {})
    result.t.get.isInstanceOf[Foo] should be(true)
  }

  it should "handle conversion of null to empty collection" in {
    val mapper = newBuilder
      .changeDefaultNullHandling(FunctionConverters.asJavaUnaryOperator(_ => JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY)))
      .build()
    val json = """{"o": null}"""
    val result1 = mapper.readValue(json, classOf[JavaOptionalWrapper])
    result1 shouldEqual JavaOptionalWrapper(java.util.Optional.empty[String]())
    val result2 = mapper.readValue(json, classOf[OptionWrapper])
    result2 shouldEqual OptionWrapper(None)
  }

  //https://github.com/FasterXML/jackson-module-scala/issues/382
  it should "handle generics" in {
    val builder = newBuilder
    val polymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
      .allowIfBaseType(classOf[Any])
      .build()
    builder.activateDefaultTypingAsProperty(polymorphicTypeValidator, DefaultTyping.OBJECT_AND_NON_CONCRETE, "_t")
    val mapper = builder.build()
    val v = XMarker(Some(AMarker(1)))
    val str = mapper.writeValueAsString(v)
    val v2 = mapper.readValue(str, classOf[XMarker])
    v2 shouldEqual v
  }
}
