@file:Suppress("MaxLineLength", "MagicNumber", "LongParameterList", "UnusedParameter")

package se.hellsoft.stupidledscreen

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.bluetooth.BluetoothAddress
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_NOTIFY
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_READ
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_WRITE
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_WRITE_NO_RESPONSE
import androidx.bluetooth.GattClientScope
import androidx.bluetooth.ScanFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.util.UUID
import kotlin.experimental.and

const val DEVICE_NAME = "LED_BLE_59271C84"
const val DEVICE_ADDRESS = "A2:40:59:27:1C:84"

const val LED_SERVICE = "000000fa-0000-1000-8000-00805f9b34fb"
const val NOTIF_CHAR = "0000fa03-0000-1000-8000-00805f9b34fb"
const val WRITE_CHAR = "0000fa02-0000-1000-8000-00805f9b34fb"

const val NAME_SERVICE = "00001800-0000-1000-8000-00805f9b34fb"
const val NAME_CHAR = "00002a00-0000-1000-8000-00805f9b34fb"

data class CharacteristicProperties(
    val hasWrite: Boolean,
    val hasWriteNoResponse: Boolean,
    val hasRead: Boolean,
    val hasNotify: Boolean
)

@OptIn(ExperimentalStdlibApi::class)
@SuppressLint("MissingPermission")
class LedScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val ble: BluetoothLe = BluetoothLe(application)

    private val scanFilters = listOf(
        ScanFilter(
            deviceAddress = BluetoothAddress(
                DEVICE_ADDRESS,
                BluetoothAddress.ADDRESS_TYPE_PUBLIC
            ), deviceName = DEVICE_NAME
        )
    )

    fun scanAndConnect() {
        viewModelScope.launch {
            val scanResult = ble.scan(scanFilters)
                .onEach {
                    Log.d(
                        TAG,
                        "scanAndConnect: found device: ${it.deviceAddress.address} ${it.device.id}"
                    )
                }
                .first()
            ble.connectGatt(scanResult.device) {
                services.forEach { gattService ->
                    Log.d(TAG, "service: ${gattService.uuid}")
                    gattService.characteristics.forEach { gattCharacteristic ->
                        val props = characteristicProperties(gattCharacteristic)
                        Log.d(
                            TAG,
                            " characteristic: ${gattCharacteristic.uuid} " +
                                    "write=${props.hasWrite}, " +
                                    "writeNoResponse=${props.hasWriteNoResponse}, " +
                                    "read=${props.hasRead}, " +
                                    "notify=${props.hasNotify}"
                        )
                        gattCharacteristic.fwkCharacteristic.descriptors.forEach { gattDescriptor ->
                            Log.d(TAG, "  - descriptor: ${gattDescriptor.uuid} ")
                        }
                    }
                }

                val notifChar =
                    services.first { it.uuid == UUID.fromString(LED_SERVICE) }
                        .getCharacteristic(UUID.fromString(NOTIF_CHAR))!!
                val notifChannel = Channel<ByteArray>(capacity = Channel.BUFFERED)
                launch {
                    subscribeToCharacteristic(notifChar).collect {
                        notifChannel.send(it)
                        Log.d(TAG, "Receive notification: ${it.toHexString(HexFormat.UpperCase)}")
                    }
                }
//                val collectJob = launch {
//                    enableLEDNotifications(notifChar)
//                }

                val writeChar = services.first { it.uuid == UUID.fromString(LED_SERVICE) }
                    .getCharacteristic(UUID.fromString(WRITE_CHAR))!!


                // Delete all data
//                writeLEDCharacteristic(writeChar, byteArrayOf(4, 0, 3, Byte.MIN_VALUE))

                // Get LED type
//                writeLEDCharacteristic(writeChar, byteArrayOf(8, 0, 1, Byte.MIN_VALUE, 15, 54, 0, 1))
//                Log.d(TAG, "get led type: ${notifChannel.receive().toHexString()}")

                delay(100)

                // Set led light intensity
//                writeLEDCharacteristic(writeChar, byteArrayOf(5, 0, 4, Byte.MIN_VALUE, 10))

                writeLEDCharacteristic(writeChar, byteArrayOf(5, 0, 1, 1, 9.toByte()))
                Log.d(TAG, "setTextEffect: ${notifChannel.receive().toHexString()}")

                // Set clock (colock?) mode
//                val clockData = byteArrayOf(11, 0, 6, 1, 1, 1, 0, 0, 0, 0, 0)
//                clockData[7] = 1977.toByte()
//                clockData[8] = 2.toByte()
//                clockData[9] = 6.toByte()
//                clockData[10] = 6.toByte()
//                writeLEDCharacteristic(writeChar, clockData)

                // on/off
//                writeLEDCharacteristic(writeChar, byteArrayOf(5,0,7,1,1))

                // Get hardware info
//                writeLEDCharacteristic(writeChar, byteArrayOf(4, 0, 5, Byte.MIN_VALUE))
//                Log.d(TAG, "get hw info: ${notifChannel.receive().toHexString()}")

//                Send text data
//                val text = "erik".map { it.code.toByte() }.toByteArray()
//                val payload = defaultPayload(4, text, text, 0, 0, 0, 60)
//                writeLEDCharacteristic(writeChar, payload)
//                Log.d(TAG, "send text: ${notifChannel.receive().toHexString()}")

                // Send exit cmd
//                writeLEDCharacteristic(writeChar, byteArrayOf(4, 0, 1, 1))
            }
        }
    }

    private suspend fun GattClientScope.writeLEDCharacteristic(
        gattCharacteristic: GattCharacteristic,
        payload: ByteArray
    ) {
        Log.d(
            TAG,
            "Write ${payload.joinToString(":") { byte -> "%02x".format(byte) }} (${payload.size} bytes)"
        )
        val writeResult = writeCharacteristic(
            gattCharacteristic,
            payload
        )
        if (writeResult.isSuccess) {
            Log.d(
                TAG,
                "successfully wrote to ${gattCharacteristic.uuid}"
            )
        } else {
            Log.w(TAG, "failed writing to ${gattCharacteristic.uuid}")
        }
    }

    private suspend fun GattClientScope.enableLEDNotifications(gattCharacteristic: GattCharacteristic) {
        Log.d(TAG, "subscribe to ${gattCharacteristic.uuid}: ")

        subscribeToCharacteristic(gattCharacteristic)
            .onStart { Log.d(TAG, "Start collecting notifications!") }
            .onCompletion { Log.d(TAG, "Done collecting!") }
            .collect {
                Log.d(
                    TAG, "notification from ${gattCharacteristic.uuid}: ${
                        it.joinToString(":") { byte -> "%02x".format(byte) }
                    } - ${String(it, Charset.defaultCharset())}")
            }
    }

    private suspend fun GattClientScope.readLEDCharacteristic(gattCharacteristic: GattCharacteristic) {
        val read = readCharacteristic(gattCharacteristic)
        if (read.isSuccess) {
            read.getOrNull()?.let {
                Log.d(
                    TAG,
                    "read from ${gattCharacteristic.uuid}: " +
                            "${it.joinToString(":") { byte -> "%02x".format(byte) }} / " +
                            String(it, Charset.defaultCharset())
                )
            } ?: run {
                Log.d(
                    TAG,
                    "No response reading from ${gattCharacteristic.uuid}"
                )
            }
        } else {
            Log.w(
                TAG,
                "error reading ${gattCharacteristic.uuid}",
                read.exceptionOrNull()
            )
        }
    }

    private fun characteristicProperties(gattCharacteristic: GattCharacteristic): CharacteristicProperties {
        val hasWrite =
            gattCharacteristic.properties and PROPERTY_WRITE != 0
        val hasWriteNoResponse =
            gattCharacteristic.properties and PROPERTY_WRITE_NO_RESPONSE != 0
        val hasRead =
            gattCharacteristic.properties and PROPERTY_READ != 0
        val hasNotify =
            gattCharacteristic.properties and PROPERTY_NOTIFY != 0
        val props = CharacteristicProperties(hasWrite, hasWriteNoResponse, hasRead, hasNotify)
        return props
    }
}

