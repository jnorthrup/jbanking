/**
 * Copyright 2013 Marc Wrobel (marc.wrobel@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.marcwrobel.jbanking.swift

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A partial compiled representation of a SWIFT expression (a kind of regular expression) as used in many SWIFT documents
 * (for instance [the IBAN registry document](http://www.swift.com/dsp/resources/documents/IBAN_Registry.pdf)).
 *
 *
 * This class internally uses a [java.util.regex.Pattern] by transforming the SWIFT expression to a traditional regular expression. As a result the
 * SwiftPattern API is similar to the [Pattern API][java.util.regex.Pattern].
 *
 *
 *
 * The SwiftPattern class partially supports the SWIFT expression by using the following constructions :<br></br>
 *
 * **Character representations :**
 * <table>
 * <tr><td>n</td><td>digits (numeric characters 0 to 9 only)</td></tr>
 * <tr><td>a</td><td>upper case letters (alphabetic characters A-Z only)</td></tr>
 * <tr><td>c</td><td>upper and lower case alphanumeric characters (A-Z, a-z and 0-9)</td></tr>
 * <tr><td>e</td><td>blank space</td></tr>
</table> *
 *
 * **Length indications :**
 * <table>
 * <tr><td>nn!</td><td>fixed length</td></tr>
 * <tr><td>nn</td><td>maximum length</td></tr>
</table> *
 *
 *
 *
 *
 * Here are some examples of SWIFT expressions that are supported by this SwiftPattern :<br></br>
 *
 *  * `4!n` (corresponding regex [0-9]{4}) : four consecutive digits
 *  * `4!a3c` (corresponding regex [A-Za-z0-9]{4}[A-Z]{1,3}) : four consecutive upper or lower case alphanumeric characters followed by one to three upper case letters
 *  * `2e4!a` (corresponding regex [ ]{1,2}) : one or two consecutive spaces followed by four consecutive upper case letters
 *
 *
 *
 *
 * Instances of this class are immutable and are safe for use by multiple concurrent threads.
 *
 * @author Marc Wrobel
 * @see java.util.regex.Pattern
 *
 * @since 1.0
 */
class SwiftPattern private constructor(
        /**
         * Returns the SWIFT expression from which this pattern was compiled.
         *
         * @return a non null expression that matches {@value #}
         */
        val expression: String,
        /**
         * Returns the [java Pattern][java.util.regex.Pattern] build using the SWIFT expression.
         *
         * @return a non null pattern
         */
        val equivalentJavaPattern: Pattern) {

    /**
     * Creates a matcher that will match the given input against this pattern.
     *
     * @param input The character sequence to be matched
     * @return A new matcher for this pattern
     */
    fun matcher(input: CharSequence): Matcher {
        return equivalentJavaPattern.matcher(input)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }

        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val that = o as SwiftPattern?
        return if (expression != that!!.expression) {
            false
        } else true

    }

    override fun hashCode(): Int {
        return 13 + expression.hashCode()
    }

    override fun toString(): String {
        return "$expression{regex=$equivalentJavaPattern}"
    }

    companion object {

        internal val DIGITS_CHARACTER = 'n'
        internal val UPPER_CASE_LETTERS_CHARACTER = 'a'
        internal val UPPER_AND_LOWER_CASE_ALPHANUMERICS_CHARACTER = 'c'
        internal val SPACES_CHARACTER = 'e'

        private val DIGITS_CLASS = "[0-9]"
        private val UPPER_CASE_LETTERS_CLASS = "[A-Z]"
        private val UPPER_AND_LOWER_CASE_ALPHANUMERICS_CLASS = "[A-Za-z0-9]"
        private val SPACES_CLASS = "[ ]"

        private val GROUP_REGEX = "[0-9]+!?[ance]"
        private val SWIFT_FORMAT_PATTERN = Pattern.compile("^($GROUP_REGEX)+$")
        private val SWIFT_FORMAT_GROUPS_PATTERN = Pattern.compile(GROUP_REGEX)

        /**
         * Compiles the given SWIFT expression into a SwiftPattern.
         *
         * @param expression The expression to be compiled
         * @throws SwiftPatternSyntaxException If the expression's syntax is invalid
         */
        fun compile(expression: String?): SwiftPattern {
            if (expression == null) {
                throw IllegalArgumentException("the given parameter expression cannot be null")
            }

            if (!SWIFT_FORMAT_PATTERN.matcher(expression).matches()) {
                throw SwiftPatternSyntaxException(expression)
            }

            return SwiftPattern(expression, Pattern.compile(toRegex(expression)))
        }

        private fun toRegex(expression: String): String {
            val matcher = SWIFT_FORMAT_GROUPS_PATTERN.matcher(expression)

            val regex = StringBuilder("^")
            while (matcher.find()) {
                regex.append(transform(matcher.group()))
            }
            regex.append("$")

            return regex.toString()
        }

        private fun transform(simpleExpression: String): String {
            val length = simpleExpression.length

            var charactersRegex: String? = null
            when (simpleExpression[length - 1]) {
                DIGITS_CHARACTER -> charactersRegex = DIGITS_CLASS
                UPPER_CASE_LETTERS_CHARACTER -> charactersRegex = UPPER_CASE_LETTERS_CLASS
                UPPER_AND_LOWER_CASE_ALPHANUMERICS_CHARACTER -> charactersRegex = UPPER_AND_LOWER_CASE_ALPHANUMERICS_CLASS
                SPACES_CHARACTER -> charactersRegex = SPACES_CLASS
            }

            val strict = simpleExpression[simpleExpression.length - 2] == '!'
            val maxOccurence = simpleExpression.substring(0, length - if (strict) 2 else 1)

            return charactersRegex + "{" + (if (strict) "" else "1,") + maxOccurence + "}"
        }
    }
}
