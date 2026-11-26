package com.institution.idscanner.cloud.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.institution.idscanner.cloud.AppViewModel

@Composable
fun SearchScreen(vm: AppViewModel, onProfile: (String) -> Unit) {
    val query by vm.query.collectAsState()
    val results by vm.searchResults.collectAsState()
    val institutionName by vm.institutionName.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Search Student", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query, onValueChange = { vm.setQuery(it) },
            label = { Text("ID, নাম, শ্রেণি বা রোল লিখুন") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (query.isBlank()) {
            Text("উপরে লিখে খুঁজুন। খালি থাকলে সব শিক্ষার্থী Students তালিকায় দেখুন।")
        } else if (results.isEmpty()) {
            Text("কোনো শিক্ষার্থী পাওয়া যায়নি।")
        } else {
            LazyColumn {
                items(results) { student ->
                    StudentCard(student, institutionName) { onProfile(student.studentId) }
                }
            }
        }
    }
}

@Composable
fun StudentsScreen(
    vm: AppViewModel,
    onProfile: (String) -> Unit,
    onAdd: () -> Unit,
    isAdmin: Boolean,
    onClass: (String) -> Unit
) {
    val students by vm.students.collectAsState()
    // Group by trimmed class name; order classes by the known rank, then
    // alphabetically for any other class names.
    val groups = remember(students) {
        students.groupBy { it.className.trim() }
            .entries
            .sortedWith(
                compareBy({ ClassOrder.rank(it.key) }, { it.key })
            )
    }
    Scaffold(
        floatingActionButton = {
            if (isAdmin) FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add student")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("শ্রেণি অনুযায়ী Students (${students.size})", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            if (students.isEmpty()) {
                Text("এখনো কোনো শিক্ষার্থী নেই। Admin থেকে Add করুন বা CSV import করুন।")
            } else {
                LazyColumn {
                    items(groups) { (className, members) ->
                        val label = className.ifBlank { "শ্রেণি উল্লেখ নেই" }
                        androidx.compose.material3.Card(
                            onClick = { onClass(className) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            androidx.compose.foundation.layout.Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text("${members.size} জন", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Display order of classes, matched by keyword contained in the class name. */
internal object ClassOrder {
    private val ranks = listOf(
        "শিশু", "প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম",
        "ষষ্ঠ", "সপ্তম", "অষ্টম", "নবম", "দশম", "একাদশ", "দ্বাদশ"
    )

    fun rank(className: String): Int {
        val name = className.trim()
        if (name.isBlank()) return ranks.size
        val index = ranks.indexOfFirst { name.contains(it) }
        return if (index >= 0) index else ranks.size
    }
}

@Composable
fun ClassStudentsScreen(
    vm: AppViewModel,
    className: String,
    onProfile: (String) -> Unit,
    onAdd: () -> Unit,
    isAdmin: Boolean
) {
    val students by vm.students.collectAsState()
    val institutionName by vm.institutionName.collectAsState()
    // Numeric rolls first, ascending; non-numeric rolls last; then name.
    val members = remember(students, className) {
        students.filter { it.className.trim() == className.trim() }
            .sortedWith(
                compareBy(
                    { it.roll.trim().toIntOrNull() ?: Int.MAX_VALUE },
                    { it.name }
                )
            )
    }
    val label = className.ifBlank { "শ্রেণি উল্লেখ নেই" }
    Scaffold(
        floatingActionButton = {
            if (isAdmin) FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add student")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("$label (${members.size})", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            if (members.isEmpty()) {
                Text("এই শ্রেণিতে কোনো শিক্ষার্থী নেই।")
            } else {
                LazyColumn {
                    items(members) { student ->
                        StudentCard(student, institutionName) { onProfile(student.studentId) }
                    }
                }
            }
        }
    }
}
