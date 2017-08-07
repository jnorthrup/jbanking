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
package fr.marcwrobel.jbanking.bic

import fr.marcwrobel.jbanking.IsoCountry

import java.util.regex.Pattern

/**
 *
 *
 * A Business Identifier Code (also known as BIC, SWIFT-BIC, BIC code, SWIFT ID or SWIFT code, Business Entity Identifier or BEI) as specified by ISO 9362:2009.
 *
 *
 *
 *
 * A BIC is either eight (BIC8) or eleven (BIC11) characters made up of :
 *  * 4 letters: institution code (or bank code)
 *  * 2 letters: ISO 3166-1 alpha-2 country code
 *  * 2 letters or digits: location code
 *  * 3 letters or digits (optional): branch code
 *
 * Where an 8-digit code is given, it is assumed that it refers to the primary office. The primary office is always designated by the branch code {@value #PRIMARY_OFFICE_BRANCH_CODE}).
 *
 *
 *
 *
 * This class is immutable.
 *
 *
 * @author Marc Wrobel
 * @see [http://wikipedia.org/wiki/Bank_Identifier_Code](http://wikipedia.org/wiki/Bank_Identifier_Code)
 *
 * @since 1.0
 */
class Bic
/**
 * Create a new bic from the given string.
 *
 * The given string may be a BIC8 or a BIC11.
 *
 * @param bic8Or11 A non null String.
 * @throws IllegalArgumentException if the given string is null
 * @throws BicFormatException       if the given BIC8 or BIC11 string does not match [.BIC_REGEX] or if the given BIC8 or BIC11 country code is not known in [fr.marcwrobel.jbanking.IsoCountry]
 */
