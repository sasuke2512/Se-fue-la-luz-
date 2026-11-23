package com.example.data.repository

import com.example.data.local.OutageDao
import com.example.data.local.OutageEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

data class ImportResult(
    val addedCount: Int,
    val skippedDuplicateCount: Int,
    val invalidCount: Int
)

class OutageRepository(private val outageDao: OutageDao) {

    val activeOutage: Flow<OutageEntity?> = outageDao.observeActiveOutage()
    val allOutages: Flow<List<OutageEntity>> = outageDao.observeAllOutages()
    val completedOutages: Flow<List<OutageEntity>> = outageDao.observeCompletedOutages()

    suspend fun getActiveOutageOnce(): OutageEntity? = outageDao.getActiveOutageOnce()

    suspend fun startOutage(startTimeMillis: Long = System.currentTimeMillis()): Long? {
        return outageDao.startOutageIfNoneActive(startTimeMillis)
    }

    suspend fun stopActiveOutage(endTimeMillis: Long = System.currentTimeMillis()): OutageEntity? {
        return outageDao.stopActiveOutage(endTimeMillis)
    }

    suspend fun cancelActiveOutage() {
        outageDao.deleteAllActive()
    }

    suspend fun addManualOutage(startTimeMillis: Long, endTimeMillis: Long, note: String): Long {
        require(endTimeMillis > startTimeMillis) {
            "La hora de fin debe ser posterior a la hora de inicio."
        }
        return outageDao.insert(
            OutageEntity(
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                note = note.trim(),
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateOutage(outage: OutageEntity) {
        val end = outage.endTimeMillis
        if (end != null) {
            require(end > outage.startTimeMillis) {
                "La hora de fin debe ser posterior a la hora de inicio."
            }
        }
        outageDao.update(outage.copy(note = outage.note.trim()))
    }

    suspend fun deleteOutage(id: Long) {
        outageDao.deleteById(id)
    }

    suspend fun deleteAllOutages() {
        outageDao.deleteAll()
    }

    suspend fun exportCompletedToJson(appVersion: String = "1.0.0"): String {
        val completed = outageDao.getCompletedOutagesOnce()
        val array = JSONArray()
        completed.forEach { item ->
            val end = item.endTimeMillis ?: return@forEach
            array.put(
                JSONObject().apply {
                    put("startTimeMillis", item.startTimeMillis)
                    put("endTimeMillis", end)
                    put("note", item.note)
                    put("createdAtMillis", item.createdAtMillis)
                }
            )
        }
        return JSONObject().apply {
            put("app", "Se fue la luz!")
            put("schemaVersion", 1)
            put("appVersion", appVersion)
            put("exportedAtMillis", System.currentTimeMillis())
            put("outages", array)
        }.toString(2)
    }

    suspend fun importFromJson(rawJson: String): ImportResult {
        val root = JSONObject(rawJson)
        val array = root.getJSONArray("outages")
        val existing = outageDao.getCompletedOutagesOnce()
            .mapNotNull { entry ->
                entry.endTimeMillis?.let { end -> entry.startTimeMillis to end }
            }
            .toMutableSet()

        val toInsert = mutableListOf<OutageEntity>()
        var duplicates = 0
        var invalid = 0

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            if (obj == null) {
                invalid++
                continue
            }
            val start = obj.optLong("startTimeMillis", -1L)
            val end = obj.optLong("endTimeMillis", -1L)
            if (start <= 0L || end <= start) {
                invalid++
                continue
            }
            val key = start to end
            if (!existing.add(key)) {
                duplicates++
                continue
            }
            val note = obj.optString("note", "").trim()
            val createdAt = obj.optLong("createdAtMillis", start).takeIf { it > 0L } ?: start
            toInsert.add(
                OutageEntity(
                    startTimeMillis = start,
                    endTimeMillis = end,
                    note = note,
                    createdAtMillis = createdAt
                )
            )
        }

        if (toInsert.isNotEmpty()) {
            outageDao.insertAll(toInsert)
        }

        return ImportResult(
            addedCount = toInsert.size,
            skippedDuplicateCount = duplicates,
            invalidCount = invalid
        )
    }
}
