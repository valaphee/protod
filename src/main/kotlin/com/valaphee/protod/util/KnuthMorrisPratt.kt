package com.valaphee.protod.util

/**
 * For each i in [0, length), this function computes
 * the length of the longest suffix of a substring of pattern from 0 to i
 * that is also a prefix of the pattern itself.
 */
private fun computePrefixFunction(pattern: ByteArray): IntArray {
    val resultTable = IntArray(pattern.size)

    var matches = 0
    for (i in 1..pattern.size - 1) {
        while (matches > 0 && pattern[matches] != pattern[i]) {
            matches = resultTable[matches]
        }

        if (pattern[matches] == pattern[i]) {
            matches++
        }
        resultTable[i] = matches
    }

    return resultTable
}

/**
 * Returns a list of indices where the pattern occurs in this String. This method
 * searches character by character and thus does not support regular expressions
 * as input for the pattern.
 *
 * @param [pattern] The pattern to look for in this String. Regular expressions
 *                 are not supported
 * @param [ignoreCase] If true, characters are matched even if one is upper and the other is
 *                 lower case
 *
 * @return A list of indices where the supplied [pattern] starts in the text.
 */
public fun ByteArray.occurrencesOf(pattern: ByteArray): Sequence<Int> {

    if (isEmpty() || pattern.isEmpty()) {
        return emptySequence()
    }

    if (pattern.size == 1) {
        return indices.asSequence().filter { this[it].equals(pattern[0]) }
    }

    // Non-trivial pattern matching, perform computation
    // using Knuth-Morris-Pratt

    val prefixFunction = computePrefixFunction(pattern)

    var i = 0
    var matches = 0
    return generateSequence {
        while (i < size) {
            while (matches > 0 && !pattern[matches].equals(this[i])) {
                matches = prefixFunction[matches - 1]
            }

            if (pattern[matches].equals(this[i])) {
                matches++
            }
            if (matches == pattern.size) {
                matches = prefixFunction[matches - 1]
                i++
                return@generateSequence i - pattern.size
            }

            i++
        }

        return@generateSequence null
    }
}
