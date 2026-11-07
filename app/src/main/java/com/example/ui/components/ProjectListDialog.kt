package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.ui.theme.*
import com.example.util.TimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diálogo de gestão de projetos guardados com Room (abrir, novo projeto, eliminar).
 */
@Composable
fun ProjectListDialog(
    projects: List<ProjectEntity>,
    currentProjectId: String,
    onDismiss: () -> Unit,
    onSelectProject: (String) -> Unit,
    onCreateNewProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newProjectTitle by remember { mutableStateOf("") }
    var showNewField by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projetos Guardados",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botão para criar novo projeto
                if (!showNewField) {
                    Button(
                        onClick = { showNewField = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("new_project_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Criar Novo Projeto", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newProjectTitle,
                            onValueChange = { newProjectTitle = it },
                            placeholder = { Text("Nome do projeto") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_project_title_input")
                        )
                        Button(
                            onClick = {
                                if (newProjectTitle.isNotBlank()) {
                                    onCreateNewProject(newProjectTitle)
                                    showNewField = false
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CineCyan)
                        ) {
                            Text("Criar", color = Color.Black)
                        }
                    }
                }

                HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))

                // Lista de projetos da base de dados Room
                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum projeto guardado.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(projects) { p ->
                            val isCurrent = p.id == currentProjectId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) CineCyan.copy(alpha = 0.2f) else StudioCard)
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) CineCyan else StudioBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onSelectProject(p.id)
                                        onDismiss()
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = p.title,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isCurrent) {
                                            Text(
                                                text = "(Ativo)",
                                                color = CineCyanGlow,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${p.previewWidth}x${p.previewHeight} • ${p.clipsCount} clipes • ${TimeFormatter.formatSimple(p.durationMs)}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = dateFormat.format(Date(p.lastModified)),
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteProject(p.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar projeto",
                                        tint = CineRose
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        modifier = modifier
    )
}
