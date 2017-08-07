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
package fr.marcwrobel.jbanking.creditor

import fr.marcwrobel.jbanking.IsoCountry
import fr.marcwrobel.jbanking.iban.IbanCheckDigit

import java.util.regex.Pattern

/**
 *
 *
 * A Creditor Identifier (CI) Code as specified by the
 * [EPC](http://www.europeanpaymentscouncil.eu/index.cfm/knowledge-bank/epc-documents/creditor-identifier-overview/).
 *
 *
 *
 *
 *
 * CI structure:
 *
 *  * Position 1-2 filled with the ISO country code.
 *  * Position 3-4 filled with the check digit according to ISO 7064 Mod 97-10.
 *  * Position 5-7 filled with the Creditor Business Code, if not used then filled with ZZZ.
 *  * Position 8 onwards filled with the country specific part of the identifier being
 * a national identifier of the Creditor, as defined by the National Community.
 *
 *
 *
 *
 *
 *
 * This class handles validation of the check digit and validation of the Creditor Identifier Structure described above without
 * going into the validation of the national identifier.
 *
 *
 *
 *
 * Instances of this class are immutable and are safe for use by multiple concurrent threads.
 *
 * @author Charles Kayser
 * @see [EPC Creditor Identifier Overview](http://www.europeanpaymentscouncil.eu/index.cfm/knowledge-bank/epc-documents/creditor-identifier-overview/)
 */
class CreditorIdentifier {

    private val creditorId: String

    /**
     * Create a new Creditor Identifier from the given country code, the creditor business code
     * and the creditor national id.
     *
     * @param country            A non null IsoCountry.
     * @param businessCode       A non null String.
     * @param creditorNationalId A non null String.
     * @throws IllegalArgumentException                                          if either the IsoCountry or BBAN is null
     * @throws fr.marcwrobel.jbanking.creditor.CreditorIdentifierFormatException if a valid Creditor Identifier could not be calculated using the
     * given IsoCountry, business code and creditor national id
     */
    constructor(country: IsoCountry?, businessCode: String?, creditorNationalId: String?) {
        if (country == null) {
            throw IllegalArgumentException("the country argument cannot be null")
        }

        if (businessCode == null) {
            throw IllegalArgumentException("the business code argument cannot be null")
        }

        if (creditorNationalId == null) {
            throw IllegalArgumentException("the creditorNationalId argument cannot be null")
        }

        val normalizedNationalId = normalize(creditorNationalId)
        val normalizedCreditorId = country.code + "00" + normalizedNationalId

        if (!isWellFormatted(normalizedCreditorId)) {
            throw CreditorIdentifierFormatException.forNotProperlyFormattedInput(creditorNationalId)
        }

        val checkDigits = IbanCheckDigit.INSTANCE.calculate(normalizedCreditorId)

        this.creditorId = country.code + checkDigits + businessCode + normalizedNationalId
    }

    /**
     * Create a new creditor identifier from the given string.
     *
     * @param creditorId a non null String.
     */
    constructor(creditorId: String?) {
        if (creditorId == null) {
            throw IllegalArgumentException("the creditor identifier argument cannot be null")
        }

        val normalizedCreditorId = normalize(creditorId)

        if (!isWellFormatted(normalizedCreditorId)) {
            throw CreditorIdentifierFormatException.forNotProperlyFormattedInput(normalizedCreditorId)
        }

        val country = findCountryFor(normalizedCreditorId) ?: throw CreditorIdentifierFormatException.forUnknownCountry(creditorId)

        val normalizedCreditorIdWithoutBusinessCode = removeBusinessCode(normalizedCreditorId)
        if (!IbanCheckDigit.INSTANCE.validate(normalizedCreditorIdWithoutBusinessCode)) {
            throw CreditorIdentifierFormatException.forIncorrectCheckDigits(creditorId)
        }

        this.creditorId = normalizedCreditorId
    }

    /**
     * Extract the ISO 3166-1-alpha-2 country code from this Creditor Identifier.
     *
     * @return A non null string representing this Creditor Identifier ISO 3166-1-alpha-2 country code.
     */
    val countryCode: String
        get() = creditorId.substring(COUNTRY_CODE_INDEX, COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH)

    /**
     * Extract the check digit from this Creditor Identifier.
     *
     * @return A non null string representing this Creditor Identifier check digit.
     */
    val checkDigit: String
        get() = creditorId.substring(CHECK_DIGITS_INDEX, CHECK_DIGITS_INDEX + CHECK_DIGITS_LENGTH)

