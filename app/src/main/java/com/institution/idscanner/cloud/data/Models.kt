package com.institution.idscanner.cloud.data

import com.google.firebase.firestore.Exclude

/**
 * Plain Firestore models. Every field has a default value so Firestore's
 * `toObject()` can always construct them. Field names in Firestore match
 * the property names below. No local-database annotations: Firestore is the only
 * source of truth in the cloud variant.
 *
 * Layout in Firestore (per signed-in user, keyed by Firebase UID):
 *   users/{uid}/students/{studentId}
 *   users/{uid}/attendance/{studentId_yyyy-MM-dd}
 *   users/{uid}/history/{autoId}
 *   users/{uid}/settings/profile
 *   users/{uid}/holidays/{yyyy-MM-dd}
 *   users/{uid}/guardianLinks/{E.164 phone}
 */
data class Student(
    var studentId: String = "",
    var name: String = "",
    var fatherName: String = "",
    var motherName: String = "",
    var className: String = "",
    var roll: String = "",
    var session: String = "",
    var section: String = "",
    var phone: String = "",
    /** Photo location: a Firebase Storage download URL, or (in this
     *  build, where Storage is not enabled) an absolute local file path
     *  on this phone only — or empty. */
    var photoUrl: String = ""
)

data class ScanHistory(
    /** Firestore document id — not stored as a field. */
    @get:Exclude
    var id: String = "",
    var studentId: String = "",
    var scannedAt: Long = System.currentTimeMillis()
)

/**
 * One attendance record per student per day. [date] is "yyyy-MM-dd" in the
 * Asia/Dhaka timezone. The document id is "$studentId_$date", so repeat
 * scans on the same day overwrite the same document and never duplicate.
 */
data class AttendanceRecord(
    @get:Exclude
    var id: String = "",
    var studentId: String = "",
    var date: String = "",
    var scannedAt: Long = System.currentTimeMillis()
)

/** Per-user profile document: users/{uid}/settings/profile. */
data class UserProfile(
    var institutionName: String = "আপনার প্রতিষ্ঠানের নাম",
    var adminPin: String = "1234",
    /** True once the first-login demo students have been seeded. */
    var seeded: Boolean = false,
    var email: String = ""
)

/**
 * One holiday. Stored at users/{uid}/holidays/{date}, where [date] is
 * "yyyy-MM-dd" (Asia/Dhaka) and is also the document id. [title] is the
 * holiday's name/reason, e.g. "ঈদুল ফিতর".
 */
data class Holiday(
    var date: String = "",
    var title: String = ""
)

/**
 * Guardian-to-students link for the guardian app's OTP login. Stored at
 * users/{uid}/guardianLinks/{phone}, where [phone] is the guardian's
 * mobile number normalised to E.164 (see
 * CloudRepository.normalizeGuardianPhone). [studentIds] lists every
 * student whose guardian phone equals [phone]; the doc is deleted when
 * the list becomes empty. The student documents themselves are
 * unchanged — the phone still lives only on [Student.phone].
 */
data class GuardianLink(
    var phone: String = "",
    var studentIds: List<String> = emptyList()
)
