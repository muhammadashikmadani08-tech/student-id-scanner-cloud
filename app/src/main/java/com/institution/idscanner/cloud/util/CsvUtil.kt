package com.institution.idscanner.cloud.util

import com.institution.idscanner.cloud.data.Student
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/** CSV format (header row required):
 *  Student ID | Name | Father | Mother | Class | Roll | Session | Section | Phone
 *  Extra columns are ignored; missing columns default to empty. */
object CsvUtil {
    private fun split(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { out += cur.toString().trim(); cur.clear() }
                else -> cur.append(c)
            }
        }
        out += cur.toString().trim()
        return out.map { it.trim('"') }
    }

    fun parse(input: InputStream): List<Student> {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val lines = reader.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val header = split(lines.first()).map { it.lowercase() }
        fun col(row: List<String>, vararg names: String): String {
            val idx = names.firstNotNullOfOrNull { header.indexOf(it).takeIf { i -> i >= 0 } } ?: return ""
            return row.getOrNull(idx).orEmpty()
        }
        return lines.drop(1).mapNotNull { line ->
            val row = split(line)
            val id = col(row, "student id", "studentid", "id").trim()
            if (id.isEmpty()) return@mapNotNull null
            Student(
                studentId = id,
                name = col(row, "name", "student name"),
                fatherName = col(row, "father", "father name", "father's name"),
                motherName = col(row, "mother", "mother name", "mother's name"),
                className = col(row, "class"),
                roll = col(row, "roll"),
                session = col(row, "session"),
                section = col(row, "section", "division", "branch"),
                phone = col(row, "phone", "mobile", "mobile number")
            )
        }
    }

    fun toCsv(students: List<Student>): String {
        val sb = StringBuilder("Student ID,Name,Father,Mother,Class,Roll,Session,Section,Phone\n")
        students.forEach {
            sb.appendLine(
                listOf(it.studentId, it.name, it.fatherName, it.motherName, it.className, it.roll, it.session, it.section, it.phone)
                    .joinToString(",") { v -> "\"${v.replace("\"", "")}\"" }
            )
        }
        return sb.toString()
    }
}
