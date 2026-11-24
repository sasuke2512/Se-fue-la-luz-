package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OutageDatabase
import com.example.data.local.OutageEntity
import com.example.data.repository.OutageRepository
import com.example.domain.OutageStatsCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OutagePersistenceRobolectricTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var database: OutageDatabase
    private lateinit var repository: OutageRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.cacheDir, "test_outages_${System.nanoTime()}.db")
        database = Room.databaseBuilder(context, OutageDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        repository = OutageRepository(database.outageDao())
    }

    @After
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `active outage survives database reopen and prevents duplicate active outage`() = runTest {
        val startTs = 1_763_900_000_000L
        val firstId = repository.startOutage(startTs)
        assertNotNull(firstId)

        // Attempting to start a second outage while one is active must be rejected
        val duplicateId = repository.startOutage(startTs + 60_000L)
        assertNull(duplicateId)

        // Simulate app restart / phone reboot by closing and reopening the SQLite file
        database.close()
        database = Room.databaseBuilder(context, OutageDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        repository = OutageRepository(database.outageDao())

        val restoredActive = repository.getActiveOutageOnce()
        assertNotNull(restoredActive)
        assertEquals(startTs, restoredActive!!.startTimeMillis)
        assertNull(restoredActive.endTimeMillis)

        // Elapsed time computed from persisted timestamp vs current time
        val simulatedNowAfterReboot = startTs + 5_432_000L
        val elapsed = simulatedNowAfterReboot - restoredActive.startTimeMillis
        assertEquals(5_432_000L, elapsed)

        // Stop active outage and verify it enters completed history
        val stopped = repository.stopActiveOutage(simulatedNowAfterReboot)
        assertNotNull(stopped)
        assertEquals(simulatedNowAfterReboot, stopped!!.endTimeMillis)
        assertNull(repository.getActiveOutageOnce())
        assertEquals(1, repository.completedOutages.first().size)
    }

    @Test
    fun `json export and import restore outages without duplicating existing records`() = runTest {
        val tz = TimeZone.getTimeZone("America/Havana")
        val cal = Calendar.getInstance(tz).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 23, 8, 0, 0)
        }
        val t1Start = cal.timeInMillis
        val t1End = t1Start + 3_600_000L
        val t2Start = t1Start + 7_200_000L
        val t2End = t2Start + 1_800_000L

        repository.addManualOutage(t1Start, t1End, "Mañana")
        repository.addManualOutage(t2Start, t2End, "Mediodía")

        val json = repository.exportCompletedToJson("1.0.0")

        // Re-importing the exact same backup must skip both as duplicates
        val reimportSame = repository.importFromJson(json)
        assertEquals(0, reimportSame.addedCount)
        assertEquals(2, reimportSame.skippedDuplicateCount)

        // Wipe and restore from backup
        repository.deleteAllOutages()
        assertEquals(0, repository.completedOutages.first().size)

        val restoreResult = repository.importFromJson(json)
        assertEquals(2, restoreResult.addedCount)
        assertEquals(0, restoreResult.skippedDuplicateCount)

        val restored = repository.completedOutages.first()
        assertEquals(2, restored.size)

        val now = t2End + 3_600_000L
        val stats = OutageStatsCalculator.calculate(
            outages = restored,
            nowMillis = now,
            timeZone = tz
        )
        assertEquals(5_400_000L, stats.today.withoutPowerMillis)
        assertEquals(2, stats.today.outageCount)
    }

    @Test
    fun `clearTrackingStart removes trackingStartMillis and allows reinitialization from reset time`() = runTest {
        val prefs = com.example.data.local.UserPreferencesRepository(context)
        val initialTime = 1_763_900_000_000L
        val resolved1 = prefs.ensureTrackingStartInitialized(initialTime)
        assertEquals(initialTime, resolved1)
        assertEquals(initialTime, prefs.trackingStartMillis.first())

        // Clearing removes the key completely
        prefs.clearTrackingStart()
        assertNull(prefs.trackingStartMillis.first())

        // Next initialization sets the new tracking start timestamp
        val resetTime = initialTime + 7_200_000L
        val resolved2 = prefs.ensureTrackingStartInitialized(resetTime)
        assertEquals(resetTime, resolved2)
        assertEquals(resetTime, prefs.trackingStartMillis.first())
    }
}
