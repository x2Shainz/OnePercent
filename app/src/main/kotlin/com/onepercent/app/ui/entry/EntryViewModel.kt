package com.onepercent.app.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onepercent.app.data.repository.EntryRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for the [EntryScreen].
 *
 * Loads the entry identified by [entryId], exposes its [title] and [body] as mutable flows,
 * and auto-saves changes to the repository after a 500 ms debounce whenever the content changes
 * post-load. [saveNow] provides an immediate (non-debounced) save for use as a safety net.
 */
@OptIn(FlowPreview::class)
class EntryViewModel(
    private val entryRepository: EntryRepository,
    private val entryId: Long
) : ViewModel() {

    private val _title = MutableStateFlow("")
    private val _body   = MutableStateFlow("")

    val title: StateFlow<String> = _title
    val body:  StateFlow<String> = _body

    /** True once the initial load from the DB has completed; guards [saveNow]. */
    private var loaded = false

    init {
        viewModelScope.launch {
            // Load the entry once and populate the UI state.
            val entry = entryRepository.getEntryById(entryId)
                .filterNotNull()
                .first()
            _title.value = entry.title
            _body.value  = entry.body
            loaded = true

            // Auto-save: collect changes after the load so the just-loaded values don't
            // trigger an unnecessary write. drop(1) skips the current emission at subscription
            // time (the values we just set above), so only genuine user edits cause saves.
            combine(_title, _body) { t, b -> Pair(t, b) }
                .drop(1)
                .debounce(500)
                .collect { (t, b) -> entryRepository.updateEntry(entryId, t, b) }
        }
    }

    fun onTitleChange(value: String) { _title.value = value }
    fun onBodyChange(value: String)  { _body.value  = value }

    /**
     * Immediately persists the current [title] and [body] without waiting for the debounce.
     * Called by [EntryScreen]'s `DisposableEffect.onDispose` so changes are not lost when the
     * user navigates back before the 500 ms debounce fires.
     */
    fun saveNow() {
        if (!loaded) return
        viewModelScope.launch {
            entryRepository.updateEntry(entryId, _title.value, _body.value)
        }
    }

    class Factory(
        private val entryRepository: EntryRepository,
        private val entryId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EntryViewModel(entryRepository, entryId) as T
    }
}