fun crc32(source: ByteArray?, offset: Int, length: Int): Long {
    val crc32 = java.util.zip.CRC32()
    crc32.update(source)
//    Log.d("#1.1# CRC32 src: ", crc32.value.toString())
    return crc32.value
}

fun int2byte(res: Int): ByteArray {
    return byteArrayOf(
        (res and 255).toByte(),
        (res shr 8 and 255).toByte(),
        (res shr 16 and 255).toByte(),
        (res ushr 24).toByte()
    )
}

private fun changeLight(i: Int, bArr: ByteArray) {
    val length = bArr.size
    for (i2 in 0 until length) {
        bArr[i2] = ((bArr[i2] and 255.toByte()) * i / 100 and 255).toByte()
    }
}

private fun shouldCrc(i: Int): Boolean {
    return i == 2 || i == 1 || i == 3 || i == 4
}

private fun getDataType(i: Int): ByteArray {
    return when (i) {
        0, 6 -> byteArrayOf(0, 0)
        1 -> byteArrayOf(1, 0)
        2 -> byteArrayOf(2, 0)
        3 -> byteArrayOf(3, 0)
        4 -> byteArrayOf(0, 1)
        5 -> byteArrayOf(5, 1)
        else -> byteArrayOf(0, 0)
    }
}

