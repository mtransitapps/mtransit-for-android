package org.mtransit.android.common.repository

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

// inspired from: https://gist.github.com/florina-muntenescu/1c78858f286d196d545c038a71a3e864
interface BaseDao<T> {

    /**
     * Insert an object in the database.
     *
     * @param obj the object to be inserted.
     */
    @Insert
    fun insert(obj: T)

    /**
     * Insert an array of objects in the database.
     *
     * @param obj the objects to be inserted.
     */
    @Insert
    fun insertAll(vararg obj: T)

    /**
     * Update an object from the database.
     *
     * @param obj the object to be updated
     */
    @Update
    fun update(obj: T)

    /**
     * Update an array of objects in the database.
     *
     * @param obj the objects to be updated
     */
    @Update
    fun updateAll(vararg obj: T)

    /**
     * Delete an object from the database
     *
     * @param obj the object to be deleted
     */
    @Delete
    fun delete(obj: T)

    /**
     * Delete an array of objects from the database
     *
     * @param obj the objects to be deleted
     */
    @Delete
    fun deleteAll(vararg obj: T)
}