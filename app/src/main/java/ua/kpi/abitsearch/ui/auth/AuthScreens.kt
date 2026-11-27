package ua.kpi.abitsearch.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.kpi.abitsearch.domain.model.Role
import ua.kpi.abitsearch.domain.validation.AuthValidator
import ua.kpi.abitsearch.ui.components.AbitChip
import ua.kpi.abitsearch.ui.components.GlassCard
import ua.kpi.abitsearch.ui.theme.Abit

/** Заголовок екранів автентифікації: логотип, назва і підзаголовок. */
@Composable
private fun AuthHeader(title: String, subtitle: String) {
    val c = Abit.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(c.primary, c.primaryEnd))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.School, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(title, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/**
 * Поле форми з підписом, іконкою, текстом помилки та необов'язковою підказкою.
 * onBlur викликається, коли поле втрачає фокус після того, як мало його.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    hint: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    onBlur: () -> Unit = {},
) {
    val c = Abit.colors
    var visible by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = if (isPassword) {
            @Composable {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        if (visible) "Сховати пароль" else "Показати пароль",
                    )
                }
            }
        } else null,
        isError = error != null,
        supportingText = (error ?: hint)?.let { msg -> @Composable { Text(msg) } },
        singleLine = true,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.text, unfocusedTextColor = c.text,
            focusedBorderColor = c.primary, unfocusedBorderColor = c.outline,
            focusedLabelColor = c.primary, unfocusedLabelColor = c.muted,
            focusedLeadingIconColor = c.primary, unfocusedLeadingIconColor = c.muted,
            focusedTrailingIconColor = c.muted, unfocusedTrailingIconColor = c.muted,
            focusedSupportingTextColor = c.muted, unfocusedSupportingTextColor = c.muted,
            focusedContainerColor = c.glass, unfocusedContainerColor = c.glass,
            cursorColor = c.primary,
            errorBorderColor = Color(0xFFF87171), errorLabelColor = Color(0xFFF87171),
            errorSupportingTextColor = Color(0xFFF87171), errorLeadingIconColor = Color(0xFFF87171),
            errorContainerColor = c.glass, errorTextColor = c.text,
        ),
        modifier = modifier
            .padding(bottom = 6.dp)
            .fillMaxWidth()
            .onFocusChanged {
                if (it.isFocused) hadFocus = true else if (hadFocus) onBlur()
            },
    )
}

/** Кнопка відправлення форми зі станом завантаження. */
@Composable
private fun SubmitButton(text: String, loading: Boolean, onClick: () -> Unit) {
    val c = Abit.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(c.primary, c.primaryEnd)))
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
        else Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

/** Повідомлення про помилку від сервера (невірний пароль, зайнятий e-mail тощо). */
@Composable
private fun ServerError(message: String) {
    val red = Color(0xFFF87171)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(red.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = red, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = red, fontSize = 13.sp)
    }
}

/** Інформаційне повідомлення над формою, наприклад «Сесія завершилась». */
@Composable
private fun NoticeBanner(message: String) {
    val amber = Color(0xFFFBBF24)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(amber.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Lock, null, tint = amber, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = amber, fontSize = 13.sp)
    }
}

/** Посилання внизу форми: «Немає акаунта? Зареєструватися». */
@Composable
private fun SwitchLink(question: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(question, color = Abit.colors.muted, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            action, color = Abit.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

/** Каркас екрана: прокрутка, відступи під клавіатуру, обмеження ширини для планшетів. */
@Composable
private fun AuthScaffold(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) { content() }
    }
}

