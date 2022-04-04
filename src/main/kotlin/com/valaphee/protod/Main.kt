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

    val extendedMessages = mutableMapOf<String, MutableMap<Int, String>>()
    fileDescriptorProtos.forEach {
        it.extensionList.forEach {
            extendedMessages.getOrPut(it.extendee) { mutableMapOf() }[it.number] = it.name
        }
    }

    val outputPath = File("output")
    fileDescriptorProtos.forEach { File(outputPath, it.name).apply { parentFile.mkdirs() }.printWriter().use { printWriter -> ProtoWriter(printWriter).print(it) } }
}
