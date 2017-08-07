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

import fr.marcwrobel.jbanking.IsoCountry

import java.util.regex.Pattern

/**
 *
 *
 * An International Bank Account Number (IBAN) Code as specified by the
 * [ISO 13616:2007 standard](http://www.swift.com/solutions/messaging/information_products/directory_products/iban_format_registry/index.page).
 *
 *
 *
 *
 * An IBAN consists of a two-letter ISO 3166-1 country code, followed by two check digits and up to thirty alphanumeric characters for a BBAN
 * (Basic Bank Account Number) which has a fixed length per country and, included within it, a bank identifier with a fixed position and a fixed length per country.
 * The check digits are calculated based on the scheme defined in ISO/IEC 7064 (MOD97-10).
 * Also note that an IBAN is case insensitive.
 *
 *
 *
 *
 * This class handles validation of the check digit and validation of the [BBAN structure][BbanStructure] (based on information specified in the IBAN
 * registry Release 45 of may 2013).
 *
 *
 *
 * Instances of this class are immutable and are safe for use by multiple concurrent threads.
 *
 * @author Marc Wrobel
 * @see BbanStructure
 *
 * @see [http://en.wikipedia.org/wiki/International_Bank_Account_Number](http://wikipedia.org/wiki/International_Bank_Account_Number)
 *
 * @see [IBAN registry](http://www.swift.com/solutions/messaging/information_products/directory_products/iban_format_registry/index.page)
 *
 * @since 1.0
 */
class Iban {

    private val iban: String

    /**
     * Create a new IBAN from the given country code and BBAN.
     *
     * @param country A non null IsoCountry.
     * @param bban    A non null String.
     * @throws IllegalArgumentException if either the IsoCountry or BBAN is null
     * @throws IbanFormatException      if a valid IBAN could not be calculated using the given IsoCountry and BBAN
     */
    constructor(country: IsoCountry?, bban: String?) {
        if (country == null) {
            throw IllegalArgumentException("the country argument cannot be null")
        }

        if (bban == null) {
            throw IllegalArgumentException("the bban argument cannot be null")
        }

        val normalizedBban = normalize(bban)
        val normalizedIban = country.code + "00" + normalizedBban

        val bbanStructure = BbanStructure.forCountry(country) ?: throw IbanFormatException.forNotSupportedCountry(bban, country)

        if (!bbanStructure.isBbanValid(normalizedBban)) {
            throw IbanFormatException.forInvalidBbanStructure(bban, bbanStructure)
        }

        val checkDigits = IbanCheckDigit.INSTANCE.calculate(normalizedIban)

        this.iban = country.code + checkDigits + normalizedBban
    }

    /**
     * Create a new IBAN from the given string.
     *
     * @param iban A non null String.
     * @throws IllegalArgumentException if the given string is null
     * @throws IbanFormatException      if the given string is not a valid IBAN
     */
    constructor(iban: String?) {
        if (iban == null) {
            throw IllegalArgumentException("the iban argument cannot be null")
        }

        val normalizedIban = normalize(iban)

        if (!isWellFormatted(normalizedIban)) {
            throw IbanFormatException.forNotProperlyFormattedInput(normalizedIban)
        }

        val country = findCountryFor(normalizedIban) ?: throw IbanFormatException.forUnknownCountry(iban)

        val bbanStructure = BbanStructure.forCountry(country) ?: throw IbanFormatException.forNotSupportedCountry(iban, country)

        if (!bbanStructure.isBbanValid(normalizedIban.substring(BBAN_INDEX))) {
            throw IbanFormatException.forInvalidBbanStructure(iban, bbanStructure)
        }

        if (!IbanCheckDigit.INSTANCE.validate(normalizedIban)) {
            throw IbanFormatException.forIncorrectCheckDigits(iban)
        }

        this.iban = normalizedIban
    }

    /**
     * Extract the ISO 3166-1-alpha-2 country code from this IBAN.
     *
     * @return A non null string representing this IBAN ISO 3166-1-alpha-2 country code.
     */
    val countryCode: String
        get() = iban.substring(COUNTRY_CODE_INDEX, COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH)

    /**
     * Extract the check digit from this IBAN.
     *
     * @return A non null string representing this IBAN check digit.
     */
    val checkDigit: String
        get() = iban.substring(CHECK_DIGITS_INDEX, CHECK_DIGITS_INDEX + CHECK_DIGITS_LENGTH)

    /**
     * Extract the BBAN from this IBAN.
     *
     * @return A non null string representing this IBAN BBAN.
     */
    val bban: String
        get() = iban.substring(BBAN_INDEX)

    /**
     * Gets the printable version of this IBAN.
     *
     *
     *
     * When printed on paper, the IBAN is expressed in groups of four characters separated by a single space, the last group being of variable length
     *
     *
     * @return A non null string representing this IBAN formatted for printing.
     */
    fun toPrintableString(): String {
        val printableIban = StringBuilder(iban)
        val length = iban.length

        for (i in 0..length / GROUP_SIZE_FOR_PRINTABLE_IBAN - 1) {
            printableIban.insert((i + 1) * GROUP_SIZE_FOR_PRINTABLE_IBAN + i, ' ')
        }

        return printableIban.toString()
    }

    override fun toString(): String {
        return iban
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }

        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val other = o as Iban?
        return if (iban != other!!.iban) {
            false
        } else true

    }

    /**
     *
     * Returns a normalized string representation of this IBAN.
     *
     *
     * Normalized means the string is:
     *  * made of uppercase characters
     *  * contains no spaces
     *
     *
     * @return a normalized string representation of this IBAN
     */
    override fun hashCode(): Int {
        return 29 * iban.hashCode()
    }

    companion object {

        private val BASIC_REGEX = "[A-Za-z]{2}[0-9]{2}[A-Za-z0-9]+"
        private val BASIC_PATTERN = Pattern.compile(BASIC_REGEX)

        private val COUNTRY_CODE_INDEX = 0
        private val COUNTRY_CODE_LENGTH = 2
        private val CHECK_DIGITS_INDEX = COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH
        private val CHECK_DIGITS_LENGTH = 2
        private val BBAN_INDEX = CHECK_DIGITS_INDEX + CHECK_DIGITS_LENGTH

        private val GROUP_SIZE_FOR_PRINTABLE_IBAN = 4

        /**
         * Validates the given IBAN String.
         *
         * @param iban A String.
         * @return `true` if the given String is a valid IBAN, `false` otherwise.
         */
        fun isValid(iban: String?): Boolean {
            if (iban == null) {
                return false
            }

            val normalizedIban = normalize(iban)

            if (!isWellFormatted(normalizedIban)) {
                return false
            }

            val country = findCountryFor(normalizedIban) ?: return false

            val bbanStructure = BbanStructure.forCountry(country) ?: return false

            if (!bbanStructure.isBbanValid(normalizedIban.substring(BBAN_INDEX))) {
                return false
            }

            return if (!IbanCheckDigit.INSTANCE.validate(normalizedIban)) {
                false
            } else true

        }

        private fun normalize(iban: String): String {
            return iban.replace("\\s+".toRegex(), "").toUpperCase()
        }

        private fun isWellFormatted(s: String): Boolean {
            return BASIC_PATTERN.matcher(s).matches()
        }

        private fun findCountryFor(s: String): IsoCountry? {
            return IsoCountry.fromCode(s.substring(COUNTRY_CODE_INDEX, COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH))
        }
    }
}