@Composable
fun LoginScreen(vm: AuthViewModel, onRegister: () -> Unit) {
    val s by vm.login.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    AuthScaffold {
        AuthHeader("AbitSearch", "Увійдіть, щоб переглядати статистику вступу та зберігати обране")
        notice?.let { NoticeBanner(it) }
        GlassCard(Modifier.fillMaxWidth(), padding = 18.dp) {
            Text("Вхід", color = Abit.colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                s.email, vm::onLoginEmail, "E-mail", Icons.Outlined.Mail, s.emailError,
                keyboardType = KeyboardType.Email, onBlur = vm::onLoginEmailBlur,
            )
            AuthTextField(
                s.password, vm::onLoginPassword, "Пароль", Icons.Outlined.Lock, s.passwordError,
                isPassword = true, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                onImeAction = { focus.clearFocus(); vm.submitLogin() }, onBlur = vm::onLoginPasswordBlur,
            )
            s.serverError?.let { Spacer(Modifier.height(4.dp)); ServerError(it) }
            Spacer(Modifier.height(14.dp))
            SubmitButton("Увійти", s.isLoading) { focus.clearFocus(); vm.submitLogin() }
        }
        Text("Тестові користувачі", color = Abit.colors.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AbitChip("Студент", false) { vm.fillDemo(Role.USER) }
            AbitChip("Адміністратор", false) { vm.fillDemo(Role.ADMIN) }
        }
        SwitchLink("Немає акаунта?", "Зареєструватися", onRegister)
    }
}

@Composable
fun RegisterScreen(vm: AuthViewModel, onLogin: () -> Unit) {
    val s by vm.register.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val c = Abit.colors

    AuthScaffold {
        AuthHeader("Реєстрація", "Створіть акаунт AbitSearch")
        GlassCard(Modifier.fillMaxWidth(), padding = 18.dp) {
            AuthTextField(
                s.name, vm::onName, "Ім'я та прізвище", Icons.Outlined.Person, s.nameError,
                onBlur = { vm.onBlur(RegisterFormState.Field.NAME) },
            )
            AuthTextField(
                s.email, vm::onEmail, "E-mail", Icons.Outlined.Mail, s.emailError,
                hint = "На цю адресу буде прив'язано акаунт", keyboardType = KeyboardType.Email,
                onBlur = { vm.onBlur(RegisterFormState.Field.EMAIL) },
            )
            AuthTextField(
                s.password, vm::onPassword, "Пароль", Icons.Outlined.Lock, s.passwordError,
                isPassword = true, keyboardType = KeyboardType.Password,
                onBlur = { vm.onBlur(RegisterFormState.Field.PASSWORD) },
            )
            PasswordChecklist(s.password)
            Spacer(Modifier.height(6.dp))
            AuthTextField(
                s.confirm, vm::onConfirm, "Повторіть пароль", Icons.Outlined.Badge, s.confirmError,
                isPassword = true, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                onImeAction = { focus.clearFocus() }, onBlur = { vm.onBlur(RegisterFormState.Field.CONFIRM) },
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { vm.onTerms(!s.acceptedTerms) }) {
                Checkbox(
                    checked = s.acceptedTerms, onCheckedChange = vm::onTerms,
                    colors = CheckboxDefaults.colors(checkedColor = c.primary, uncheckedColor = if (s.termsError != null) Color(0xFFF87171) else c.muted),
                )
                Text("Погоджуюсь з умовами використання", color = if (s.termsError != null) Color(0xFFF87171) else c.text, fontSize = 13.sp)
            }
            s.serverError?.let { Spacer(Modifier.height(4.dp)); ServerError(it) }
            Spacer(Modifier.height(10.dp))
            SubmitButton("Створити акаунт", s.isLoading) { focus.clearFocus(); vm.submitRegister() }
        }
        SwitchLink("Вже маєте акаунт?", "Увійти", onLogin)
    }
}

/** Жива підказка вимог до пароля: виконані пункти позначаються зеленим. */
@Composable
private fun PasswordChecklist(password: String) {
    val c = Abit.colors
    Column(Modifier.padding(start = 6.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        AuthValidator.passwordChecklist(password).forEach { (text, ok) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                    tint = if (ok) Color(0xFF34D399) else c.faint, modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(text, color = if (ok) c.text else c.muted, fontSize = 12.sp)
            }
        }
    }
}

/** Значок ролі для екрана налаштувань. */
@Composable
fun RoleBadge(role: Role) {
    val color = if (role == Role.ADMIN) Color(0xFFFBBF24) else Abit.colors.primary
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (role == Role.ADMIN) {
            Icon(Icons.Outlined.AdminPanelSettings, null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(role.title, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