    /**
     * Extract the business code from this Creditor Identifier.
     *
     * @return A non null string representing this Creditor Identifier business code.
     */
    val businessCode: String
        get() = creditorId.substring(CREDITOR_BUSINESS_CODE_INDEX, CREDITOR_BUSINESS_CODE_INDEX + CREDITOR_BUSINESS_CODE_LENGTH)

    /**
     * Extract the creditor national identifier from this Creditor Identifier.
     *
     * @return A non null string representing this Creditor Identifier National Id.
     */
    val nationalIdentifier: String
        get() = creditorId.substring(CREDITOR_NATIONAL_ID_INDEX)

    override fun toString(): String {
        return creditorId
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }

        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val other = o as CreditorIdentifier?

        return creditorId == other!!.creditorId
    }

    override fun hashCode(): Int {
        return creditorId.hashCode()
    }

    companion object {

        private val BASIC_REGEX = "[A-Za-z]{2}[0-9]{2}[A-Za-z0-9]{3}[A-Za-z0-9]+"
        private val BASIC_PATTERN = Pattern.compile(BASIC_REGEX)

        private val COUNTRY_CODE_INDEX = 0
        private val COUNTRY_CODE_LENGTH = 2
        private val CHECK_DIGITS_INDEX = COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH
        private val CHECK_DIGITS_LENGTH = 2
        private val CREDITOR_BUSINESS_CODE_INDEX = CHECK_DIGITS_INDEX + CHECK_DIGITS_LENGTH
        private val CREDITOR_BUSINESS_CODE_LENGTH = 3
        private val CREDITOR_NATIONAL_ID_INDEX = CREDITOR_BUSINESS_CODE_INDEX + CREDITOR_BUSINESS_CODE_LENGTH

        /**
         *
         * Returns a normalized string representation of the given Creditor Identifier.
         *
         *
         *
         * Normalized means the string is:
         *  * made of uppercase characters
         *  * contains no spaces
         *
         */
        private fun normalize(creditorIdentifier: String): String {
            return creditorIdentifier.replace("\\s+".toRegex(), "").toUpperCase()
        }

        /**
         *
         * Check if the given string matches the basic format of a Creditor Identifier.
         *
         * Returns `true` if the given strings matches the following pattern:
         *
         *  * Position 1-2 filled with alphabetic values (the ISO country code).
         *  * Position 3-4 filled with numeric values (the check digits).
         *  * Position 5-7 filled with alpha-numeric values (the Creditor Business Code).
         *  * Position 8 onwards filled with alpha-numeric values (a national identifier of the Creditor).
         *
         *
         */
        private fun isWellFormatted(creditorIdentifier: String): Boolean {
            return BASIC_PATTERN.matcher(creditorIdentifier).matches()
        }

        /**
         *
         * Returns the `Country` reference from the given Creditor Identifier string.
         *
         * Returns null if not found.
         */
        private fun findCountryFor(creditorIdentifier: String): IsoCountry? {
            return IsoCountry.fromCode(creditorIdentifier.substring(COUNTRY_CODE_INDEX, COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH))
        }

        /**
         *
         * Removes the business code part from the given Creditor Identifier string.
         */
        private fun removeBusinessCode(creditorIdentifier: String): String {
            return creditorIdentifier.substring(COUNTRY_CODE_INDEX, CREDITOR_BUSINESS_CODE_INDEX) + creditorIdentifier.substring(CREDITOR_NATIONAL_ID_INDEX)
        }

        /**
         * Validates the given Creditor Identifier String.
         *
         * @param creditorIdentifier A String.
         * @return `true` if the given String is a valid Creditor Identifier, `false` otherwise.
         */
        fun isValid(creditorIdentifier: String?): Boolean {
            if (creditorIdentifier == null) {
                return false
            }

            val normalizedCreditorId = normalize(creditorIdentifier)

            if (!isWellFormatted(normalizedCreditorId)) {
                return false
            }

            val country = findCountryFor(normalizedCreditorId) ?: return false

            val normalizedCreditorIdWithoutBusinessCode = removeBusinessCode(normalizedCreditorId)

            return IbanCheckDigit.INSTANCE.validate(normalizedCreditorIdWithoutBusinessCode)
        }
    }

}
