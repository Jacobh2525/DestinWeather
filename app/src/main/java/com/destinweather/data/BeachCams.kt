package com.destinweather.data

/**
 * Curated YouTube live streams for area beach cams.
 *
 * To add or replace a cam: open the YouTube live stream in a browser and copy
 * the video ID (the part after "watch?v=" in the URL), then add a line below.
 * Cams that go offline show an "Offline" badge automatically and can be
 * replaced without touching any UI code.
 */
val beachCams = listOf(
    BeachCam(
        name = "Dune Allen Beach Cam",
        location = "Dune Allen Beach 30a",
        videoId = "xNZBPxx8ykg"
    ),
    BeachCam(
        name = "Mid-Bay Bridge Cam",
        location = "Lulu's Destin",
        videoId = "SLnToeuwLhA"
    ),
    BeachCam(
        name = "Inlet Reef Beach Cam",
        location = "Destin, Florida",
        videoId = "DbpGwxabykI"
    ),
    BeachCam(
        name = "Wyndham Garden Beach Cam",
        location = "Ft. Walton Beach, Florida",
        videoId = "QnvxSpZ9HPI"
    )
)

data class BeachCam(
    val name: String,
    val location: String,
    val videoId: String
)