(bic8Or11: String?) {

    private val normalizedBic: String

    init {
        if (bic8Or11 == null) {
            throw IllegalArgumentException("the bic8Or11 argument cannot be null")
        }

        if (!isWellFormatted(bic8Or11)) {
            throw BicFormatException.forNotProperlyFormattedInput(bic8Or11)
        }

        if (!hasKnownCountryCode(bic8Or11)) {
            throw BicFormatException.forUnknownCountryCode(bic8Or11)
        }

        var cleanedBic = bic8Or11.toUpperCase()
        if (cleanedBic.length == BIC8_LENGTH) {
            cleanedBic += PRIMARY_OFFICE_BRANCH_CODE
        }

        this.normalizedBic = cleanedBic
    }

    /**
     * Extract the institution code (or bank code) from this BIC.
     *
     * @return A non null string representing this BIC institution code.
     */
    val institutionCode: String
        get() = normalizedBic.substring(INSTITUTION_CODE_INDEX, INSTITUTION_CODE_INDEX + INSTITUTION_CODE_LENGTH)

    /**
     * Extract the country code from this BIC.
     *
     * @return A non null string representing this BIC country code.
     */
    val countryCode: String
        get() = normalizedBic.substring(COUNTRY_CODE_INDEX, COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH)

    /**
     * Extract the location code from this BIC.
     *
     * @return A non null string representing this BIC location code.
     */
    val locationCode: String
        get() = normalizedBic.substring(LOCATION_CODE_INDEX, LOCATION_CODE_INDEX + LOCATION_CODE_LENGTH)

    /**
     * Extract the branch code from this BIC.
     *
     * @return A non null string representing this BIC branch code.
     */
    val branchCode: String
        get() = normalizedBic.substring(BRANCH_CODE_INDEX, BRANCH_CODE_INDEX + BRANCH_CODE_LENGTH)

    /**
     * Test whether or not this BIC is a test bic.
     *
     *
     * A BIC is a test BIC if the last character of the location code is {@value #TEST_BIC_INDICATOR}.
     *
     * @return `true` if this BIC is a test BIC, otherwise `false`
     * @see .isLiveBic
     */
    val isTestBic: Boolean
        get() = normalizedBic[LOCATION_CODE_INDEX + LOCATION_CODE_LENGTH - 1] == TEST_BIC_INDICATOR

    /**
     * Test whether or not this BIC is a live bic.
     *
     *
     * A BIC is a live BIC if the last character of the location code is not {@value #TEST_BIC_INDICATOR}.
     *
     * @return `true` if this BIC is a live BIC, otherwise `false`
     * @see .isTestBic
     */
    val isLiveBic: Boolean
        get() = !isTestBic

    /**
     * Transform this BIC to a test BIC.
     *
     * @return this if this BIC is a test BIC, or this BIC corresponding test BIC otherwise.
     */
    fun asTestBic(): Bic {
        if (isTestBic) {
            return this
        }

        val testBicBuilder = StringBuilder(normalizedBic)
        testBicBuilder.setCharAt(LOCATION_CODE_INDEX + LOCATION_CODE_LENGTH - 1, TEST_BIC_INDICATOR)
        return Bic(testBicBuilder.toString())
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     *
     * To be equals to this one an other object must be a Bic and the BICs normalized form (see [.toString]) must be equal.
     *
     * @param o the object with which to compare.
     * @return `true` if this object is the same as the obj argument or `false` otherwise.
     * @see Object.toString
     */
    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }

        return if (o == null || javaClass != o.javaClass) {
            false
        } else normalizedBic == (o as Bic).normalizedBic

    }

    /**
     * @see Object.hashCode
     */
    override fun hashCode(): Int {
        return 31 * normalizedBic.hashCode()
    }

    /**
     *
     * Returns a normalized string representation of this BIC.
     *
     *
     * Normalized means the string is:
     *  * made of uppercase characters
     *  * eleven characters long (BIC11)
     *
     *
     * @return a normalized string representation of this BIC
     */
    override fun toString(): String {
        return normalizedBic
    }

    companion object {

        /**
         * A simple regex that validate well-formed BIC.
         */
        val BIC_REGEX = "[A-Za-z]{4}[A-Za-z]{2}[A-Za-z0-9]{2}([A-Za-z0-9]{3})?"

        /**
         * A pre-compiled Pattern for [.BIC_REGEX].
         */
        val BIC_PATTERN = Pattern.compile(BIC_REGEX)

        /**
         * The branch code for primary offices.
         */
        val PRIMARY_OFFICE_BRANCH_CODE = "XXX"

        /**
         * If the last character of the location code in a BIC is this one it means that the BIC is a Test BIC,
         */
        val TEST_BIC_INDICATOR = '0'

        private val BIC8_LENGTH = 8
        private val INSTITUTION_CODE_INDEX = 0
        private val INSTITUTION_CODE_LENGTH = 4
        private val COUNTRY_CODE_INDEX = INSTITUTION_CODE_INDEX + INSTITUTION_CODE_LENGTH
        private val COUNTRY_CODE_LENGTH = 2
        private val LOCATION_CODE_INDEX = COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH
        private val LOCATION_CODE_LENGTH = 2
        private val BRANCH_CODE_INDEX = LOCATION_CODE_INDEX + LOCATION_CODE_LENGTH
        private val BRANCH_CODE_LENGTH = 3

        /**
         * Check whether or not the given string is valid BIC.
         *
         * @param bic A String.
         * @return `true` if the given string is valid BIC, otherwise `false`
         */
        fun isValid(bic: String?): Boolean {
            return bic != null && isWellFormatted(bic) && hasKnownCountryCode(bic)
        }

        private fun isWellFormatted(s: String): Boolean {
            return BIC_PATTERN.matcher(s).matches()
        }

        private fun hasKnownCountryCode(s: String): Boolean {
            return IsoCountry.fromCode(s.substring(COUNTRY_CODE_INDEX, COUNTRY_CODE_INDEX + COUNTRY_CODE_LENGTH)) != null
        }
    }
}
