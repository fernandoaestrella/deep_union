/*
 * Copyright (c) 2023, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.kotlin.ble.ui.scanner.main

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.kotlin.ble.ui.scanner.repository.ScanningState
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResults
import no.nordicsemi.android.kotlin.ble.ui.scanner.R
import no.nordicsemi.android.kotlin.ble.ui.scanner.main.viewmodel.ScannerViewModel

@Suppress("FunctionName")
internal fun LazyListScope.DeviceListItems(
    devices: ScanningState.DevicesDiscovered,
    onClick: (BleScanResults) -> Unit,
    deviceView: @Composable (BleScanResults) -> Unit,
    viewModel: ScannerViewModel
) {
    val bondedDevices = devices.bonded
    val discoveredDevices = devices.notBonded

    if (bondedDevices.isNotEmpty()) {
        item {
            Text(
                text = stringResource(id = R.string.bonded_devices),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }
        items(bondedDevices.size) {
            ClickableDeviceItem(bondedDevices[it], onClick, deviceView, viewModel)
        }
    }

    if (discoveredDevices.isNotEmpty()) {
        item {
            Text(
                text = stringResource(id = R.string.discovered_devices),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }

        items(discoveredDevices.size) {
            ClickableDeviceItem(discoveredDevices[it], onClick, deviceView, viewModel)
        }
    }
}

internal fun countMatches(otherUserDataByteArray: ByteArray, viewModel: ScannerViewModel): Int {
    val myUserDataByteArray = viewModel.getUserData()
    var myUserByteIndex = 0
    var matchesCount = 0

    for (otherUserByte in otherUserDataByteArray) {
        for (bitIndex in 0..7) {
            // In order to only look at the first 14 bits
            if (myUserByteIndex == 0 || (myUserByteIndex == 1 && bitIndex < 6)) {
                // If we are now looking at any bit placed in an even position (0, 2, 4, 6)
                if (bitIndex % 2 == 0) {
                    // If this user's bit is 1
                    if ((myUserDataByteArray[myUserByteIndex].toInt() shr bitIndex) and 1 != 0) {
                        // Compare whether the next bit in the other user's data is 1
                        if ((otherUserByte.toInt() shr (bitIndex + 1)) and 1 != 0) {
                            matchesCount++
                        }
                    }
                // Else, if we are looking at a bit placed in an odd position (1, 3, 5, 7)
                } else {
                    // If this user's bit is 1
                    if ((myUserDataByteArray[myUserByteIndex].toInt() shr bitIndex) and 1 != 0) {
                        // Check whether the previous bit in the other user's data is 1
                        if ((otherUserByte.toInt() shr (bitIndex - 1)) and 1 != 0) {
                            matchesCount++
                        }
                    }
                }
            }
        }

        myUserByteIndex++
    }

    return matchesCount
}

fun hexStringToByteArray(hexString: String): ByteArray {
    val byteArray = ByteArray(hexString.length / 2)
    for (i in hexString.indices step 2) {
        val highNibble = Character.digit(hexString[i], 16) shl 4
        val lowNibble = Character.digit(hexString[i + 1], 16)
        byteArray[i / 2] = (highNibble or lowNibble).toByte()
    }
    return byteArray
}

fun byteArrayToHexString(byteArray: ByteArray): String {
    return byteArray.joinToString("") { it.toString(16).padStart(2, '0') }
}

fun byteArrayToBinaryString(byteArray: ByteArray): String {

    val booleanString = StringBuilder()
    for (byte in byteArray) {
        for (bitIndex in 7 downTo 0) {
            val bit = (byte.toInt() shr bitIndex) and 1 != 0
            booleanString.append(if (bit) '1' else '0')
        }
    }
    return booleanString.toString()

//    return byteArray.joinToString("") { it.toString(2).padStart(8, '0') }
}

@Composable
private fun ClickableDeviceItem(
    device: BleScanResults,
    onClick: (BleScanResults) -> Unit,
    deviceView: @Composable (BleScanResults) -> Unit,
    viewModel: ScannerViewModel
) {


    if (device.scanResult.isNotEmpty()) {
        val userDataString = device.scanResult[0].scanRecord?.serviceData.toString().substringAfter("=(0x) ").substringBefore("}").replace(":","")

        Log.d("progression", "hexstring" + userDataString + "\nbytearray: " + byteArrayToBinaryString(hexStringToByteArray(userDataString)) + "\noutput user description: " + outputUserDescription(hexStringToByteArray(userDataString)))
        Text(text = "With this user, you have this many matches: " + countMatches(hexStringToByteArray(userDataString), viewModel).toString() + " out of 14")
        Text(text = "This user looks like this: \n" + outputUserDescription(hexStringToByteArray(userDataString)))
        Text(text = "User Data: $userDataString")

    }
    Box(modifier = Modifier
        .clip(RoundedCornerShape(10.dp))
        .clickable { onClick(device) }
        .padding(8.dp)
    ) {
//        Text(text = device.scanResult[0].scanRecord?.serviceData.toString())

        deviceView(device)
    }
}

fun byteArrayToBitArray(byteArray: ByteArray): List<Boolean> {
    val bitArray = mutableListOf<Boolean>()
    // Convert each byte to a bit array maintaining the order of the bits inside the bytes
    for (bitChar in byteArrayToBinaryString(byteArray)) {
        if (bitChar == '1') {
            bitArray.add(true)
        } else {
            bitArray.add(false)
        }
    }
    return bitArray
}

fun outputUserDescription(otherUserDataByteArray: ByteArray): String {
    var outputString = ""
    var otherUserLooksLikeMan = false
    // creates an array of bits from the input array
    val bitArray = byteArrayToBitArray(otherUserDataByteArray)
    var bitIndex = 0

    Log.d("outputUserDescription", "bitArray: $bitArray")
    for (bit in bitArray) {
        when (bitIndex) {
            14 -> {
                // If this user's bit is 1
                if (bit) {
                    otherUserLooksLikeMan = true
                    outputString += "Man"
                } else {
                    outputString += "Woman"
                }

                outputString += "\n"
            }
            15 -> {
                // if the other user looks like a man
                if (otherUserLooksLikeMan) {
                    // If the other user's bit is true
                    if (bit) {
                        outputString += "Taller than 5 feet 9 inches (175 cm)"
                    } else {
                        outputString += "Shorter than 5 feet 9 inches (175 cm)"
                    }
                    // Else if the other user looks like a woman
                } else {
                    // If the other user's bit is true
                    if (bit) {
                        outputString += "Taller than 5 feet 4 inches (162 cm)"
                    } else {
                        outputString += "Shorter than 5 feet 4 inches (162 cm)"
                    }
                }

                outputString += "\n"
            }
            16 -> {
                if (otherUserLooksLikeMan) {
                    // If the other user's bit is 1
                    if (bit) {
                        outputString += "Older than 30.3 years"
                    } else {
                        outputString += "Younger than 30.3 years"
                    }
                    // Else if the other user looks like a woman
                } else {
                    if (bit) {
                        outputString += "Older than 31.8 years"
                    } else {
                        outputString += "Younger than 31.8 years"
                    }
                }

                outputString += "\n"
            }
            17 -> {
                if (otherUserLooksLikeMan) {
                    if (bit) {
                        outputString += "Has facial hair"
                    } else {
                        outputString += "Does not have facial hair"
                    }
                    // Else if the other user looks like a woman
                } else {
                    if (bit) {
                        outputString += "Hair reaches below shoulder"
                    } else {
                        outputString += "Hair does not reach below shoulder"
                    }
                }

                outputString += "\n"
            }
            18 -> {
                if (bit) {
                    outputString += "Wearing glasses"
                } else {
                    outputString += "Not wearing glasses"
                }

                outputString += "\n"
            }
        }

        // Represents next 4 bits as an int

        val otherUserTopColor = mutableListOf<Boolean>()
//
//        if (bitIndex == 19)
//        otherUserTopColor.add(bit)

        bitIndex++
    }

//    Log.d("outputUserDescription", "outputString: $outputString")
    return outputString
}
