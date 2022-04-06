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
import com.google.protobuf.WireFormat
import java.io.PrintWriter

/**
 * @author Kevin Ludwig
 */
class ProtoWriter(
    private val printWriter: PrintWriter,
    private val enums: MutableMap<String, DescriptorProtos.EnumDescriptorProto>,
    private val messages: MutableMap<String, DescriptorProtos.DescriptorProto>,
    private val messageExtensions: MutableMap<String, out Map<Int, DescriptorProtos.FieldDescriptorProto>>
) {
    private var first = false
    private var indentLevel = 0

    fun print(file: DescriptorProtos.FileDescriptorProto) {
        println("syntax = \"proto2\";")
        if (file.hasPackage()) {
            println()
            println("package ${file.`package`};")
        }

        printImports(file.dependencyList, file.publicDependencyList)
        println()
        if (!file.options.hasJavaMultipleFiles()) println("option java_multiple_files = true;")
        if (!file.options.hasJavaGenericServices() && file.serviceCount != 0) println("option java_generic_services = true;")
        printOptions(file.options)
        printExtensions(file.extensionList)
        printEnums(file.enumTypeList)
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

    private fun printImports(dependencyList: List<String>, publicDependencyList: List<Int>) {
        if (dependencyList.isNotEmpty()) if (first) first = false else println()
        dependencyList.forEachIndexed { i, dependency -> println("import ${if (publicDependencyList.contains(i)) "public " else ""}\"$dependency\";") }
    }

    private fun printOptions(options: GeneratedMessageV3.ExtendableMessage<*>) {
        val generatedOptions = generateOptions(options)
        if (generatedOptions.isNotEmpty()) if (first) first = false else println()
        generatedOptions.forEach { println("option $it;") }
    }

    private fun generateOptions(options: GeneratedMessageV3.ExtendableMessage<*>): MutableList<String> {
        val generatedOptions = mutableListOf<String>()
        options.allFields.forEach {
            generatedOptions += "${it.key.name} = ${
                when (it.value) {
                    is String -> "\"${it.value}\""
                    else -> it.value
                }
            }"
        }
        val messageExtensions = messageExtensions[".${options.descriptorForType.fullName}"]
        options.unknownFields.asMap().forEach { unknownField ->
            messageExtensions?.get(unknownField.key)?.let { messageExtension ->
                val messageExtensionFields = checkNotNull(messages[messageExtension.typeName]).fieldList
                unknownField.value.lengthDelimitedList.forEach {
                    val codedInputStream = it.newCodedInput()
                    while (true) {
                        val tag = codedInputStream.readTag()
                        if (tag == 0) break
                        val fieldNumber = WireFormat.getTagFieldNumber(tag)
                        val messageExtensionField = messageExtensionFields.single { it.number == fieldNumber }
                        generatedOptions += "(${messageExtension.name}).${messageExtensionField.name} = ${
                            when (messageExtensionField.type) {
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE -> codedInputStream.readDouble()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT -> codedInputStream.readFloat()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64 -> codedInputStream.readInt64()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64 -> codedInputStream.readUInt64()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32 -> codedInputStream.readInt32()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64 -> codedInputStream.readFixed64()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32 -> codedInputStream.readFixed32()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL -> codedInputStream.readBool()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING -> "\"${codedInputStream.readString()}\""
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES -> codedInputStream.readBytes()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32 -> codedInputStream.readUInt32()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM -> {
                                    val value = codedInputStream.readEnum()
                                    checkNotNull(enums[messageExtensionField.typeName]).valueList.single { it.number == value }.name
                                }
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32 -> codedInputStream.readSFixed32()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64 -> codedInputStream.readSFixed64()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32 -> codedInputStream.readSInt32()
                                DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64 -> codedInputStream.readSInt64()
                                else -> TODO()
                            }
                        }"
                    }
                }
            } ?: println("Unresolvable field .${options.descriptorForType.fullName} ${unknownField.key}")
        }
        return generatedOptions
    }

    private fun printExtensions(extensionList: List<DescriptorProtos.FieldDescriptorProto>) {
        var first0 = first
        extensionList.groupBy { it.extendee }.forEach {
            if (first0) first0 = false else println()
            println("extend ${it.key} {")
            first = true
            indentLevel++
            printFields(it.value)
            first = first0
            indentLevel--
            println("}")
        }
    }

    private fun printEnums(enumTypeList: List<DescriptorProtos.EnumDescriptorProto>) {
        var first0 = first
        enumTypeList.forEach {
            if (first0) first0 = false else println()
            println("enum ${it.name} {")
            first = true
            indentLevel++
            printOptions(it.options)
            printEnumValues(it.valueList)
            if (it.reservedRangeList.isNotEmpty()) {
                if (first) first = false else println()
                println(it.reservedRangeList.joinToString(prefix = "reserved ", postfix = ";") { if (it.start != it.end) "${it.start} to ${if (it.end != max) it.end else "max"}" else "${it.start}" })
            }
            if (it.reservedNameList.isNotEmpty()) {
                if (first) first = false else println()
                println(it.reservedNameList.joinToString(prefix = "reserved ", postfix = ";"))
            }
            first = first0
            indentLevel--
            println("}")
        }
    }

    private fun printEnumValues(enumValueList: List<DescriptorProtos.EnumValueDescriptorProto>) {
        if (enumValueList.isNotEmpty()) if (first) first = false else println()
        enumValueList.forEach {
            val generatedOptions = generateOptions(it.options)
            println("${it.name} = ${it.number}${if (generatedOptions.isNotEmpty()) " [${generatedOptions.joinToString()}]" else ""};")
        }
    }

    private fun printMessages(messageTypeList: List<DescriptorProtos.DescriptorProto>) {
        var first0 = first
        messageTypeList.forEach {
            if (first0) first0 = false else println()
            println("message ${it.name} {")
            first = true
            indentLevel++
            printOptions(it.options)
            printExtensions(it.extensionList)
            /*if (it.oneofDeclList.size != 0)*/
            printEnums(it.enumTypeList)
            printMessages(it.nestedTypeList)
            printFields(it.fieldList)
            if (it.extensionRangeCount != 0) {
                if (first) first = false else println()
                println(it.extensionRangeList.joinToString(prefix = "extensions ", postfix = ";") { if (it.start != it.end) "${it.start} to ${if (it.end != max) it.end else "max"}" else "${it.start}" })
            }
            if (it.reservedRangeCount != 0) {
                if (first) first = false else println()
                println(it.reservedRangeList.joinToString(prefix = "reserved ", postfix = ";") { if (it.start != it.end) "${it.start} to ${if (it.end != max) it.end else "max"}" else "${it.start}" })
            }
            if (it.reservedNameCount != 0) {
                if (first) first = false else println()
                println(it.reservedNameList.joinToString(prefix = "reserved ", postfix = ";"))
            }
            first = first0
            indentLevel--
            println("}")
        }
    }

    private fun printFields(fieldList: List<DescriptorProtos.FieldDescriptorProto>) {
        if (fieldList.isNotEmpty()) if (first) first = false else println()
        fieldList.forEach {
            val generatedOptions = generateOptions(it.options)
            if (it.hasDefaultValue()) generatedOptions += "default = ${if (it.type == DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING) "\"${it.defaultValue}\"" else it.defaultValue}"
            println("${checkNotNull(fieldLabels[it.label])} ${if (it.hasTypeName()) it.typeName else checkNotNull(fieldTypes[it.type])} ${it.name} = ${it.number}${if (generatedOptions.isNotEmpty()) " [${generatedOptions.joinToString()}]" else ""};")
        }
    }

    private fun printServices(serviceList: List<DescriptorProtos.ServiceDescriptorProto>) {
        var first0 = first
        serviceList.forEach {
            if (first0) first0 = false else println()
            println("service ${it.name} {")
            first = true
            indentLevel++
            printOptions(it.options)
            printMethods(it.methodList)
            first = first0
            indentLevel--
            println("}")
        }
    }

    private fun printMethods(methodList: List<DescriptorProtos.MethodDescriptorProto>) {
        var first0 = first
        methodList.forEach {
            if (first0) first0 = false else println()
            println("rpc ${it.name} (${it.inputType}) returns (${it.outputType}) {")
            first = true
            indentLevel++
            printOptions(it.options)
            first = first0
            indentLevel--
            println("}")
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
        private val max = 1 shl 29
    }
}
