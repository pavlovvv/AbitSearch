package ua.kpi.abitsearch.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.kpi.abitsearch.data.repository.AuthRepository
import ua.kpi.abitsearch.data.repository.AuthResult
import ua.kpi.abitsearch.domain.model.Role
import ua.kpi.abitsearch.domain.validation.AuthValidator

/** Стан форми входу. Помилки показуються після першої спроби відправити форму або виходу з поля. */
data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val submitted: Boolean = false,
    val isLoading: Boolean = false,
    val serverError: String? = null,
) {
    val emailError: String? get() = AuthValidator.email(email).takeIf { submitted || emailTouched }
    val passwordError: String? get() = AuthValidator.loginPassword(password).takeIf { submitted || passwordTouched }
    val isValid: Boolean get() = AuthValidator.email(email) == null && AuthValidator.loginPassword(password) == null
}

/** Стан форми реєстрації. */
data class RegisterFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val acceptedTerms: Boolean = false,
    val touched: Set<Field> = emptySet(),
    val submitted: Boolean = false,
    val isLoading: Boolean = false,
    val serverError: String? = null,
) {
    enum class Field { NAME, EMAIL, PASSWORD, CONFIRM }

    private fun show(field: Field) = submitted || field in touched

    val nameError: String? get() = AuthValidator.name(name).takeIf { show(Field.NAME) }
    val emailError: String? get() = AuthValidator.email(email).takeIf { show(Field.EMAIL) }
    val passwordError: String? get() = AuthValidator.newPassword(password).takeIf { show(Field.PASSWORD) }
    val confirmError: String? get() = AuthValidator.confirmPassword(password, confirm).takeIf { show(Field.CONFIRM) }
    val termsError: String? get() = "Потрібна згода з умовами".takeIf { submitted && !acceptedTerms }

    val isValid: Boolean
        get() = AuthValidator.name(name) == null && AuthValidator.email(email) == null &&
            AuthValidator.newPassword(password) == null && AuthValidator.confirmPassword(password, confirm) == null &&
            acceptedTerms
}

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _login = MutableStateFlow(LoginFormState())
    val login: StateFlow<LoginFormState> = _login.asStateFlow()

    private val _register = MutableStateFlow(RegisterFormState())
    val register: StateFlow<RegisterFormState> = _register.asStateFlow()

    val notice: StateFlow<String?> = auth.notice

    // ---------- вхід ----------
    fun onLoginEmail(v: String) = _login.update { it.copy(email = v, serverError = null) }
    fun onLoginPassword(v: String) = _login.update { it.copy(password = v, serverError = null) }
    fun onLoginEmailBlur() = _login.update { if (it.email.isNotEmpty()) it.copy(emailTouched = true) else it }
    fun onLoginPasswordBlur() = _login.update { if (it.password.isNotEmpty()) it.copy(passwordTouched = true) else it }

    /** Підставляє дані тестового користувача (ПЗ 11: користувачі з різними ролями). Їх створює сервер при першому запуску. */
    fun fillDemo(role: Role) = _login.update {
        if (role == Role.ADMIN) it.copy(email = DEMO_ADMIN_EMAIL, password = DEMO_ADMIN_PASSWORD, serverError = null)
        else it.copy(email = DEMO_USER_EMAIL, password = DEMO_USER_PASSWORD, serverError = null)
    }

    fun submitLogin() {
        val state = _login.value
        _login.value = state.copy(submitted = true)
        if (!state.isValid || state.isLoading) return
        viewModelScope.launch {
            _login.update { it.copy(isLoading = true, serverError = null) }
            val result = auth.login(state.email, state.password)
            _login.update { it.copy(isLoading = false, serverError = (result as? AuthResult.Failure)?.message) }
        }
    }

    // ---------- реєстрація ----------
    fun onName(v: String) = _register.update { it.copy(name = v, serverError = null) }
    fun onEmail(v: String) = _register.update { it.copy(email = v, serverError = null) }
    fun onPassword(v: String) = _register.update { it.copy(password = v, serverError = null) }
    fun onConfirm(v: String) = _register.update { it.copy(confirm = v, serverError = null) }
    fun onTerms(v: Boolean) = _register.update { it.copy(acceptedTerms = v) }
    fun onBlur(field: RegisterFormState.Field) = _register.update { it.copy(touched = it.touched + field) }

    fun submitRegister() {
        val state = _register.value
        _register.value = state.copy(submitted = true)
        if (!state.isValid || state.isLoading) return
        viewModelScope.launch {
            _register.update { it.copy(isLoading = true, serverError = null) }
            val result = auth.register(state.name, state.email, state.password)
            _register.update { it.copy(isLoading = false, serverError = (result as? AuthResult.Failure)?.message) }
        }
    }
}

/** Облікові записи, які сервер AbitSearch створює при першому запуску (UserRepository.seedDemoUsers). */
private const val DEMO_ADMIN_EMAIL = "admin@abitsearch.ua"
private const val DEMO_ADMIN_PASSWORD = "Admin2026"
private const val DEMO_USER_EMAIL = "student@abitsearch.ua"
private const val DEMO_USER_PASSWORD = "Student2026"
