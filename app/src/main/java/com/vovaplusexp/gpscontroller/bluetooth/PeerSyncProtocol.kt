package com.vovaplusexp.gpscontroller.bluetooth

import com.vovaplusexp.gpscontroller.models.Location
import com.vovaplusexp.gpscontroller.models.PeerEstimate
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PeerSyncProtocol {

    fun createPacket(location: Location, deviceId: Long): ByteArray {
        val buffer = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(location.timestamp)
        buffer.putDouble(location.latitude)
        buffer.putDouble(location.longitude)
        buffer.putFloat(location.confidence)
        buffer.putLong(deviceId)
        return buffer.array()
    }

    fun parsePacket(packet: ByteArray, deviceId: Long): PeerEstimate? {
        if (packet.size != PACKET_SIZE) return null

        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        val timestamp = buffer.long
        val latitude = buffer.double
        val longitude = buffer.double
        val confidence = buffer.float

        val location = Location(
            latitude = latitude,
            longitude = longitude,
            speed = 0f,
            bearing = 0f,
            timestamp = timestamp,
            source = Location.LocationSource.PEER_FUSED,
            confidence = confidence
        )

        return PeerEstimate(location, deviceId, timestamp)
    }

    companion object {
        const val PACKET_SIZE = 36 // 8 (ts) + 8 (lat) + 8 (lon) + 4 (conf) + 8 (id)
    }
}
