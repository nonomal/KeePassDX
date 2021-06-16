package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpElement
import java.util.*


class EntryViewModel: ViewModel() {

    private val mDatabase: Database? = Database.getInstance()

    private var mEntry: Entry? = null
    private var mLastEntryVersion: Entry? = null
    private var mHistoryPosition: Int = -1

    val entryInfo : LiveData<EntryInfo> get() = _entryInfo
    private val _entryInfo = MutableLiveData<EntryInfo>()

    val entryIsHistory : LiveData<Boolean> get() = _entryIsHistory
    private val _entryIsHistory = MutableLiveData<Boolean>()

    val entryHistory : LiveData<List<Entry>> get() = _entryHistory
    private val _entryHistory = MutableLiveData<List<Entry>>()

    val onOtpElementUpdated : LiveData<OtpElement> get() = _onOtpElementUpdated
    private val _onOtpElementUpdated = SingleLiveEvent<OtpElement>()

    val attachmentSelected : LiveData<Attachment> get() = _attachmentSelected
    private val _attachmentSelected = SingleLiveEvent<Attachment>()
    val onAttachmentAction : LiveData<EntryAttachmentState?> get() = _onAttachmentAction
    private val _onAttachmentAction = MutableLiveData<EntryAttachmentState?>()

    val historySelected : LiveData<EntryHistory> get() = _historySelected
    private val _historySelected = SingleLiveEvent<EntryHistory>()

    fun loadEntry(entryId: NodeId<UUID>, historyPosition: Int) {
        IOActionTask(
            {
                // Manage current version and history
                mLastEntryVersion = mDatabase?.getEntryById(entryId)
                mEntry = if (historyPosition > -1) {
                    mLastEntryVersion?.getHistory()?.get(historyPosition)
                } else {
                    mLastEntryVersion
                }
                mHistoryPosition = historyPosition
                createEntryInfoHistory(mEntry)
            },
            { entryInfoHistory ->
                if (entryInfoHistory != null) {
                    _entryInfo.value = entryInfoHistory.entryInfo
                    _entryIsHistory.value = mHistoryPosition != -1
                    _entryHistory.value = entryInfoHistory.entryHistory
                }
            }
        ).execute()
    }

    fun updateEntry() {
        mEntry?.nodeId?.let { nodeId ->
            loadEntry(nodeId, mHistoryPosition)
        }
    }

    private fun createEntryInfoHistory(entry: Entry?): EntryInfoHistory? {
        if (entry != null) {
            // To simplify template field visibility
            mDatabase?.decodeEntryWithTemplateConfiguration(entry)?.let {
                // To update current modification time
                it.touch(modified = false, touchParents = false)
                return EntryInfoHistory(it.getEntryInfo(mDatabase), it.getHistory())
            }
        }
        return null
    }

    // TODO Remove
    fun getEntry(): Entry? {
        return mEntry
    }

    // TODO Remove
    fun getMainEntry(): Entry? {
        return mLastEntryVersion
    }

    // TODO Remove
    fun getEntryHistoryPosition(): Int {
        return mHistoryPosition
    }

    // TODO Remove
    fun getEntryIsHistory(): Boolean {
        return entryIsHistory.value ?: false
    }

    fun onOtpElementUpdated(optElement: OtpElement) {
        _onOtpElementUpdated.value = optElement
    }

    fun onAttachmentSelected(attachment: Attachment) {
        _attachmentSelected.value = attachment
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        _onAttachmentAction.value = entryAttachmentState
    }

    fun onHistorySelected(item: Entry, position: Int) {
        _historySelected.value = EntryHistory(item.nodeId, item, null, position)
    }

    data class EntryInfoHistory(val entryInfo: EntryInfo,
                                val entryHistory: List<Entry>)
    // Custom data class to manage entry to retrieve and define is it's an history item (!= -1)
    data class EntryHistory(var nodeIdUUID: NodeId<UUID>?,
                            var entry: Entry?,
                            var lastEntryVersion: Entry?,
                            var historyPosition: Int = -1)

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}