package com.onepercent.app.data.repository

import androidx.room.withTransaction
import com.onepercent.app.data.db.AppDatabase
import com.onepercent.app.data.db.EntryDao
import com.onepercent.app.data.model.Entry
import kotlinx.coroutines.flow.Flow

class EntryRepositoryImpl(
    private val db: AppDatabase,
    private val entryDao: EntryDao
) : EntryRepository {

    override suspend fun addEntry(title: String, body: String, sectionId: Long?): Long =
        entryDao.insertEntry(
            Entry(
                title = title,
                body = body,
                sectionId = sectionId,
                createdAt = System.currentTimeMillis()
            )
        )

    override suspend fun updateEntry(id: Long, title: String, body: String) =
        entryDao.updateEntry(id, title, body)

    override suspend fun deleteEntry(entry: Entry) = entryDao.deleteEntry(entry)

    override fun getEntryById(id: Long): Flow<Entry?> = entryDao.getEntryById(id)

    override fun getAllEntries(): Flow<List<Entry>> = entryDao.getAllEntries()

    override suspend fun moveEntry(entryId: Long, newSectionId: Long?) =
        entryDao.updateSection(entryId, newSectionId)

    override fun searchEntries(query: String): Flow<List<Entry>> =
        entryDao.searchEntries(query)

    override suspend fun reorderEntries(entries: List<Entry>) {
        db.withTransaction {
            entries.forEachIndexed { index, entry ->
                entryDao.updatePosition(entry.id, index)
            }
        }
    }
}
