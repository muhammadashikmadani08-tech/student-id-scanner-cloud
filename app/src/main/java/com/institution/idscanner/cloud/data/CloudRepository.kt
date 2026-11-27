package com.institution.idscanner.cloud.data

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed repository. All data lives under users/{uid}/, so one
 * Gmail (Firebase UID) can never see another user's students, attendance
 * or history. Snapshot listeners keep every screen live: a change made on
 * one phone appears on another phone signed in with the same account as
 * soon as it syncs. Firestore's offline persistence (enabled in
 * MainActivity) serves the last synced data when there is no internet.
 */
class CloudRepository(val uid: String) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private fun userDoc() = db.collection("users").document(uid)
    private fun studentsRef() = userDoc().collection("students")
    private fun attendanceRef() = userDoc().collection("attendance")
    private fun historyRef() = userDoc().collection("history")
    private fun holidaysRef() = userDoc().collection("holidays")
    private fun guardianLinksRef() = userDoc().collection("guardianLinks")
    private fun profileRef() = userDoc().collection("settings").document("profile")

    val allStudents: Flow<List<Student>> = callbackFlow {
        val reg = studentsRef().orderBy("name").addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(Student::class.java)?.also {
                    if (it.studentId.isBlank()) it.studentId = doc.id
                }
            }.orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    val history: Flow<List<ScanHistory>> = callbackFlow {
        val reg = historyRef().orderBy("scannedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(ScanHistory::class.java)?.also { it.id = doc.id }
                }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Firestore has no LIKE search: filter the live list in memory. */
    fun search(query: String): Flow<List<Student>> {
        val q = query.trim()
        return allStudents.map { list ->
            if (q.isBlank()) list
            else list.filter {
                it.studentId.contains(q, ignoreCase = true) ||
                    it.name.contains(q, ignoreCase = true) ||
                    it.className.contains(q, ignoreCase = true) ||
                    it.roll.contains(q, ignoreCase = true)
            }
        }
    }

    fun attendanceFor(studentId: String): Flow<List<AttendanceRecord>> = callbackFlow {
        val reg = attendanceRef().whereEqualTo("studentId", studentId)
            .orderBy("date").addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(AttendanceRecord::class.java)?.also { it.id = doc.id }
                }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** One-shot read of every attendance record (used by CSV export). */
    suspend fun getAllAttendance(): List<AttendanceRecord> {
        return attendanceRef().get().await().documents.mapNotNull { doc ->
            doc.toObject(AttendanceRecord::class.java)?.also { it.id = doc.id }
        }
    }

    /** Normalises a scanned value (trims spaces, upper-cases) before lookup. */
    suspend fun findByScanned(raw: String): Student? {
        val id = raw.trim().uppercase()
        return getStudent(id) ?: getStudent(raw.trim())
    }

    suspend fun getStudent(id: String): Student? {
        val doc = studentsRef().document(id).get().await()
        if (!doc.exists()) return null
        return doc.toObject(Student::class.java)?.also {
            if (it.studentId.isBlank()) it.studentId = doc.id
        }
    }

    suspend fun recordScan(studentId: String) {
        historyRef().add(ScanHistory(studentId = studentId)).await()
    }

    /** Records today's attendance (Asia/Dhaka). The composite document id
     *  makes repeat scans on the same day idempotent. */
    suspend fun markAttendance(studentId: String, nowMillis: Long = System.currentTimeMillis()) {
        val date = dateString(nowMillis)
        val docId = "${studentId}_$date"
        attendanceRef().document(docId)
            .set(AttendanceRecord(studentId = studentId, date = date, scannedAt = nowMillis))
            .await()
    }

    /**
     * Saves a student. When [newPhotoUri] is given, the picked photo is
     * uploaded to Firebase Storage first and its download URL is stored as
     * [Student.photoUrl]. Storage is NOT provisioned on the free Spark
     * plan, so when the upload returns null (Storage unavailable/offline)
     * the photo is instead copied into the app's internal storage and
     * that absolute file path is stored — the photo then works on this
     * phone only and does not sync to other phones. When it is null,
     * any existing photoUrl is kept.
     */
    suspend fun upsert(student: Student, newPhotoUri: Uri? = null, context: Context? = null): Student {
        var toSave = student
        if (newPhotoUri != null && context != null) {
            val url = uploadPhoto(student.studentId, newPhotoUri, context)
                ?: savePhotoLocally(student.studentId, newPhotoUri, context)
            if (url != null) toSave = student.copy(photoUrl = url)
        }
        // Capture the previously stored phone BEFORE overwriting, so a
        // phone change can move the guardian link (see below). A failed
        // read must not block the save: fall back to "no previous phone"
        // and still add the new link.
        val previousPhone = runCatching { getStudent(toSave.studentId)?.phone }.getOrNull()
        studentsRef().document(toSave.studentId).set(toSave).await()
        syncGuardianLinkOnSave(toSave, previousPhone)
        return toSave
    }

    suspend fun upsertAll(students: List<Student>) {
        // Capture each student's previously stored phone BEFORE the
        // batch overwrites, so phone changes move the guardian links.
        // A failed read must not block the import: that student is then
        // treated as new (its new link is still added).
        val withPrevious = students.map { student ->
            student to runCatching { getStudent(student.studentId)?.phone }.getOrNull()
        }
        // WriteBatch caps at 500 writes; chunk larger imports.
        withPrevious.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (student, _) -> batch.set(studentsRef().document(student.studentId), student) }
            batch.commit().await()
        }
        withPrevious.forEach { (student, previousPhone) ->
            syncGuardianLinkOnSave(student, previousPhone)
        }
    }

    suspend fun delete(student: Student, context: Context? = null) {
        // Use the stored phone (authoritative) when it can be read, so
        // the guardian link is cleaned up even if the passed-in copy is
        // stale; fall back to the passed-in phone.
        val storedPhone = runCatching { getStudent(student.studentId)?.phone }.getOrNull()
        studentsRef().document(student.studentId).delete().await()
        removeFromGuardianLink(normalizeGuardianPhone(storedPhone ?: student.phone), student.studentId)
        deletePhoto(student, context)
    }

    /** Deletes the student's Storage photo, if the stored URL points there,
     *  and the local fallback photo file, if one was saved on this phone. */
    suspend fun deletePhoto(student: Student, context: Context? = null) {
        if (context != null) {
            try {
                localPhotoFile(student.studentId, context).delete()
            } catch (_: Exception) {
                // A missing photo file must not block the delete.
            }
        }
        val url = student.photoUrl
        if (url.isBlank()) return
        if (url.startsWith("/")) {
            // Local fallback path: delete the file itself, no Storage call.
            try {
                java.io.File(url).delete()
            } catch (_: Exception) {
            }
            return
        }
        try {
            if (url.contains("/photos/") || url.contains("firebasestorage")) {
                storage.getReferenceFromUrl(url).delete().await()
            }
        } catch (_: Exception) {
            // A missing or already-deleted photo must not block the delete.
        }
    }

    suspend fun deleteHistory(id: String) {
        historyRef().document(id).delete().await()
    }

    suspend fun clearHistory() {
        val docs = historyRef().get().await().documents
        docs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    /** Uploads a picked photo to users/{uid}/photos/{studentId}.jpg. */
    suspend fun uploadPhoto(studentId: String, uri: Uri, context: Context): String? {
        return try {
            val ref = storage.reference
                .child("users/$uid/photos/$studentId.jpg")
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            stream.use { ref.putStream(it).await() }
            ref.downloadUrl.await().toString()
        } catch (_: Exception) {
            null
        }
    }

    /** Local fallback photo file: filesDir/photos/{studentId}.jpg */
    private fun localPhotoFile(studentId: String, context: Context): java.io.File {
        val dir = java.io.File(context.filesDir, "photos")
        return java.io.File(dir, "$studentId.jpg")
    }

    /**
     * Local photo fallback, used when the Storage upload fails (Storage
     * is not provisioned on the free Spark plan). Copies the picked
     * image into internal storage and returns its absolute path, or null
     * if the copy fails — the caller then keeps the previous photoUrl.
     */
    private fun savePhotoLocally(studentId: String, uri: Uri, context: Context): String? {
        return try {
            val outFile = localPhotoFile(studentId, context)
            outFile.parentFile?.mkdirs()
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { src ->
                outFile.outputStream().use { dst -> src.copyTo(dst) }
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    // ---- Holidays ----

    /** Live list of holidays, oldest date first. Document id = date. */
    val holidays: Flow<List<Holiday>> = callbackFlow {
        val reg = holidaysRef().orderBy("date").addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(Holiday::class.java)?.also {
                    if (it.date.isBlank()) it.date = doc.id
                }
            }.orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    /** Adds (or overwrites, for the same date) a holiday. The caller
     *  validates that [holiday.date] is "yyyy-MM-dd" and the title is
     *  non-blank; the date is stored both as document id and field. */
    suspend fun addHoliday(holiday: Holiday) {
        val date = holiday.date.trim()
        holidaysRef().document(date).set(Holiday(date = date, title = holiday.title.trim())).await()
    }

    suspend fun deleteHoliday(date: String) {
        holidaysRef().document(date.trim()).delete().await()
    }

    // ---- Guardian links (users/{uid}/guardianLinks/{E.164 phone}) ----
    //
    // Kept in sync with Student.phone by every save/delete path in this
    // repository (upsert, upsertAll, delete, and the first-login seed in
    // ensureProfile). No student, attendance or history schema changes:
    // the link doc only mirrors {phone, studentIds[]}.

    /**
     * Keeps the guardian link(s) correct after a student save.
     * [previousPhone] is the phone stored before this save (null for a
     * new student): when it differs from the new phone, the student id
     * is removed from the old phone's link and added to the new one.
     * A blank new phone only removes the old link.
     */
    private suspend fun syncGuardianLinkOnSave(student: Student, previousPhone: String?) {
        val oldPhone = normalizeGuardianPhone(previousPhone)
        val newPhone = normalizeGuardianPhone(student.phone)
        if (oldPhone != null && oldPhone != newPhone) {
            removeFromGuardianLink(oldPhone, student.studentId)
        }
        if (newPhone != null) {
            addToGuardianLink(newPhone, student.studentId)
        }
    }

    /** Merges [studentId] into the link doc for [phone], creating the
     *  doc ({phone, studentIds[]}) when it does not exist yet.
     *  arrayUnion is idempotent, so re-saving the same student is safe. */
    private suspend fun addToGuardianLink(phone: String, studentId: String) {
        guardianLinksRef().document(phone).set(
            mapOf(
                "phone" to phone,
                "studentIds" to FieldValue.arrayUnion(studentId)
            ),
            SetOptions.merge()
        ).await()
    }

    /** Removes [studentId] from the link doc for [phone], deleting the
     *  doc when its studentIds array becomes empty. A missing doc is
     *  not an error (nothing to remove). */
    private suspend fun removeFromGuardianLink(phone: String?, studentId: String) {
        if (phone.isNullOrBlank()) return
        val ref = guardianLinksRef().document(phone)
        try {
            ref.update("studentIds", FieldValue.arrayRemove(studentId)).await()
        } catch (_: Exception) {
            // No such link doc: nothing to remove.
            return
        }
        val snap = ref.get().await()
        val remaining = snap.get("studentIds") as? List<*>
        if (!snap.exists() || remaining.isNullOrEmpty()) {
            ref.delete().await()
        }
    }

    // ---- Profile (institution name + admin PIN, per user) ----

    val profile: Flow<UserProfile> = callbackFlow {
        val reg = profileRef().addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(UserProfile::class.java) ?: UserProfile())
        }
        awaitClose { reg.remove() }
    }

    suspend fun setInstitutionName(name: String) {
        profileRef().set(
            mapOf("institutionName" to name),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun setAdminPin(pin: String) {
        profileRef().set(
            mapOf("adminPin" to pin),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    /**
     * First-login setup: creates the profile document (with the signed-in
     * email) and seeds the same 5 demo students as the offline app, once
     * per user. Existing users are untouched.
     */
    suspend fun ensureProfile(email: String) {
        val snap = profileRef().get().await()
        if (snap.exists()) return
        val demo = listOf(
            Student("STD-1001", "আব্দুল্লাহ আল মামুন", "মোঃ আব্দুল করিম", "ফাতেমা বেগম", "দশম", "01", "2025-26", "ক", "01700000001"),
            Student("STD-1002", "ফারজানা আক্তার", "মোঃ ফজলুল হক", "শাহনাজ বেগম", "নবম", "05", "2025-26", "খ", "01700000002"),
            Student("STD-1003", "মোঃ রাকিব হাসান", "মোঃ আনোয়ার হোসেন", "রোকেয়া বেগম", "অষ্টম", "12", "2025-26", "ক", "01700000003"),
            Student("STD-1004", "নুসরাত জাহান", "মোঃ নজরুল ইসলাম", "মরিয়ম বেগম", "সপ্তম", "03", "2025-26", "গ", ""),
            Student("STD-1005", "তানভীর আহমেদ", "মোঃ তাজুল ইসলাম", "আমিনা খাতুন", "ষষ্ঠ", "07", "2025-26", "ক", "01700000005")
        )
        val batch = db.batch()
        batch.set(profileRef(), UserProfile(email = email, seeded = true))
        demo.forEach { batch.set(studentsRef().document(it.studentId), it) }
        // Seed guardian links for the demo students, grouped by phone,
        // so the guardian app sees them from the first login.
        demo.groupBy { normalizeGuardianPhone(it.phone) }.forEach { (phone, students) ->
            if (phone != null) {
                batch.set(
                    guardianLinksRef().document(phone),
                    mapOf(
                        "phone" to phone,
                        "studentIds" to FieldValue.arrayUnion(*students.map { it.studentId }.toTypedArray())
                    ),
                    SetOptions.merge()
                )
            }
        }
        batch.commit().await()
    }

    companion object {
        private val DHAKA: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

        /** "yyyy-MM-dd" for [millis] in Asia/Dhaka. */
        fun dateString(millis: Long = System.currentTimeMillis()): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fmt.timeZone = DHAKA
            return fmt.format(Date(millis))
        }

        /**
         * Normalises a guardian phone number to E.164 for use as a
         * guardianLinks document id and in the link's phone field.
         * Returns null for a blank number. Spaces and dashes are
         * stripped; a leading "01..." (Bangladeshi mobile) becomes
         * "+8801..."; "+880..." is kept as-is; any other format is
         * returned trimmed (and space/dash-stripped) as-is.
         */
        fun normalizeGuardianPhone(raw: String?): String? {
            val cleaned = raw?.trim()?.replace(" ", "")?.replace("-", "") ?: return null
            if (cleaned.isBlank()) return null
            if (cleaned.startsWith("01")) return "+880" + cleaned.substring(1)
            return cleaned
        }
    }
}
