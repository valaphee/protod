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

import com.google.protobuf.CodedInputStream
import com.google.protobuf.DescriptorProtos
import com.valaphee.protod.util.occurrencesOf
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import java.io.File

fun main(arguments: Array<String>) {
    val argumentParser = ArgParser("protod")
    val inputArgument by argumentParser.option(ArgType.String, "input", "i", "Input file").required()
    val outputArgument by argumentParser.option(ArgType.String, "output", "o", "Output path").required()
    argumentParser.parse(arguments)

    val bytes = File(inputArgument).readBytes()
    val files = mutableListOf<DescriptorProtos.FileDescriptorProto>()
    String(bytes, Charsets.US_ASCII).occurrencesOf(".proto").forEach {
        var offset = 0
        while (true) {
            try {
                val length = CodedInputStream.newInstance(bytes, it - offset, offset).readRawVarint32()
                if (length == offset + 5) {
                    val begin = it - offset - 1
                    val end = bytes.size
                    if (CodedInputStream.newInstance(bytes, begin, end - begin).readTag() == 10) {
                        var offset0 = end - begin
                        while (true) {
                            val codedInputStream = CodedInputStream.newInstance(bytes, begin, offset0)
                            try {
                                files += DescriptorProtos.FileDescriptorProto.parseFrom(codedInputStream)

                                break
                            } catch (_: Exception) {
                            }
                            offset0 = codedInputStream.totalBytesRead - 1
                        }
                    }
                    break
                } else if (bytes[it - offset].toInt().toChar().isISOControl()) break
            } catch (_: Exception) {
            }
            offset++
        }
    }

    val enums = mutableMapOf<String, DescriptorProtos.EnumDescriptorProto>()
    val messages = mutableMapOf<String, DescriptorProtos.DescriptorProto>()
    files.forEach { file ->
        file.enumTypeList.forEach { enums[".${file.`package`}.${it.name}"] = it }
        file.messageTypeList.forEach {
            fun flatten(name: String, message: DescriptorProtos.DescriptorProto) {
                messages[name] = message
                message.enumTypeList.forEach { enums["$name.${it.name}"] = it }
                message.nestedTypeList.forEach { flatten("$name.${it.name}", it) }
            }

            flatten(".${file.`package`}.${it.name}", it)
        }
    }
    val messageExtensions = mutableMapOf<String, MutableMap<Int, DescriptorProtos.FieldDescriptorProto>>()
    files.forEach { file -> file.extensionList.forEach { extension -> messages[extension.typeName]?.let { messageExtensions.getOrPut(extension.extendee) { mutableMapOf() }[extension.number] = extension } } }

    val outputPath = File(outputArgument)
    files.forEach { file ->
        if (!included.contains(file.name)) File(outputPath, file.name).apply { parentFile.mkdirs() }.printWriter().use { printWriter ->
            printWriter.println(
                """
                /* AUTO-GENERATED FILE. DO NOT MODIFY.
                 */
            """.trimIndent()
            )
            ProtoWriter(printWriter, enums, messages, messageExtensions).print(file)
        }
    }
}

private val included = setOf(
    "google/protobuf/compiler/plugin.proto",
    "google/protobuf/any.proto",
    "google/protobuf/api.proto",
    "google/protobuf/descriptor.proto",
    "google/protobuf/duration.proto",
    "google/protobuf/empty.proto",
    "google/protobuf/field_mask.proto",
    "google/protobuf/source_context.proto",
    "google/protobuf/struct.proto",
    "google/protobuf/timestamp.proto",
    "google/protobuf/type.proto",
    "google/protobuf/wrappers.proto",
)
