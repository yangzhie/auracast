package com.innovatex.auracast.core

import com.innovatex.auracast.bluetooth.BroadcastMetadata
import com.innovatex.auracast.data.Stop

// The class holds no state, better as object
// Do not need a throwaway instance each time
object StopMatcher {
    fun matches(metadata: BroadcastMetadata, stop: Stop): Boolean {
        // Check: if a stop is fitted with Auracast
        val broadcast = stop.broadcast
        if (broadcast == null) {
            return false
        }

        // Version is not compared
        // MetadataParser rejects before anything reaches this point

        // Compare the metadata and broadcast attributes and return bool
        return metadata.routeID == broadcast.routeID &&
                metadata.stopIndex == broadcast.stopIndex &&
                metadata.direction == broadcast.direction &&
                metadata.language == broadcast.language
    }
}