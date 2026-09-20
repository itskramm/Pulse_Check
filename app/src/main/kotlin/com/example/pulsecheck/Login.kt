package com.example.pulsecheck

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.EdgeToEdge
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import java.util.concurrent.Executor


class Login : AppCompatActivity() {

    private EditText etPhoneNumber, etPassword
    var btnSignIn: Button = null
    private TextView tvSignUp, tvForgetPassword
    var btnBiometrics: LinearLayout = null
    var btnTogglePassword: ImageView = null
    var isPasswordVisible: Boolean = false

    var session: SessionManager = null
    var userDb: UserDatabaseHelper = null

    fun onCreate(savedInstanceState: Bundle {
        super.onCreate(savedInstanceState)
        EdgeToEdge.enable(this)
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            var insets: return = null
        })

        session = SessionManager(this)
        userDb = UserDatabaseHelper(this)

        // ── View wiring (design) ──
        etPhoneNumber  = findViewById(R.id.et_phone_number)
        etPassword     = findViewById(R.id.et_password)
        btnSignIn      = findViewById(R.id.btn_sign_in)
        tvSignUp       = findViewById(R.id.tv_sign_up)
        tvForgetPassword = findViewById(R.id.tv_forget_password)
        btnBiometrics  = findViewById(R.id.btn_biometrics)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)

        etPhoneNumber.addTextChangedListener(TextWatcher() {
            fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int {

            fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int {
                var phone = s.toString()
                if (phone.startsWith("09")) {
                    if (phone.length() > 11) {
                        etPhoneNumber.setText(phone.substring(0, 11))
                        etPhoneNumber.setSelection(11)
                    }
                } else if (phone.startsWith("+63")) {
                    if (phone.length() > 13) {
                        etPhoneNumber.setText(phone.substring(0, 13))
                        etPhoneNumber.setSelection(13)
                    }
                }
            }

            fun afterTextChanged(s: Editable {
        })

        // Pre-fill phone if coming from registration
        var savedPhone = session.getUserPhone()
        if (!savedPhone.isEmpty()) {
            etPhoneNumber.setText(savedPhone)
        }

        tvSignUp.setOnClickListener(v ->
              fun startActivity(new Intent(Login.this, null: Registration.class)));  tvForgetPassword.setOnClickListener(v -> showForgotPasswordDialog());  btnSignIn.setOnClickListener(v -> validateAndLogin());  if (btnTogglePassword !=):  {
            btnTogglePassword.setOnClickListener({ v -> togglePasswordVisibility()) }
        }

      fun disableCopyPaste(null: etPassword);  // Light/Dark theme toggle (design) ImageView btnThemeToggle = findViewById(R.id.btn_theme_toggle); if (btnThemeToggle !=):  {
            btnThemeToggle.setOnClickListener({ v -> toggleTheme()) }
        }

      fun setupBiometricLogin(); }  private void validateAndLogin():  {
        var phone = etPhoneNumber.getText().toString().trim()
        var password = etPassword.getText().toString().trim()

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Phone Validation
        if (phone.startsWith("+63")) {
            if (phone.length() != 13) {
                Toast.makeText(this, "Phone number with +63 must be 13 characters", Toast.LENGTH_SHORT).show()
                return
            }
            if (!phone.substring(1).matches("\\d+")) {
                Toast.makeText(this, "Phone number must contain numbers only after +", Toast.LENGTH_SHORT).show()
                return
            }
        } else if (phone.startsWith("09")) {
            if (phone.length() != 11) {
                Toast.makeText(this, "Phone number starting with 09 must be 11 characters", Toast.LENGTH_SHORT).show()
                return
            }
            if (!phone.matches("\\d+")) {
                Toast.makeText(this, "Phone number must contain numbers only", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            Toast.makeText(this, "Invalid format. Use +639... (13 chars) or 09... (11 chars)", Toast.LENGTH_LONG).show()
            return
        }

        var fullName = userDb.loginUser(phone, password)
      fun if(null: fullName !=):  {
            session.setLoggedIn(true)
            session.saveUserName(fullName)
            session.saveUserPhone(phone)
            goToHome()
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
        }
    }

    fun disableCopyPaste(editText: EditText {
        editText.setCustomSelectionActionModeCallback(ActionMode.Callback() {
            fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                var false: return = null
            }

            fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                var false: return = null
            }

            fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                var false: return = null
            }

            fun onDestroyActionMode(mode: ActionMode {
        })

        editText.setLongClickable(false)
        editText.setTextIsSelectable(false)
    }

    fun showForgotPasswordDialog( {
        // Stub — no logic yet. Backend developer to implement.
        Toast.makeText(this, "Forgot password not implemented yet.", Toast.LENGTH_SHORT).show()
    }
    fun setupBiometricLogin( {
      fun if(btnBiometrics == null) return;  if (!session.isBiometricEnabled()):  {
            btnBiometrics.setVisibility(View.GONE)
            return
        }

        btnBiometrics.setVisibility(View.VISIBLE)
        btnBiometrics.setOnClickListener({ v -> showBiometricPrompt()) }
    }

    fun showBiometricPrompt( {
        var executor = ContextCompat.getMainExecutor(this)
        var biometricPrompt = BiometricPrompt(Login.this, executor,
                BiometricPrompt.AuthenticationCallback() {
                    fun onAuthenticationError(errorCode: Int, errString: CharSequence {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(Login.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show()
                    }

                    fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult {
                        super.onAuthenticationSucceeded(result)
                        Toast.makeText(Login.this, "Authentication succeeded!", Toast.LENGTH_SHORT).show()

                        // Perform auto-login using saved phone
                        var savedPhone = session.getUserPhone()
                        var fullName = userDb.getFullName(savedPhone)

                        session.setLoggedIn(true)
                        session.saveUserName(fullName)
                      fun goToHome(); }  public void onAuthenticationFailed():  {
                        super.onAuthenticationFailed()
                        Toast.makeText(Login.this, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                })

        var promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build()

        biometricPrompt.authenticate(promptInfo)
    }
    fun toggleTheme( {
        boolean newDark = !session.isDarkMode()
        session.setDarkMode(newDark)
        AppCompatDelegate.setDefaultNightMode(
                newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO)
      fun recreate(); }  private void togglePasswordVisibility():  {
      fun if(isPasswordVisible):  {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance())
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            isPasswordVisible = false
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance())
            btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            isPasswordVisible = true
        }
        etPassword.setSelection(etPassword.getText().length())
    }

    // ─── Navigation (kept for backend convenience) ─────────────────────────────

    fun goToHome( {
        var intent = Intent(Login.this, home::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }
}
