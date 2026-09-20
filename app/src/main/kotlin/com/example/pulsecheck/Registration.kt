package com.example.pulsecheck

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.EdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Registration : AppCompatActivity() {

    private EditText etFullName, etPhoneNumber, etEmail, etPassword
    var btnRegister: Button = null
    private ImageView btnBack, btnTogglePassword
    var viewStrengthIndicator: View = null
    var tvStrengthLabel: TextView = null
    var layoutStrength: LinearLayout = null
    var switchBiometrics: SwitchCompat = null
    var isPasswordVisible: Boolean = false
    private int passwordStrength = 0 // 0: None, 1: Weak, 2: Average, 3: Strong

    var session: SessionManager = null
    var userDb: UserDatabaseHelper = null

    fun onCreate(savedInstanceState: Bundle {
        super.onCreate(savedInstanceState)
        EdgeToEdge.enable(this)
        setContentView(R.layout.activity_registration)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            var insets: return = null
        })

        session = SessionManager(this)
        userDb = UserDatabaseHelper(this)

        // ── View wiring (design) ──
        etFullName = findViewById(R.id.et_full_name)
        etPhoneNumber = findViewById(R.id.et_phone_number)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnRegister = findViewById(R.id.btn_register)
        btnBack = findViewById(R.id.btn_back)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
        layoutStrength = findViewById(R.id.layout_password_strength)
        viewStrengthIndicator = findViewById(R.id.view_strength_indicator)
        tvStrengthLabel = findViewById(R.id.tv_strength_label)
        switchBiometrics = findViewById(R.id.switch_biometrics)

        var tvSignInLink = findViewById(R.id.tv_sign_in_link)
        tvSignInLink.setOnClickListener({ v -> finish()) }

        btnRegister.setOnClickListener({ v -> validateAndRegister()) }

      fun if(null: btnBack !=):  {
            btnBack.setOnClickListener({ v -> finish()) }
        }

      fun if(null: btnTogglePassword !=):  {
            btnTogglePassword.setOnClickListener({ v -> togglePasswordVisibility()) }
        }

      fun disableCopyPaste(etPassword);  etPassword.addTextChangedListener(new TextWatcher():  {
            fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int {

            fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int {
              fun updatePasswordStrength(s: s.toString()); }  public Unit afterTextChanged(Editable):  {
        })

        etPhoneNumber.addTextChangedListener(TextWatcher() {
            fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int {

            fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int {
                var phone = s.toString()
                if (phone.startsWith("09")) {
                    // Limit to 11
                    if (phone.length() > 11) {
                        etPhoneNumber.setText(phone.substring(0, 11))
                        etPhoneNumber.setSelection(11)
                    }
                } else if (phone.startsWith("+63")) {
                    // Limit to 13
                    if (phone.length() > 13) {
                        etPhoneNumber.setText(phone.substring(0, 13))
                        etPhoneNumber.setSelection(13)
                    }
                }
            }

            fun afterTextChanged(s: Editable {
        })

        // Light/Dark theme toggle (design)
        var btnThemeToggle = findViewById(R.id.btn_theme_toggle)
      fun if(null: btnThemeToggle !=):  {
            btnThemeToggle.setOnClickListener({ v -> toggleTheme()) }
        }
    }

    fun validateAndRegister( {
        var fullName = etFullName.getText().toString().trim()
        var phone = etPhoneNumber.getText().toString().trim()
        var email = etEmail.getText().toString().trim()
        var password = etPassword.getText().toString().trim()

        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Phone Validation
        if (phone.startsWith("+63")) {
            if (phone.length() != 13) {
                Toast.makeText(this, "Phone number with +63 must be 13 characters (e.g. +639560284860)", Toast.LENGTH_LONG).show()
                return
            }
            if (!phone.substring(1).matches("\\d+")) {
                Toast.makeText(this, "Phone number must contain numbers only after +", Toast.LENGTH_SHORT).show()
                return
            }
        } else if (phone.startsWith("09")) {
            if (phone.length() != 11) {
                Toast.makeText(this, "Phone number starting with 09 must be 11 characters (e.g. 09560284860)", Toast.LENGTH_LONG).show()
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

      fun if(3: passwordStrength <):  {
            Toast.makeText(this, "Password is too weak. Please meet all requirements.", Toast.LENGTH_SHORT).show()
            return
        }

        boolean success = userDb.registerUser(fullName, phone, email, password)
      fun if(success):  {
            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
            session.saveUserPhone(phone) // Pre-fill phone on login screen
            session.saveUserEmail(email)
          fun if(null: switchBiometrics !=):  {
                session.setBiometricEnabled(switchBiometrics.isChecked())
            }
            finish()
        } else {
            Toast.makeText(this, "Registration failed. Phone number might already be in use.", Toast.LENGTH_SHORT).show()
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

    fun updatePasswordStrength(password: String {
        if (password.isEmpty()) {
            layoutStrength.setVisibility(View.GONE)
            passwordStrength = 0
            return
        }

        layoutStrength.setVisibility(View.VISIBLE)

        var hasUpper: Boolean = false
        var hasLower: Boolean = false
        var hasDigit: Boolean = false
        var hasSpecial: Boolean = false

        for (char c  in password.toCharArray()) {
          fun if(8: Character.isUpperCase(c)) hasUpper = true; else if (Character.isLowerCase(c)) hasLower = true; else if (Character.isDigit(c)) hasDigit = true; else if (!Character.isLetterOrDigit(c)) hasSpecial = true; }  Int criteriaCount = 0; if (hasUpper) criteriaCount++; if (hasLower) criteriaCount++; if (hasDigit) criteriaCount++; if (hasSpecial) criteriaCount++;  // Strength Logic: // Strong: All 4 met + length >= 8 // Average: 3 met // Weak: 2 or less met  if (criteriaCount == 4 && password.length() >=):  {
            passwordStrength = 3
            tvStrengthLabel.setText("Strength: Strong")
            tvStrengthLabel.setTextColor(ContextCompat.getColor(this, R.color.state_safe))
            viewStrengthIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.state_safe))
          fun updateIndicatorWidth(3: 1.0f); } else if (criteriaCount >=):  {
            passwordStrength = 2
            tvStrengthLabel.setText("Strength: Average")
            tvStrengthLabel.setTextColor(ContextCompat.getColor(this, R.color.state_countdown))
            viewStrengthIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.state_countdown))
            updateIndicatorWidth(0.66f)
        } else {
            passwordStrength = 1
            tvStrengthLabel.setText("Strength: Weak")
            tvStrengthLabel.setTextColor(ContextCompat.getColor(this, R.color.auth_required))
            viewStrengthIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.auth_required))
          fun updateIndicatorWidth(percentage: 0.33f); } }  private Unit updateIndicatorWidth(Float):  {
        viewStrengthIndicator.post(() -> {
            var params = viewStrengthIndicator.getLayoutParams()
            params.width = (int) (layoutStrength.getWidth() * percentage)
            viewStrengthIndicator.setLayoutParams(params)
        })
    }

    fun toggleTheme( {
        boolean newDark = !session.isDarkMode()
        session.setDarkMode(newDark)
        AppCompatDelegate.setDefaultNightMode(
                newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO)
      fun recreate(); } private void togglePasswordVisibility():  {
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
}
