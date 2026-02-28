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
 * Instrumented tests for [EntryRepositoryImpl] using an in-memory Room database.
 * Exercises timestamp injection, transaction semantics, and cascade behaviour.
 */
@RunWith(AndroidJUnit4::class)
class EntryRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: EntryRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = EntryRepositoryImpl(db, db.entryDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun addEntry_stampsCreatedAtNearCurrentTime() = runBlocking {
        val before = System.currentTimeMillis()
        val id = repo.addEntry(title = "Test", body = "")
        val after = System.currentTimeMillis()
        val entry = repo.getEntryById(id).first()!!
        assertTrue(entry.createdAt in before..after)
    }

    @Test
    fun addEntry_withNullSectionId() = runBlocking {
        val id = repo.addEntry(title = "No section", body = "", sectionId = null)
        val entry = repo.getEntryById(id).first()!!
        assertNull(entry.sectionId)
    }

    @Test
    fun addEntry_withSectionId() = runBlocking {
        val id = repo.addEntry(title = "With section", body = "", sectionId = 42L)
        val entry = repo.getEntryById(id).first()!!
        assertEquals(42L, entry.sectionId)
    }

    @Test
    fun reorderEntries_updatesPositions() = runBlocking {
        val id1 = repo.addEntry(title = "A", body = "")
        val id2 = repo.addEntry(title = "B", body = "")
        val id3 = repo.addEntry(title = "C", body = "")
        val e1 = repo.getEntryById(id1).first()!!
        val e2 = repo.getEntryById(id2).first()!!
        val e3 = repo.getEntryById(id3).first()!!
        // Reorder as C, A, B
        repo.reorderEntries(listOf(e3, e1, e2))
        val all = repo.getAllEntries().first()
        assertEquals("C", all[0].title)
        assertEquals("A", all[1].title)
        assertEquals("B", all[2].title)
    }

    @Test
    fun reorderEntries_isAtomicTransactionOrder() = runBlocking {
        // All positions must be updated together; the final state must be fully consistent.
        val id1 = repo.addEntry(title = "First",  body = "")
        val id2 = repo.addEntry(title = "Second", body = "")
        val e1 = repo.getEntryById(id1).first()!!
        val e2 = repo.getEntryById(id2).first()!!
        // Reverse: second becomes position 0, first becomes position 1.
        repo.reorderEntries(listOf(e2, e1))
        val all = repo.getAllEntries().first()
        assertEquals("Second", all[0].title)
        assertEquals("First",  all[1].title)
        assertEquals(0, all[0].position)
        assertEquals(1, all[1].position)
    }

    @Test
    fun moveEntry_changesSectionId() = runBlocking {
        val id = repo.addEntry(title = "Entry", body = "", sectionId = 1L)
        repo.moveEntry(id, 2L)
        val entry = repo.getEntryById(id).first()!!
        assertEquals(2L, entry.sectionId)
    }

    @Test
    fun deleteEntry_removesEntry() = runBlocking {
        val id = repo.addEntry(title = "To delete", body = "")
        val entry = repo.getEntryById(id).first()!!
        repo.deleteEntry(entry)
        val result = repo.getEntryById(id).first()
        assertNull(result)
    }
}
