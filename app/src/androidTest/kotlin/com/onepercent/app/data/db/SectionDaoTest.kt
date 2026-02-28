package com.onepercent.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onepercent.app.data.model.Section
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [SectionDao] using an in-memory Room database.
 * No Hilt needed — the DAO is exercised directly.
 */
@RunWith(AndroidJUnit4::class)
class SectionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var sectionDao: SectionDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sectionDao = db.sectionDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertSection_returnsGeneratedId() = runBlocking {
        val id = sectionDao.insertSection(Section(name = "Work", createdAt = 0L))
        assertTrue(id > 0L)
    }

    @Test
    fun deleteSection_removesSection() = runBlocking {
        val id = sectionDao.insertSection(Section(name = "Delete me", createdAt = 0L))
        val toDelete = sectionDao.getAllSections().first().find { it.id == id }!!
        sectionDao.deleteSection(toDelete)
        val remaining = sectionDao.getAllSections().first()
        assertTrue(remaining.none { it.id == id })
    }

    @Test
    fun getAllSections_emptyWhenNoSections() = runBlocking {
        val sections = sectionDao.getAllSections().first()
        assertTrue(sections.isEmpty())
    }

    @Test
    fun getAllSections_orderedByPositionAsc() = runBlocking {
        sectionDao.insertSection(Section(name = "Third",  createdAt = 0L, position = 2))
        sectionDao.insertSection(Section(name = "First",  createdAt = 0L, position = 0))
        sectionDao.insertSection(Section(name = "Second", createdAt = 0L, position = 1))
        val sections = sectionDao.getAllSections().first()
        assertEquals("First",  sections[0].name)
        assertEquals("Second", sections[1].name)
        assertEquals("Third",  sections[2].name)
    }

    @Test
    fun updatePosition_affectsOrdering() = runBlocking {
        val id1 = sectionDao.insertSection(Section(name = "A", createdAt = 0L, position = 0))
        val id2 = sectionDao.insertSection(Section(name = "B", createdAt = 0L, position = 1))
        // Swap positions so B comes first.
        sectionDao.updatePosition(id1, 1)
        sectionDao.updatePosition(id2, 0)
        val sections = sectionDao.getAllSections().first()
        assertEquals("B", sections[0].name)
        assertEquals("A", sections[1].name)
    }
}
