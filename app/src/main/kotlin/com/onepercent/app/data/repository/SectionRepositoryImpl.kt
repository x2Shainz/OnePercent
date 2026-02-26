package com.onepercent.app.data.repository

import com.onepercent.app.data.db.EntryDao
import com.onepercent.app.data.db.SectionDao
import com.onepercent.app.data.model.Section
import kotlinx.coroutines.flow.Flow

class SectionRepositoryImpl(
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
}
