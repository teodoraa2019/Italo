package hr.unipu.android.italo.ui.screens


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.firebase.auth.FirebaseAuthException

@Composable
fun EmailField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("E-mail adresa") },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
        modifier = modifier
    )
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Lozinka",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier
    )
}

@Composable
fun AssistiveError(msg: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = msg,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

fun Throwable?.fbMsg(): String = when ((this as? FirebaseAuthException)?.errorCode) {
    "ERROR_EMAIL_ALREADY_IN_USE" -> "E-mail je već registriran."
    "ERROR_INVALID_EMAIL"        -> "Neispravna e-mail adresa."
    "ERROR_WEAK_PASSWORD"        -> "Lozinka mora imati najmanje 6 znakova."
    "ERROR_USER_NOT_FOUND"       -> "Korisnik ne postoji."
    "ERROR_WRONG_PASSWORD"       -> "Pogrešna lozinka."
    "ERROR_TOO_MANY_REQUESTS"    -> "Previše pokušaja. Pokušaj kasnije."
    else -> this?.localizedMessage ?: "Došlo je do pogreške."
}
