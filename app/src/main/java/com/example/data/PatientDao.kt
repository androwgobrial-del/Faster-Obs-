package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE postId = :postId ORDER BY createdAt DESC")
    fun getPatientsForPost(postId: Int): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY createdAt ASC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Int): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)

    @Query("DELETE FROM patients WHERE postId = :postId")
    suspend fun deleteAllPatientsForPost(postId: Int)

    @Query("UPDATE patients SET isSelectedForPrint = :isSelected WHERE postId = :postId")
    suspend fun updateAllSelectionsForPost(postId: Int, isSelected: Boolean)

    @Query("UPDATE patients SET isSelectedForPrint = :isSelected WHERE id = :id")
    suspend fun updateSelection(id: Int, isSelected: Boolean)
}
