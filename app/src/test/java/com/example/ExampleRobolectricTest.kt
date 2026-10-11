package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jules.sdk.models.Session
import com.example.jules.sdk.models.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jules", appName)
    }

    @Test
    fun `test jules session model extraction`() {
        val session = Session(
            name = "sessions/xyz123",
            id = "xyz123",
            title = "Test Coding Session"
        )
        assertEquals("xyz123", session.cleanId)
        assertEquals("Test Coding Session", session.displayTitle)
    }

    @Test
    fun `test source clean name helper`() {
        val source = Source(name = "sources/github/TheRealAshik/Jules")
        assertEquals("TheRealAshik/Jules", source.cleanName)
    }
}
