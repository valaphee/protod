/*
 * Copyright (c) 2021-2022, Valaphee.
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

package com.valaphee.protod.util

fun ByteArray.occurrencesOf(pattern: ByteArray): Sequence<Int> {
    if (isEmpty() || pattern.isEmpty()) return emptySequence()
    if (pattern.size == 1) return indices.asSequence().filter { this[it] == pattern[0] }

    val resultTable = IntArray(pattern.size)
    var matches = 0
    for (i in 1 until pattern.size) {
        while (matches > 0 && pattern[matches] != pattern[i]) matches = resultTable[matches]
        if (pattern[matches] == pattern[i]) matches++
        resultTable[i] = matches
    }

    var i = 0
    var _matches = 0
    return generateSequence {
        while (i < size) {
            while (_matches > 0 && pattern[_matches] != this[i]) _matches = resultTable[_matches - 1]
            if (pattern[_matches] == this[i]) _matches++
            if (_matches == pattern.size) {
                _matches = resultTable[_matches - 1]
                i++
                return@generateSequence i - pattern.size
            }
            i++
        }
        return@generateSequence null
    }
}