const val LED_FRAME_SIZE = 3072

fun defaultPayload(
    i: Int,
    bArr: ByteArray,
    bArr2: ByteArray?,
    i2: Int,
    i3: Int,
    i4: Int,
    i5: Int,
): ByteArray {
    val bArr21: ByteArray? = if (i5 and 4 != 0) ByteArray(0) else bArr2
    return payload(
        i,
        bArr,
        bArr21,
        if (i5 and 8 != 0) 0 else i2,
        if (i5 and 16 != 0) LED_FRAME_SIZE else i3,
        if (i5 and 32 != 0) 100 else i4
    )
}

private fun payload(
    i: Int,
    bArr: ByteArray,
    bArr2: ByteArray?,
    i2: Int,
    input3: Int,
    i4: Int
): ByteArray {
    var i3 = input3
    val crc32: Long
    val i5 = if (i != 4) if (i != 5) 9 else 5 else 10
    val dataType: ByteArray = getDataType(i)
    val bArr3 = ByteArray(i5)
    val shouldCrc: Boolean = shouldCrc(i)
    var length = bArr.size + i5
    if (shouldCrc) {
        length = bArr.size + i5 + 5
    }
    bArr3[0] = (length and 255).toByte()
    bArr3[1] = (length shr 8 and 255).toByte()
    bArr3[2] = dataType[0]
    bArr3[3] = dataType[1]
    bArr3[4] = i2.toByte()
    if (i != 5) {
        if (i == 0 || i == 6) {
            i3 = bArr.size
        }
        val int2byte: ByteArray = int2byte(i3)
        System.arraycopy(int2byte, 0, bArr3, 5, int2byte.size)
    }
    if (i4 != 100) {
        changeLight(i4, bArr)
    }
    if (shouldCrc) {
        val bArr4 = ByteArray(5)
        crc32 = if (i == 1 || i == 3) {
            crc32(bArr2, 0, bArr.size)
        } else {
            crc32(bArr, 0, bArr.size)
        }
        val int2byte2: ByteArray = int2byte(crc32.toInt())
        System.arraycopy(int2byte2, 0, bArr4, 0, int2byte2.size)
        if (i == 3) {
            bArr4[4] = 2
        }
        return bArr3 + bArr4 + bArr
    }
    return bArr3 + bArr
}
