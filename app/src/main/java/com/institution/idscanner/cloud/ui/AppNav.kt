package com.institution.idscanner.cloud.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.institution.idscanner.cloud.AppViewModel
import com.institution.idscanner.cloud.data.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"

    Scaffold(
        topBar = {
            if (route != "home") {
                TopAppBar(
                    title = { Text("Student ID Scanner Cloud") },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm) { nav.navigate(it) } }
            composable("scanner") {
                ScannerScreen(vm) { id -> if (id.isNotBlank()) nav.navigate("profile/$id") else nav.navigate("search") }
            }
            composable("search") { SearchScreen(vm) { nav.navigate("profile/$it") } }
            composable("students") {
                StudentsScreen(
                    vm,
                    onProfile = { nav.navigate("profile/$it") },
                    onAdd = { nav.navigate("form") },
                    isAdmin = true,
                    onClass = { nav.navigate("class/${android.net.Uri.encode(it)}") }
                )
            }
            composable(
                "class/{className}",
                arguments = listOf(navArgument("className") { type = NavType.StringType })
            ) { entry ->
                val className = android.net.Uri.decode(entry.arguments?.getString("className").orEmpty())
                ClassStudentsScreen(
                    vm, className,
                    onProfile = { nav.navigate("profile/$it") },
                    onAdd = { nav.navigate("form") },
                    isAdmin = true
                )
            }
            composable(
                "attendance/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                WeeklyAttendanceScreen(vm, id)
            }
            composable("history") { HistoryScreen(vm) { nav.navigate("profile/$it") } }
            composable("admin") {
                AdminScreen(vm, onStudents = { nav.navigate("students") }, onAdd = { nav.navigate("form") }, onSettings = { nav.navigate("settings") })
            }
            composable("settings") { SettingsScreen(vm) { nav.popBackStack() } }
            composable("form") {
                StudentFormScreen(vm, existing = null) { nav.popBackStack() }
            }
            composable(
                "profile/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ProfileScreen(
                    vm, id,
                    onEdit = { nav.navigate("edit/$id") },
                    onDelete = { nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                    onAttendance = { nav.navigate("attendance/$id") }
                )
            }
            composable(
                "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                // Resolve the student from the current list snapshot via a lookup screen state:
                // simpler: pass through StudentsScreen state by reading vm.students in a wrapper.
                EditWrapper(vm, id) { nav.popBackStack() }
            }
        }
    }
}

@Composable
private fun EditWrapper(vm: AppViewModel, id: String, onDone: () -> Unit) {
    val students by vm.students.collectAsState()
    val existing: Student? = students.firstOrNull { it.studentId == id }
    StudentFormScreen(vm, existing = existing) { onDone() }
}
