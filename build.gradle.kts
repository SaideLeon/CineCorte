// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// Automatically restore debug.keystore from debug.keystore.base64 when absent (e.g., in CI/GitHub Actions)
val keystoreFile = file("debug.keystore")
val base64KeystoreFile = file("debug.keystore.base64")
if (!keystoreFile.exists() && base64KeystoreFile.exists()) {
  try {
    val decoded = java.util.Base64.getDecoder().decode(base64KeystoreFile.readText().trim())
    keystoreFile.writeBytes(decoded)
  } catch (_: Exception) {
  }
}
