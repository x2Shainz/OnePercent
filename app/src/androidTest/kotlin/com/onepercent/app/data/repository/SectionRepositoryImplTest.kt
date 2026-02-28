package com.onepercent.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onepercent.app.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [SectionRepositoryImpl] using an in-memory Room database.
 * Exercises timestamp injection, cascade-delete (SET NULL on child entries), and reorder
 * transaction semantics.
 */
@RunWith(AndroidJUnit4::class)
class SectionRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var sectionRepo: SectionRepositoryImpl
    private lateinit var entryRepo: EntryRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sectionRepo = SectionRepositoryImpl(db, db.sectionDao(), db.entryDao())
        entryRepo = EntryRepositoryImpl(db, db.entryDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun addSection_stampsCreatedAtNearCurrentTime() = runBlocking {
        val before = System.currentTimeMillis()
        sectionRepo.addSection("Work")
        val after = System.currentTimeMillis()
        val sections = sectionRepo.getAllSections().first()
        assertEquals(1, sections.size)
        assertTrue(sections[0].createdAt in before..after)
    }

    @Test
    fun deleteSection_setsChildEntrySectionIdToNull() = runBlocking {
        // Child entries must become free-floating (sectionId = null) after their section is deleted.
        val sectionId = sectionRepo.addSection("Delete me")
        entryRepo.addEntry(title = "Child entry", body = "", sectionId = sectionId)
        val section = sectionRepo.getAllSections().first().find { it.id == sectionId }!!
        sectionRepo.deleteSection(section)
        val allEntries = entryRepo.getAllEntries().first()
        assertEquals(1, allEntries.size)
        assertNull(allEntries[0].sectionId)
    }

    @Test
    fun deleteSection_doesNotDeleteChildEntries() = runBlocking {
        // Entries must still exist in the database after their parent section is deleted.
        val sectionId = sectionRepo.addSection("Delete me")
        entryRepo.addEntry(title = "Child 1", body = "", sectionId = sectionId)
        entryRepo.addEntry(title = "Child 2", body = "", sectionId = sectionId)
        val section = sectionRepo.getAllSections().first().find { it.id == sectionId }!!
        sectionRepo.deleteSection(section)
        val allEntries = entryRepo.getAllEntries().first()
        assertEquals(2, allEntries.size)
    }

    @Test
    fun deleteSection_removesSection() = runBlocking {
        val sectionId = sectionRepo.addSection("To remove")
        val section = sectionRepo.getAllSections().first().find { it.id == sectionId }!!
        sectionRepo.deleteSection(section)
        val sections = sectionRepo.getAllSections().first()
        assertTrue(sections.none { it.id == sectionId })
    }

    @Test
    fun reorderSections_updatesPositions() = runBlocking {
        sectionRepo.addSection("A")
        sectionRepo.addSection("B")
        sectionRepo.addSection("C")
        val all = sectionRepo.getAllSections().first()
        // Reorder as C, A, B
        val c = all.find { it.name == "C" }!!
        val a = all.find { it.name == "A" }!!
        val b = all.find { it.name == "B" }!!
        sectionRepo.reorderSections(listOf(c, a, b))
        val reordered = sectionRepo.getAllSections().first()
        assertEquals("C", reordered[0].name)
        assertEquals("A", reordered[1].name)
        assertEquals("B", reordered[2].name)
    }
}
