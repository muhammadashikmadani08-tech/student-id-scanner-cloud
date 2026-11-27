package com.institution.idscanner.cloud.util

import com.institution.idscanner.cloud.data.AttendanceRecord
import com.institution.idscanner.cloud.data.CloudRepository
import com.institution.idscanner.cloud.data.Student
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Attendance CSV export. Day and week status rules are the SAME as
 * WeeklyAttendanceScreen: a record means উপস্থিত (with its Dhaka scan
 * time and সকাল/বিকেল session); a past date with no record, or today
 * at/after 20:00 with no record, means অনুপস্থিত; anything else is
 * এখনো বাকি. Weeks run Saturday–Friday, week offset 0 = current week.
 *
 * CSV layout matches the existing students export: every value quoted,
 * internal quotes stripped, UTF-8. Callers must prepend a UTF-8 BOM
 * when writing the file so Excel shows Bengali text correctly.
 */
object AttendanceCsv {

    /** CSV header: Date, Student ID, Name, Class, Roll, Status, Scan Time, Session. */
    const val HEADER = "Date,Student ID,Name,Class,Roll,Status,Scan Time,Session"

    private val DhakaTz: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    private fun quote(value: String): String = "\"${value.replace("\"", "")}\""

    private fun timeFormat(): SimpleDateFormat =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = DhakaTz }

    /** "yyyy-MM-dd" dates (Asia/Dhaka) for the Sat–Fri week at [weekOffset]. */
    fun weekDates(weekOffset: Int = 0, nowMillis: Long = System.currentTimeMillis()): List<String> {
        val start = Calendar.getInstance(DhakaTz).apply { timeInMillis = nowMillis }
        val back = (start.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7
        start.add(Calendar.DAY_OF_YEAR, -back + weekOffset * 7)
        return (0..6).map { i ->
            val day = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            CloudRepository.dateString(day.timeInMillis)
        }
    }

    /** First date ("yyyy-MM-dd") of the current Sat–Fri week. */
    fun weekStartDate(nowMillis: Long = System.currentTimeMillis()): String =
        weekDates(weekOffset = 0, nowMillis = nowMillis).first()

    /**
     * Status for one student-day, exactly as WeeklyAttendanceScreen shows
     * it. Returns (status, scan time, session); scan time and session
     * are empty unless the student is present.
     */
    fun statusFor(
        record: AttendanceRecord?,
        date: String,
        todayString: String,
        nowHour: Int
    ): Triple<String, String, String> {
        if (record != null) {
            val hourCal = Calendar.getInstance(DhakaTz).apply { timeInMillis = record.scannedAt }
            val session = if (hourCal.get(Calendar.HOUR_OF_DAY) < 12) "সকাল" else "বিকেল"
            return Triple("উপস্থিত", timeFormat().format(Date(record.scannedAt)), session)
        }
        if (date < todayString || (date == todayString && nowHour >= 20)) {
            return Triple("অনুপস্থিত", "", "")
        }
        return Triple("এখনো বাকি", "", "")
    }

    private fun rowsForDates(
        students: List<Student>,
        records: List<AttendanceRecord>,
        dates: List<String>,
        todayString: String,
        nowHour: Int
    ): String {
        val byKey = records.associateBy { it.studentId to it.date }
        val sb = StringBuilder(HEADER).append('\n')
        for (student in students) {
            for (date in dates) {
                val (status, scanTime, session) = statusFor(
                    byKey[student.studentId to date], date, todayString, nowHour
                )
                sb.appendLine(
                    listOf(date, student.studentId, student.name, student.className, student.roll, status, scanTime, session)
                        .joinToString(",") { quote(it) }
                )
            }
        }
        return sb.toString()
    }

    /** One row per student for [date] (defaults to today, Asia/Dhaka). */
    fun dayCsv(
        students: List<Student>,
        records: List<AttendanceRecord>,
        date: String = CloudRepository.dateString(),
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val now = Calendar.getInstance(DhakaTz).apply { timeInMillis = nowMillis }
        return rowsForDates(
            students, records, listOf(date),
            CloudRepository.dateString(nowMillis), now.get(Calendar.HOUR_OF_DAY)
        )
    }

    /** One row per student per day for the Sat–Fri week at [weekOffset]. */
    fun weekCsv(
        students: List<Student>,
        records: List<AttendanceRecord>,
        weekOffset: Int = 0,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val now = Calendar.getInstance(DhakaTz).apply { timeInMillis = nowMillis }
        return rowsForDates(
            students, records, weekDates(weekOffset, nowMillis),
            CloudRepository.dateString(nowMillis), now.get(Calendar.HOUR_OF_DAY)
        )
    }
}
