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
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.ExtensionRegistry
import com.valaphee.protod.util.occurrencesOf
import java.io.File

fun main() {
    val bytes = File("C:\\Program Files (x86)\\Battle.net\\Battle.net.13401\\battle.net.dll").readBytes()
    val fileDescriptorProtos = mutableListOf<DescriptorProtos.FileDescriptorProto>()
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
                                fileDescriptorProtos += DescriptorProtos.FileDescriptorProto.parseFrom(codedInputStream)

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

    val fileDescriptorProtos0 = fileDescriptorProtos.toMutableList()
    val fileDescriptors = mutableMapOf<String, Descriptors.FileDescriptor>()
    var changed = true
    while (fileDescriptorProtos0.isNotEmpty() && changed) {
        changed = false

        val fileDescriptorProtoIterator = fileDescriptorProtos0.iterator()
        while (fileDescriptorProtoIterator.hasNext()) {
            val fileDescriptorProto = fileDescriptorProtoIterator.next()
            if (fileDescriptorProto.dependencyList.all { fileDescriptors.contains(it) }) {
                try {
                    val fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, fileDescriptorProto.dependencyList.map { fileDescriptors[it] }.toTypedArray())
                    fileDescriptors[fileDescriptor.name] = fileDescriptor
                    fileDescriptorProtoIterator.remove()
                    changed = true
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    val extensionRegistry = ExtensionRegistry.newInstance()
    fileDescriptors.values.forEach { it.extensions.forEach {if (it.javaType == Descriptors.FieldDescriptor.JavaType.MESSAGE) extensionRegistry.add(it, DynamicMessage.newBuilder(it.messageType).apply { it.messageType.fields.forEach { setField(it, it.defaultValue) } }.build()) else extensionRegistry.add(it) } }

    val outputPath = File("output")
    fileDescriptorProtos.forEach {
        File(outputPath, it.name).apply { parentFile.mkdirs() }.printWriter().use { printWriter ->
            ProtoWriter(printWriter).also {
                it.extensionRegistry = extensionRegistry
            }.print(it)
        }
    }
}
