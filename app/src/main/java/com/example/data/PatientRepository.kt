package com.example.data

import kotlinx.coroutines.flow.Flow

class PatientRepository(
    private val patientDao: PatientDao,
    private val postDao: PostDao
) {
    val allPosts: Flow<List<PostEntity>> = postDao.getAllPosts()
    val allPatients: Flow<List<PatientEntity>> = patientDao.getAllPatients()

    suspend fun insertPost(post: PostEntity): Long {
        return postDao.insertPost(post)
    }

    suspend fun updatePost(post: PostEntity) {
        postDao.updatePost(post)
    }

    suspend fun deletePost(post: PostEntity) {
        // Cascade delete patients of this post
        patientDao.deleteAllPatientsForPost(post.id)
        postDao.deletePost(post)
    }

    suspend fun getPostById(id: Int): PostEntity? {
        return postDao.getPostById(id)
    }

    fun getPatientsForPost(postId: Int): Flow<List<PatientEntity>> {
        return patientDao.getPatientsForPost(postId)
    }

    suspend fun getPatientById(id: Int): PatientEntity? {
        return patientDao.getPatientById(id)
    }

    suspend fun insertPatient(patient: PatientEntity): Long {
        return patientDao.insertPatient(patient)
    }

    suspend fun updatePatient(patient: PatientEntity) {
        patientDao.updatePatient(patient)
    }

    suspend fun deletePatient(patient: PatientEntity) {
        patientDao.deletePatient(patient)
    }

    suspend fun deleteAllPatientsForPost(postId: Int) {
        patientDao.deleteAllPatientsForPost(postId)
    }

    suspend fun updateAllSelectionsForPost(postId: Int, isSelected: Boolean) {
        patientDao.updateAllSelectionsForPost(postId, isSelected)
    }

    suspend fun updateSelection(id: Int, isSelected: Boolean) {
        patientDao.updateSelection(id, isSelected)
    }
}
