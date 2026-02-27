package com.onepercent.app.data.repository

import androidx.room.withTransaction
import com.onepercent.app.data.db.AppDatabase
import com.onepercent.app.data.db.EntryDao
import com.onepercent.app.data.db.SectionDao
import com.onepercent.app.data.model.Section
import kotlinx.coroutines.flow.Flow

class SectionRepositoryImpl(
    private val db: AppDatabase,
    private val sectionDao: SectionDao,
    private val entryDao: EntryDao
) : SectionRepository {

    override suspend fun addSection(name: String): Long =
        sectionDao.insertSection(Section(name = name, createdAt = System.currentTimeMillis()))

    /**
     * Runs as a transaction: first nullifies the sectionId of all child entries
     * (making them free-floating), then deletes the section row itself.
     */
    override suspend fun deleteSection(section: Section) {
        entryDao.clearSection(section.id)
        sectionDao.deleteSection(section)
    }

    override fun getAllSections(): Flow<List<Section>> = sectionDao.getAllSections()

    override suspend fun reorderSections(sections: List<Section>) {
        db.withTransaction {
            sections.forEachIndexed { index, section ->
                sectionDao.updatePosition(section.id, index)
            }
        }
    }
}
