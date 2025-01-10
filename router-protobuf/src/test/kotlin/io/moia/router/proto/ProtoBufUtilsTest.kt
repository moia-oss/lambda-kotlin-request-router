package io.moia.router.proto

import com.google.protobuf.StringValue
import com.jayway.jsonpath.JsonPath
import io.moia.router.proto.sample.SampleOuterClass.ComplexSample
import io.moia.router.proto.sample.SampleOuterClass.ComplexSample.SampleEnum.ONE
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class ProtoBufUtilsTest {
    @Test
    fun `should serialize empty list`() {
        val message =
            ComplexSample
                .newBuilder()
                .addAllSamples(emptyList())
                .build()

        val json = ProtoBufUtils.toJsonWithoutWrappers(message)

        then(JsonPath.read<List<Any>>(json, "samples")).isEmpty()
    }

    @Test
    fun `should remove wrapper object`() {
        val message =
            ComplexSample
                .newBuilder()
                .setSomeString(StringValue.newBuilder().setValue("some").build())
                .build()

        val json = ProtoBufUtils.toJsonWithoutWrappers(message)

        then(JsonPath.read<String>(json, "someString")).isEqualTo("some")
    }

    @Test
    fun `should serialize value when it is the default`() {
        val message =
            ComplexSample
                .newBuilder()
                .setEnumAttribute(ONE) // enum zero value
                .build()

        val json = ProtoBufUtils.toJsonWithoutWrappers(message)

        then(JsonPath.read<String>(json, "enumAttribute")).isEqualTo("ONE")
    }
}
