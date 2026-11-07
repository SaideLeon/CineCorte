package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.GroqWhisperService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Diálogo de Definições para configuração segura da Chave de API da Groq (Whisper).
 */
@Composable
fun SettingsDialog(
    initialApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cabeçalho com Ícone
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = CineCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "Definições da Aplicação",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Configuração do Motor Groq Whisper",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = StudioBorder)

                // Campo da Chave de API
                Text(
                    text = "Chave da API da Groq (Whisper AI)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        testResultText = null
                    },
                    placeholder = { Text("gsk_...", color = TextTertiary, fontSize = 13.sp) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Ocultar chave" else "Mostrar chave",
                                tint = TextSecondary
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CineCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = StudioDarkBg,
                        unfocusedContainerColor = StudioDarkBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("groq_api_key_input")
                )

                // Botão e link para obter a chave
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CineCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Obter chave em console.groq.com",
                            fontSize = 11.sp,
                            color = CineCyan
                        )
                    }

                    // Botão Testar Ligação
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                testResultText = null
                                val service = GroqWhisperService()
                                val result = service.testApiKey(apiKey)
                                isTestingConnection = false
                                result.fold(
                                    onSuccess = { msg ->
                                        isTestSuccess = true
                                        testResultText = msg
                                    },
                                    onFailure = { err ->
                                        isTestSuccess = false
                                        testResultText = err.message ?: "Falha ao ligar à Groq."
                                    }
                                )
                            }
                        },
                        enabled = apiKey.isNotBlank() && !isTestingConnection,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CineCyan.copy(alpha = 0.2f),
                            contentColor = CineCyan
                        ),
                        modifier = Modifier.testTag("test_api_key_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = CineCyan
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testar Ligação", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Resultado do Teste
                AnimatedVisibility(visible = testResultText != null) {
                    testResultText?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTestSuccess) CineEmerald.copy(alpha = 0.15f) else CineRose.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTestSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isTestSuccess) CineEmerald else CineRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    color = if (isTestSuccess) CineEmerald else CineRose
                                )
                            }
                        }
                    }
                }

                // Aviso de Privacidade e Consentimento
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CineAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Privacidade e Processamento",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A sua chave permanece exclusivamente guardada no dispositivo. Durante o Corte Automático com IA, o áudio é enviado de forma segura e pontual para transcrição via Groq.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Botões de Ação Cancelar / Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveApiKey(apiKey.trim())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
