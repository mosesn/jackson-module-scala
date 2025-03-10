package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.module.scala.deser.{ScalaNumberDeserializersModule, ScalaObjectDeserializerModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

/**
 * Complete module with support for all features.
 *
 * This class aggregates all of the feature modules into a single concrete class.
 * Its use is recommended for new users and users who want things to "just work".
 * If more customized support is desired, consult each of the constituent traits.
 *
 * @see [[com.fasterxml.jackson.module.scala.JacksonModule]]
 *
 * @since 1.9.0
 */
class DefaultScalaModule extends JacksonModule {
  override def getModuleName = "DefaultScalaModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    IteratorModule.getInitializers(config) ++
      EnumerationModule.getInitializers(config)  ++
      OptionModule.getInitializers(config) ++
      SeqModule.getInitializers(config) ++
      IterableModule.getInitializers(config) ++
      TupleModule.getInitializers(config) ++
      MapModule.getInitializers(config) ++
      SetModule.getInitializers(config) ++
      ScalaNumberDeserializersModule.getInitializers(config) ++
      ScalaObjectDeserializerModule.getInitializers(config) ++
      ScalaAnnotationIntrospectorModule.getInitializers(config) ++
      UntypedObjectDeserializerModule.getInitializers(config) ++
      EitherModule.getInitializers(config) ++
      SymbolModule.getInitializers(config)
  }
}

object DefaultScalaModule extends DefaultScalaModule
