package org.mtransit.android.provider.favorite

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.EditText
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.POI
import org.mtransit.android.data.FavoriteFolder
import org.mtransit.android.data.TextMessage
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.ui.MTDialog

object FavoritesUI : MTLog.Loggable {

    private val LOG_TAG: String = FavoritesUI::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    fun generateFavEmptyFavPOI(context: Context, textMessageId: Long, @DataSourceTypeId.DataSourceType dataSourceTypeId: Int): POI {
        return TextMessage(textMessageId, dataSourceTypeId, context.getString(R.string.favorite_folder_empty))
    }

    suspend fun FavoriteRepository.addOrRemoveFavoriteUI(activity: Activity, fkId: String) = withContext(Dispatchers.IO) {
        val favorite = getFavorite(fkId)
        val isFavorite = favorite != null
        val favoriteFolders = findFoldersList()
        val usingFavoriteFolder = favoriteFolders.any { it.id != FavoriteFolder.DEFAULT_FOLDER_ID }
        if (!usingFavoriteFolder) {
            if (isFavorite) {
                deleteFavorite(fkId)
            } else {
                addFavorite(fkId)
            }
            return@withContext
        }
        val favoriteFolderId = favorite?.folderId
        val favoriteFolder = favoriteFolders.find { it.id == favoriteFolderId }
        withContext(Dispatchers.Main) {
            showPickFavoriteFolderDialog(
                activity,
                fkId,
                favoriteFolder,
                favoriteFolders,
            )
        }
    }

