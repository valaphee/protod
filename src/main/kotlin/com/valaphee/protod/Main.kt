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
import java.io.File

fun main() {
    val bytes = File("C:\\Program Files (x86)\\Battle.net\\Battle.net.13401\\battle.net.dll").readBytes()
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

    val messages = mutableMapOf<String, DescriptorProtos.DescriptorProto>()
    files.forEach { file -> file.messageTypeList.forEach { message -> messages[".${file.`package`}.${message.name}"] = message } }
    val messageExtensions = mutableMapOf<String, MutableMap<Int, DescriptorProtos.FieldDescriptorProto>>()
    files.forEach { file -> file.extensionList.forEach { extension -> messages[extension.typeName]?.let { messageExtensions.getOrPut(extension.extendee) { mutableMapOf() }[extension.number] = extension } } }

    val outputPath = File("output")
    files.forEach { file -> File(outputPath, file.name).apply { parentFile.mkdirs() }.printWriter().use { printWriter -> ProtoWriter(printWriter, messages, messageExtensions).print(file) } }
}
