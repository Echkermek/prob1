// com/example/prob1/data/database/dao/CourseDao.kt
package com.example.prob1.data.database.dao

import androidx.room.*
import com.example.prob1.data.database.entities.CourseEntity
import com.example.prob1.data.database.entities.CourseGroupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface CourseDao {

    // ==================== Курсы ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: String): CourseEntity?

    @Query("SELECT * FROM courses WHERE groupId = :groupId")
    suspend fun getCourseByGroupId(groupId: String): CourseEntity?

    @Query("SELECT * FROM courses ORDER BY name")
    suspend fun getAllCourses(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE semester = :semester")
    suspend fun getCoursesBySemester(semester: Int): List<CourseEntity>

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourseById(courseId: String)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    fun observeCourseById(courseId: String): Flow<CourseEntity?> =
        observeAllCourses().map { list -> list.find { it.id == courseId } }

    // ==================== CourseGroup (связи курс-группа) ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseGroup(courseGroup: CourseGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseGroups(courseGroups: List<CourseGroupEntity>)

    @Delete
    suspend fun deleteCourseGroup(courseGroup: CourseGroupEntity)

    @Query("SELECT * FROM course_groups WHERE groupId = :groupId")
    suspend fun getCourseGroupByGroupId(groupId: String): CourseGroupEntity?

    @Query("SELECT * FROM course_groups WHERE courseId = :courseId")
    suspend fun getCourseGroupsByCourseId(courseId: String): List<CourseGroupEntity>

    @Query("SELECT * FROM course_groups")
    suspend fun getAllCourseGroups(): List<CourseGroupEntity>

    @Query("DELETE FROM course_groups WHERE groupId = :groupId")
    suspend fun deleteCourseGroupByGroupId(groupId: String)

    @Query("DELETE FROM course_groups WHERE courseId = :courseId")
    suspend fun deleteCourseGroupsByCourseId(courseId: String)

    @Query("DELETE FROM course_groups")
    suspend fun deleteAllCourseGroups()

    // ==================== Flow (реактивные запросы) ====================

    @Query("SELECT * FROM courses ORDER BY name")
    fun observeAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM course_groups WHERE groupId = :groupId")
    fun observeCourseGroupByGroupId(groupId: String): Flow<CourseGroupEntity?>
}