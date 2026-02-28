package com.onepercent.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onepercent.app.data.model.Entry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [EntryDao] using an in-memory Room database.
 * No Hilt needed — the DAO is exercised directly.
 */
@RunWith(AndroidJUnit4::class)
class EntryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var entryDao: EntryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        entryDao = db.entryDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun entry(
        title: String = "Title",
        body: String = "Body",
        sectionId: Long? = null,
        createdAt: Long = 1000L,
        position: Int = 0
    ) = Entry(title = title, body = body, sectionId = sectionId, createdAt = createdAt, position = position)

    @Test
    fun insertEntry_returnsGeneratedId() = runBlocking {
        val id = entryDao.insertEntry(entry(title = "Test"))
        assertTrue(id > 0L)
    }

    @Test
    fun updateEntry_changesOnlyTitleAndBody() = runBlocking {
        // sectionId, createdAt, and position must not be touched by updateEntry.
        val id = entryDao.insertEntry(entry(title = "Original", body = "Old body", sectionId = 5L, createdAt = 100L))
        entryDao.updateEntry(id, "Updated", "New body")
        val updated = entryDao.getEntryById(id).first()!!
        assertEquals("Updated", updated.title)
        assertEquals("New body", updated.body)
        assertEquals(5L, updated.sectionId)
        assertEquals(100L, updated.createdAt)
        assertEquals(0, updated.position)
    }

    @Test
    fun updateSection_changesSection() = runBlocking {
        val id = entryDao.insertEntry(entry(sectionId = 1L))
        entryDao.updateSection(id, 2L)
        val updated = entryDao.getEntryById(id).first()!!
        assertEquals(2L, updated.sectionId)
    }

    @Test
    fun updateSection_canSetToNull() = runBlocking {
        // Entries can be made free-floating by setting sectionId to null.
        val id = entryDao.insertEntry(entry(sectionId = 1L))
        entryDao.updateSection(id, null)
        val updated = entryDao.getEntryById(id).first()!!
        assertNull(updated.sectionId)
    }

    @Test
    fun deleteEntry_removesEntry() = runBlocking {
        val id = entryDao.insertEntry(entry(title = "To delete"))
        val toDelete = entryDao.getEntryById(id).first()!!
        entryDao.deleteEntry(toDelete)
        val result = entryDao.getEntryById(id).first()
        assertNull(result)
    }

    @Test
    fun clearSection_nullifiesChildEntries() = runBlocking {
        // All entries with the matching sectionId must have it set to null.
        entryDao.insertEntry(entry(title = "Child 1", sectionId = 10L))
        entryDao.insertEntry(entry(title = "Child 2", sectionId = 10L))
        entryDao.clearSection(10L)
        val all = entryDao.getAllEntries().first()
        assertTrue(all.all { it.sectionId == null })
    }

    @Test
    fun clearSection_doesNotAffectOtherSections() = runBlocking {
        // Entries in a different section must be unchanged.
        entryDao.insertEntry(entry(title = "Section 10 entry", sectionId = 10L))
        entryDao.insertEntry(entry(title = "Section 20 entry", sectionId = 20L))
        entryDao.clearSection(10L)
        val all = entryDao.getAllEntries().first()
        val section20Entry = all.find { it.title == "Section 20 entry" }
        assertNotNull(section20Entry)
        assertEquals(20L, section20Entry!!.sectionId)
    }

    @Test
    fun getEntryById_returnsCorrectEntry() = runBlocking {
        val id = entryDao.insertEntry(entry(title = "Specific entry"))
        val result = entryDao.getEntryById(id).first()
        assertNotNull(result)
        assertEquals("Specific entry", result!!.title)
    }

    @Test
    fun getEntryById_returnsNullForMissingId() = runBlocking {
        val result = entryDao.getEntryById(9999L).first()
        assertNull(result)
    }

    @Test
    fun getAllEntries_orderedByPositionAsc() = runBlocking {
        entryDao.insertEntry(entry(title = "Third",  position = 2))
        entryDao.insertEntry(entry(title = "First",  position = 0))
        entryDao.insertEntry(entry(title = "Second", position = 1))
        val all = entryDao.getAllEntries().first()
        assertEquals("First",  all[0].title)
        assertEquals("Second", all[1].title)
        assertEquals("Third",  all[2].title)
    }

    @Test
    fun getUnassignedEntries_returnsOnlyNullSection() = runBlocking {
        entryDao.insertEntry(entry(title = "Unassigned", sectionId = null))
        entryDao.insertEntry(entry(title = "Assigned",   sectionId = 1L))
        val unassigned = entryDao.getUnassignedEntries().first()
        assertEquals(1, unassigned.size)
        assertEquals("Unassigned", unassigned[0].title)
    }

    @Test
    fun getEntriesForSection_returnsOnlyThatSection() = runBlocking {
        entryDao.insertEntry(entry(title = "Section 1 entry", sectionId = 1L))
        entryDao.insertEntry(entry(title = "Section 2 entry", sectionId = 2L))
        val section1Entries = entryDao.getEntriesForSection(1L).first()
        assertEquals(1, section1Entries.size)
        assertEquals("Section 1 entry", section1Entries[0].title)
    }

    @Test
    fun updatePosition_affectsGetAllOrdering() = runBlocking {
        val id1 = entryDao.insertEntry(entry(title = "Entry A", position = 0))
        val id2 = entryDao.insertEntry(entry(title = "Entry B", position = 1))
        // Swap positions so B comes first.
        entryDao.updatePosition(id1, 1)
        entryDao.updatePosition(id2, 0)
        val all = entryDao.getAllEntries().first()
        assertEquals("Entry B", all[0].title)
        assertEquals("Entry A", all[1].title)
    }

    @Test
    fun searchEntries_matchesTitle() = runBlocking {
        entryDao.insertEntry(entry(title = "Shopping List", body = "Eggs"))
        val results = entryDao.searchEntries("Shopping").first()
        assertEquals(1, results.size)
        assertEquals("Shopping List", results[0].title)
    }

    @Test
    fun searchEntries_matchesBody() = runBlocking {
        entryDao.insertEntry(entry(title = "Notes", body = "Important meeting"))
        val results = entryDao.searchEntries("meeting").first()
        assertEquals(1, results.size)
        assertEquals("Notes", results[0].title)
    }

    @Test
    fun searchEntries_isCaseInsensitive() = runBlocking {
        entryDao.insertEntry(entry(title = "My Journal", body = ""))
        val results = entryDao.searchEntries("journal").first()
        assertEquals(1, results.size)
    }

    @Test
    fun searchEntries_returnsNewestFirst() = runBlocking {
        entryDao.insertEntry(entry(title = "Older", body = "note", createdAt = 1000L))
        entryDao.insertEntry(entry(title = "Newer", body = "note", createdAt = 2000L))
        val results = entryDao.searchEntries("note").first()
        assertEquals("Newer", results[0].title)
        assertEquals("Older", results[1].title)
    }

    @Test
    fun searchEntries_returnsEmptyForNoMatch() = runBlocking {
        entryDao.insertEntry(entry(title = "Something", body = "content"))
        val results = entryDao.searchEntries("xyz_not_matching_anything").first()
        assertTrue(results.isEmpty())
    }
}
