/*
 * Copyright (c) 2022, Valaphee.
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

package com.valaphee.protod

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.UnknownFieldSet
import java.io.PrintWriter

/**
 * @author Kevin Ludwig
 */
class ProtoWriter(
    private val printWriter: PrintWriter,
    private val messages: MutableMap<String, DescriptorProtos.DescriptorProto>,
    private val messageExtensions: MutableMap<String, out Map<Int, DescriptorProtos.FieldDescriptorProto>>
) {
    private var first = false
    private var indentLevel = 0

    fun print(file: DescriptorProtos.FileDescriptorProto) {
        println("""syntax = "proto2";""")
        println()
        println("""package ${file.`package`};""")
        printImports(file.dependencyList)
        printOptions(file.options)
        printMessages(file.messageTypeList)
        printServices(file.serviceList)
    }

    private fun println() {
        repeat(indentLevel) { printWriter.print("  ") }
        printWriter.println()
    }

    private fun println(value: String) {
        repeat(indentLevel) { printWriter.print("  ") }
        printWriter.println(value)
    }

    private fun printImports(dependencyList: List<String>) {
        if (dependencyList.isNotEmpty()) if (first) first = false else println()
        dependencyList.forEach { println("""import "$it";""") }
    }

    private fun printOptions(options: GeneratedMessageV3.ExtendableMessage<*>) {
        if (options.allFields.isNotEmpty() || options.unknownFields.asMap().isNotEmpty()) if (first) first = false else println()
        options.allFields.forEach {
            println("""option ${it.key.name} = ${when (it.value) {
                is String -> """"${it.value}""""
                else -> it.value
            }};""")
        }
        val messageExtensions = messageExtensions[".${options.descriptorForType.fullName}"] ?: emptyMap()
        options.unknownFields.asMap().forEach { unknownField -> messageExtensions[unknownField.key]?.let { messageExtension -> messages[messageExtension.typeName]?.fieldList?.let { messageExtensionFields -> unknownField.value.lengthDelimitedList.forEach { UnknownFieldSet.parseFrom(it).asMap().forEach { messageExtensionField -> println("""option (${messageExtension.name}).${messageExtensionFields.single { it.number == messageExtensionField.key}?.name} = ${messageExtensionField.value.varintList.firstOrNull() ?: messageExtensionField.value.fixed32List.firstOrNull() ?: messageExtensionField.value.fixed64List.firstOrNull() ?: messageExtensionField.value.lengthDelimitedList.firstOrNull()?.toStringUtf8()?.let { """"$it"""" } ?: TODO()};""") } } } } }
    }

    private fun printMessages(messageTypeList: List<DescriptorProtos.DescriptorProto>) {
        var first0 = first
        messageTypeList.forEach {
            if (first0) first0 = false else println()
            println("""message ${it.name} {""")
            first = true
            indentLevel++
            printFields(it.fieldList)
            first = first0
            indentLevel--
            println("""}""")
        }
    }

    private fun printFields(fieldList: List<DescriptorProtos.FieldDescriptorProto>) {
        if (fieldList.isNotEmpty()) if (first) first = false else println()
        fieldList.forEach { println("""${checkNotNull(fieldLabels[it.label])} ${if (it.hasTypeName()) it.typeName else checkNotNull(fieldTypes[it.type])} ${it.name} = ${it.number};""") }
    }

    private fun printServices(serviceList: List<DescriptorProtos.ServiceDescriptorProto>) {
        var first0 = first
        serviceList.forEach {
            if (first0) first0 = false else println()
            println("""service ${it.name} {""")
            first = true
            indentLevel++
            printOptions(it.options)
            printMethods(it.methodList)
            first = first0
            indentLevel--
            println("""}""")
        }
    }

    private fun printMethods(methodList: List<DescriptorProtos.MethodDescriptorProto>) {
        var first0 = first
        methodList.forEach {
            if (first0) first0 = false else println()
            println("""rpc ${it.name} (${it.inputType}) returns (${it.outputType}) {""")
            first = true
            indentLevel++
            printOptions(it.options)
            first = first0
            indentLevel--
            println("""}""")
        }
    }

    companion object {
        private val fieldLabels = mapOf(
            DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL to "optional",
            DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED to "required",
            DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED to "repeated"
        )

        private val fieldTypes = mapOf(
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE to "double",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT to "float",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64 to "int64",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64 to "uint64",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32 to "int32",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64 to "fixed64",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32 to "fixed32",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL to "bool",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING to "string",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES to "bytes",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32 to "uint32",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32 to "sfixed32",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64 to "sfixed64",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32 to "sint32",
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64 to "sint64",
        )
    }
}
