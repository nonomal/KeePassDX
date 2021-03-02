package com.kunzisoft.keepass.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.tasks.BinaryStreamManager.resizeBitmapAndStoreDataInBinaryFile
import kotlinx.coroutines.*
import java.io.File


class IconPickerViewModel: ViewModel() {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    val iconStandardSelected: MutableLiveData<IconImageStandard> by lazy {
        MutableLiveData<IconImageStandard>()
    }

    val iconCustomSelected: MutableLiveData<IconImageCustom> by lazy {
        MutableLiveData<IconImageCustom>()
    }

    val iconCustomAdded: MutableLiveData<IconImageCustom> by lazy {
        MutableLiveData<IconImageCustom>()
    }

    fun selectIconStandard(icon: IconImageStandard) {
        iconStandardSelected.value = icon
    }

    fun selectIconCustom(icon: IconImageCustom) {
        iconCustomSelected.value = icon
    }

    fun addCustomIcon(database: Database,
                      contentResolver: ContentResolver,
                      iconDir: File,
                      iconToUploadUri: Uri) {
        mainScope.launch {
            withContext(Dispatchers.IO) {
                // on Progress with thread
                val asyncResult: Deferred<IconImageCustom?> = async {
                    database.buildNewCustomIcon(iconDir)?.let { customIcon ->
                        resizeBitmapAndStoreDataInBinaryFile(contentResolver,
                                iconToUploadUri, customIcon.binaryFile)
                        customIcon
                    }
                }
                withContext(Dispatchers.Main) {
                    asyncResult.await()?.let { customIcon ->
                        iconCustomAdded.value = customIcon
                        // Remove icon if data cannot be saved
                        if (customIcon.binaryFile.length <= 0) {
                            database.removeCustomIcon(customIcon.uuid)
                        }
                    }
                }
            }
        }
    }
}