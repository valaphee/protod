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
import kotlinx.cli.default
import kotlinx.cli.multiple
import java.io.File

fun main(arguments: Array<String>) {
    val argumentParser = ArgParser("protod")
    val input by argumentParser.argument(ArgType.String, "input", "Input file")
    val output by argumentParser.argument(ArgType.String, "output", "Output path")
    val exclude by argumentParser.option(ArgType.String, "exclude", null, "Exclude files").multiple().default(listOf("google/protobuf/compiler/plugin.proto", "google/protobuf/any.proto", "google/protobuf/api.proto", "google/protobuf/descriptor.proto", "google/protobuf/duration.proto", "google/protobuf/empty.proto", "google/protobuf/field_mask.proto", "google/protobuf/source_context.proto", "google/protobuf/struct.proto", "google/protobuf/timestamp.proto", "google/protobuf/type.proto", "google/protobuf/wrappers.proto"))
    argumentParser.parse(arguments)

    val inputFile = File(input)
    println("Searching for Protocol Buffers descriptors in $inputFile")

    val bytes = File(input).readBytes()
    val files = mutableListOf<DescriptorProtos.FileDescriptorProto>()
    bytes.occurrencesOf(".proto".toByteArray()).forEach {
        var offset = 0
        while (true) {
            try {
                val length = CodedInputStream.newInstance(bytes, it - offset, offset).readRawVarint32()
                if (length == offset + 5) {
                    val begin = it - offset - 1
                    if (CodedInputStream.newInstance(bytes, begin, bytes.size - begin).readTag() == 10) {
                        var read = bytes.size - begin
                        while (true) {
                            val codedInputStream = CodedInputStream.newInstance(bytes, begin, read)
                            try {
                                files += DescriptorProtos.FileDescriptorProto.parseFrom(codedInputStream)
                                println("Descriptor found, begins at 0x${begin.toString(16).uppercase()}, ends at 0x${(begin + read).toString(16).uppercase()}")

                                break
                            } catch (_: Exception) {
                            }
                            read = codedInputStream.totalBytesRead - 1
                        }
                    }
                    break
                } else if (bytes[it - offset].toInt().toChar().isISOControl()) break
            } catch (_: Exception) {
            }
            offset++
        }
    }

    println("Building lookup table")
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

    println("Generating ${files.size} Protocol Buffers definitions")
    val outputPath = File(output)
    files.forEach { file ->
        if (!exclude.contains(file.name)) {
            val outputFile = File(outputPath, file.name)
            outputFile.parentFile.mkdirs()
            outputFile.printWriter().use { printWriter ->
                printWriter.println(
                    """
                /* AUTO-GENERATED FILE. DO NOT MODIFY.
                 */
                """.trimIndent()
                )
                ProtoWriter(printWriter, enums, messages, messageExtensions).print(file)
            }
            println("Generated $outputFile")
        }
    }
}
