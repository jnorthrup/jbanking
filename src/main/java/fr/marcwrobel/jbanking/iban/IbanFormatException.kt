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

/**
 * Thrown to indicate that an attempt has been made to convert a string to a [Iban], but that the string does not have the appropriate format.
 *
 * @author Marc Wrobel
 * @see Iban.Iban
 * @since 1.0
 */
class IbanFormatException
/**
 * Constructs a `IbanFormatException` with the string that caused the error and the given detail message.
 *
 * @param input   a string
 * @param message a string
 */
private constructor(
        /**
         * Returns the input String that caused this exception to be raised.
         *
         * @return a string
         */
        val inputString: String, message: String) : RuntimeException(message) {
    companion object {

        internal fun forNotProperlyFormattedInput(input: String): IbanFormatException {
            return IbanFormatException(input, String.format("'%s' format is not appropriate for an IBAN", input))
        }

        internal fun forIncorrectCheckDigits(input: String): IbanFormatException {
            return IbanFormatException(input, String.format("'%s' check digits are incorrect", input))
        }

        internal fun forUnknownCountry(input: String): IbanFormatException {
            return IbanFormatException(input, String.format("'%s' country code is not an ISO 3166-1-alpha-2 code", input))
        }

        internal fun forNotSupportedCountry(input: String, country: IsoCountry): IbanFormatException {
            return IbanFormatException(input, String.format("'%s' country does not support IBAN", country))
        }

        internal fun forInvalidBbanStructure(input: String, bbanStructure: BbanStructure): IbanFormatException {
            return IbanFormatException(input, String.format("'%s' BBAN structure is not valid against BBAN structure used in %s", input, bbanStructure.country))
        }
    }
}