    @MainThread
    private fun FavoriteRepository.showPickFavoriteFolderDialog(
        activity: Activity,
        fkId: String,
        favoriteFolder: FavoriteFolder?,
        favoriteFolders: Collection<FavoriteFolder>,
    ) {
        val favoriteFolderId = favoriteFolder?.id ?: FavoriteFolder.INVALID_FOLDER_ID
        var initialWhich: Int
        var selectedWhich: Int
        var checkedItem = -1
        val itemsList = mutableListOf<String>()
        val itemsListId = mutableListOf<Int>()
        var i = 0
        itemsListId.add(FavoriteFolder.DEFAULT_FOLDER_ID)
        itemsList.add(activity.getString(R.string.favorite_folder_default))
        if (favoriteFolderId != FavoriteFolder.INVALID_FOLDER_ID && favoriteFolderId == FavoriteFolder.DEFAULT_FOLDER_ID) {
            checkedItem = i
        }
        i++
        val favoriteFoldersList = favoriteFolders.sortedWith(FavoriteFolder.NAME_COMPARATOR)
        for (favoriteFolder in favoriteFoldersList) {
            if (favoriteFolder.id == FavoriteFolder.DEFAULT_FOLDER_ID) continue
            itemsListId.add(favoriteFolder.id)
            itemsList.add(favoriteFolder.name)
            if (favoriteFolderId != FavoriteFolder.INVALID_FOLDER_ID && favoriteFolderId == favoriteFolder.id) {
                checkedItem = i
            }
            i++
        }
        val newFolderId = Int.MAX_VALUE
        itemsListId.add(newFolderId)
        itemsList.add(activity.getString(R.string.favorite_folder_new))
        i++
        val removeFavoriteId = newFolderId - 1
        if (favoriteFolderId != FavoriteFolder.INVALID_FOLDER_ID) {
            itemsListId.add(removeFavoriteId)
            itemsList.add(activity.getString(R.string.favorite_remove))
            @Suppress("AssignedValueIsNeverRead") // TODO ???
            i++
        }
        initialWhich = checkedItem
        if (checkedItem < 0) {
            checkedItem = 0 // (default choice when adding favorite)
        }
        selectedWhich = checkedItem
        var lifecycleOwner: LifecycleOwner? = null
        MTDialog.Builder(activity)
            .setTitle(R.string.favorite_folder_pick)
            .setSingleChoiceItems(
                itemsList.toTypedArray(),
                checkedItem
            ) { _: DialogInterface?, which: Int ->
                selectedWhich = which
            }
            .setPositiveButton(R.string.favorite_folder_pick_ok) { _: DialogInterface?, _: Int ->
                if (selectedWhich < 0 || selectedWhich == initialWhich) {
                    return@setPositiveButton
                }
                lifecycleOwner?.lifecycleScope?.launch {
                    val selectedFavoriteFolderId = itemsListId[selectedWhich]
                    if (selectedFavoriteFolderId == newFolderId) { // create new folder
                        showAddFolderDialog(activity, fkId, favoriteFolder, parentLifecycleOwner = lifecycleOwner)
                    } else if (selectedFavoriteFolderId == removeFavoriteId) { // delete favorite
                        val updated = deleteFavorite(fkId)
                        if (updated) {
                            if (favoriteFolder != null) {
                                showToast(activity, R.string.favorite_removed_from_folder_and_folder, favoriteFolder.name)
                            }
                            showToast(activity, R.string.favorite_removed)
                        }
                    } else if (favoriteFolderId != FavoriteFolder.INVALID_FOLDER_ID) { // move favorite
                        val updated = updateFavoriteFolder(fkId, selectedFavoriteFolderId)
                        if (updated) {
                            val selectedFavoriteFolder = findFolder(selectedFavoriteFolderId)
                            if (selectedFavoriteFolder != null) {
                                showToast(activity, R.string.favorite_moved_to_folder_and_folder, selectedFavoriteFolder.name)
                            } else {
                                showToast(activity, R.string.favorite_moved)
                            }
                        }
                    } else { // add new favorite
                        val added = addFavorite(fkId, selectedFavoriteFolderId)
                        if (added) {
                            val selectedFavoriteFolder = findFolder(selectedFavoriteFolderId)
                            if (selectedFavoriteFolder != null) {
                                showToast(activity, R.string.favorite_added_to_folder_and_folder, selectedFavoriteFolder.name)
                            } else {
                                showToast(activity, R.string.favorite_added)
                            }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.favorite_folder_pick_cancel) { dialog: DialogInterface?, _: Int ->
                dialog?.cancel()
            }
            .create()
            .also { lifecycleOwner = it }
            .show()
    }

    @MainThread
    fun FavoriteRepository.showAddFolderDialog(
        activity: Activity,
        updatedFkId: String? = null,
        updatedFavoriteFolder: FavoriteFolder? = null,
        parentLifecycleOwner: LifecycleOwner? = null,
    ) {
        @SuppressLint("InflateParams") // dialog
        val view = LayoutInflater.from(activity).inflate(R.layout.layout_favorites_folder_edit, null, false)
        val newFolderNameTv = view.findViewById<EditText>(R.id.folder_name)
        var lifecycleOwner: LifecycleOwner? = parentLifecycleOwner
        MTDialog.Builder(activity)
            .setView(view)
            .setPositiveButton(R.string.favorite_folder_new_create) { _: DialogInterface?, _: Int ->
                lifecycleOwner?.lifecycleScope?.launch {
                    val newFolderName = newFolderNameTv.getText().toString()
                    if (newFolderName.isBlank()) {
                        showToast(activity, R.string.favorite_folder_new_invalid_name)
                        return@launch
                    }
                    val createdFolder = addFolder(newFolderName)
                    if (createdFolder == null) {
                        showToast(activity, R.string.favorite_folder_new_creation_error_and_folder_name, newFolderName)
                        return@launch
                    }
                    showToast(activity, R.string.favorite_folder_new_created_and_folder_name, createdFolder.name)
                    if (!updatedFkId.isNullOrEmpty()) {
                        if (updatedFavoriteFolder != null) { // move favorite
                            val updated = updateFavoriteFolder(updatedFkId, createdFolder.id)
                            if (updated) {
                                showToast(activity, R.string.favorite_moved_to_folder_and_folder, createdFolder.name)
                            }
                        } else { // add new favorite
                            val added = addFavorite(updatedFkId, createdFolder.id)
                            if (added) {
                                showToast(activity, R.string.favorite_added_to_folder_and_folder, createdFolder.name)
                            }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.favorite_folder_new_cancel) { dialog: DialogInterface?, _: Int ->
                dialog?.cancel()
            }
            .create()
            .also {
                if (lifecycleOwner != null) return@also
                lifecycleOwner = it
            }
            .show()
    }

    @MainThread
    @JvmStatic
    fun FavoriteRepository.showDeleteFolderDialog(
        activity: Activity?,
        favoriteFolder: FavoriteFolder,
    ) {
        if (activity == null || activity.isFinishing) return // SKIP
        var lifecycleOwner: LifecycleOwner? = null
        MTDialog.Builder(activity)
            .setTitle(activity.getString(R.string.favorite_folder_deletion_confirmation_title_and_name, favoriteFolder.name))
            .setMessage(activity.getString(R.string.favorite_folder_deletion_confirmation_text_and_name, favoriteFolder.name))
            .setPositiveButton(R.string.delete) { _: DialogInterface?, _: Int ->
                lifecycleOwner?.lifecycleScope?.launch {
                    val deleted = deleteFolder(favoriteFolder)
                    if (deleted) {
                        showToast(activity, R.string.favorite_folder_deleted_and_folder_name, favoriteFolder.name)
                    } else {
                        showToast(activity, R.string.favorite_folder_deletion_error_and_folder_name, favoriteFolder.name)
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { dialog: DialogInterface?, _: Int ->
                dialog?.cancel()
            }
            .create()
            .also { lifecycleOwner = it }
            .show()
    }

    @MainThread
    @JvmStatic
    fun FavoriteRepository.showUpdateFolderDialog(
        activity: Activity?,
        layoutInflater: LayoutInflater,
        favoriteFolder: FavoriteFolder,
    ) {
        if (activity == null || activity.isFinishing) return  // SKIP
        @SuppressLint("InflateParams") // dialog
        val editView = layoutInflater.inflate(R.layout.layout_favorites_folder_edit, null)
        val newFolderNameTv = editView.findViewById<EditText>(R.id.folder_name)
        newFolderNameTv.setText(favoriteFolder.name)
        var lifecycleOwner: LifecycleOwner? = null
        MTDialog.Builder(activity)
            .setView(editView)
            .setPositiveButton(R.string.favorite_folder_edit) { _: DialogInterface?, _: Int ->
                lifecycleOwner?.lifecycleScope?.launch {
                    val newFolderName = newFolderNameTv.getText().toString()
                    if (newFolderName.isEmpty()) {
                        showToast(activity, R.string.favorite_folder_new_invalid_name)
                        return@launch
                    }
                    val updated = updateFolder(favoriteFolder.copy(name = newFolderName))
                    if (updated) {
                        showToast(activity, R.string.favorite_folder_edited_and_folder, newFolderName)
                    }
                }
            }
            .setNegativeButton(R.string.favorite_folder_new_cancel) { dialog: DialogInterface?, _: Int ->
                dialog?.cancel()
            }
            .create()
            .also { lifecycleOwner = it }
            .show()
    }

    private suspend fun showToast(activity: Activity, @StringRes resId: Int) = withContext(Dispatchers.Main) {
        ToastUtils.makeTextAndShowCentered(activity, resId)
    }

    private suspend fun showToast(activity: Activity, @StringRes resId: Int, vararg args: Any) = withContext(Dispatchers.Main) {
        ToastUtils.makeTextAndShowCentered(activity, activity.getString(resId, *args))
    }
}
