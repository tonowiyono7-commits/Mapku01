package com.example

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import com.example.detector.MockLocationDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name matches Mapku`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mapku", appName)
    }

    @Test
    fun `isLocationFromMockProvider detects mock flag accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertFalse(MockLocationDetector.isLocationFromMockProvider(context, null))

        val authenticLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 37.7749
            longitude = -122.4194
            isMock = false
        }
        assertFalse(MockLocationDetector.isLocationFromMockProvider(context, authenticLocation))

        val spoofedLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 48.8566
            longitude = 2.3522
            isMock = true
        }
        assertTrue(MockLocationDetector.isLocationFromMockProvider(context, spoofedLocation))
    }

    @Test
    fun `isAllowMockLocationsOn returns false on API 23 and above`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(MockLocationDetector.isAllowMockLocationsOn(context))
    }

    @Test
    fun `inspectVisiblePackages completes without error`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = MockLocationDetector.inspectVisiblePackages(context)
        assertFalse(result.hasMockApps)
    }
}
