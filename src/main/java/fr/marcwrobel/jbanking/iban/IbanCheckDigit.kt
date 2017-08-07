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
package fr.marcwrobel.jbanking.iban

/**
 * Provide ISO 7064 Mod 97,10 IBAN check digit calculation and validation.
 *
 *
 *
 * This class is implementing the singleton pattern by being an enumeration. Algorithm is based on the work in
 * [Apache commons
 * validator project](http://svn.apache.org/viewvc/commons/proper/validator/trunk/src/main/java/org/apache/commons/validator/routines/checkdigit/IBANCheckDigit.java?view=co).
 *
 *
 * @author Marc Wrobel
 * @see [http://en.wikipedia.org/wiki/International_Bank_Account_Number](http://en.wikipedia.org/wiki/International_Bank_Account_Number)
 *
 * @see [IBANCheckDigit](http://svn.apache.org/viewvc/commons/proper/validator/trunk/src/main/java/org/apache/commons/validator/routines/checkdigit/IBANCheckDigit.java?view=co)
 *
 * @since 1.0
 */
enum class IbanCheckDigit {
    INSTANCE;

    /**
     * Validate the given IBAN check digit.
     *
     * @param iban a non null string.
     * @return `true` if the given IBAN check digit is valid, `false` otherwise.
     * @throws IllegalArgumentException if the given IBAN is null or if its size is not at least four characters.
     */
    fun validate(iban: String): Boolean {
        return modulus(iban) == CHECK_DIGITS_REMAINDER
    }

    /**
     * Calculate the given IBAN check digit. For a valid calculation the given IBAN its characters have to be alphanumeric ([a-zA-Z0-9]) and check digit characters have to be set
     * to zero.
     *
     * @param iban a non null string
     * @return the given IBAN check digit
     */
    fun calculate(iban: String): String {
        val modulusResult = modulus(iban)
        val charValue = 98 - modulusResult
        val checkDigit = Integer.toString(charValue)
        return if (charValue > 9) checkDigit else "0" + checkDigit
    }

    private fun modulus(iban: String?): Int {
        if (iban == null) {
            throw IllegalArgumentException("the iban argument cannot be null")
        }
        if (iban.length <= BBAN_INDEX) {
            throw IllegalArgumentException("the iban argument size must be grater than " + BBAN_INDEX)
        }

        val reformattedIban = iban.substring(BBAN_INDEX) + iban.substring(0, BBAN_INDEX)
        var total: Long = 0
        for (i in 0..reformattedIban.length - 1) {
            val charValue = Character.getNumericValue(reformattedIban.get(i))
            total = (if (charValue > 9) total * 100 else total * 10) + charValue
            if (total > CHECK_DIGITS_MAX) {
                total = total % CHECK_DIGITS_MODULUS
            }
        }

        return (total % CHECK_DIGITS_MODULUS).toInt()
    }

    companion object {

        private val BBAN_INDEX = 4

        private val CHECK_DIGITS_MAX: Long = 999999999
        private val CHECK_DIGITS_MODULUS: Long = 97
        private val CHECK_DIGITS_REMAINDER = 1
    }
}
