package com.innovatex.auracast.bluetooth

object FmaSourceResolver {

    fun resolve(
        androidBroadcast: DiscoveredBroadcast,
        fmaBroadcasts: Collection<FmaReceiverBroadcast>
    ): FmaReceiverBroadcast? {

        val androidAddress =
            normalizeAddress(
                androidBroadcast.deviceAddress
            )

        for (fmaBroadcast in fmaBroadcasts) {

            val fmaAddress =
                normalizeAddress(
                    fmaBroadcast.address
                )

            if (fmaAddress == androidAddress) {
                return fmaBroadcast
            }
        }

        val androidName =
            androidBroadcast.broadcastName

        if (androidName != null) {

            for (fmaBroadcast in fmaBroadcasts) {

                if (
                    fmaBroadcast.broadcastName ==
                    androidName
                ) {
                    return fmaBroadcast
                }
            }
        }

        return null
    }

    private fun normalizeAddress(
        address: String
    ): String {

        return address
            .replace(":", "")
            .replace("-", "")
            .trim()
            .uppercase()
    }
}