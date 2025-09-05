package hr.unipu.android.italo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CoursesScreen(onOpenCourse: (String) -> Unit, vm: CoursesVM = androidx.lifecycle.viewmodel.compose.viewModel()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tečajevi", style = MaterialTheme.typography.headlineSmall)
        vm.items.forEach { c ->
            Card(onClick = { onOpenCourse(c.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(c.title, style = MaterialTheme.typography.titleMedium)
                    Text(c.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        vm.error?.let { Text("Greška: $it", color = MaterialTheme.colorScheme.error) }
    }
}

