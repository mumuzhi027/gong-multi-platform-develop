package com.sky31.gongmultiplatform.ui.screen.loginScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.ui.component.LoadingButton
import com.sky31.gongmultiplatform.ui.viewModel.AuthViewModel
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.PlatformOperation
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.login_logo
import gongmultiplatform.composeapp.generated.resources.password_invisible
import gongmultiplatform.composeapp.generated.resources.password_visible
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun LoginScreen() {
    val autoFillManager = LocalAutofillManager.current
    val authViewModel = getKoin().get<AuthViewModel>()
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var alertVisible by remember { mutableStateOf(false) }
    var alertText by remember { mutableStateOf("") }

    val animatedColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "color"
    )

    val authState by authViewModel.authState.collectAsState()

    val passwordVisible = remember { mutableStateOf(false) }
    val passwordIconResource = remember(passwordVisible.value) {
        if (passwordVisible.value) Res.drawable.password_visible else Res.drawable.password_invisible
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(authState) {
        if (authState.errorMessage != null) {
            alertText = authState.errorMessage!!
            alertVisible = true
            delay(2500)
            alertVisible = false
            authViewModel.resetAuthState()
        }
    }

    LaunchedEffect(username, password) {
        enabled = username.isNotEmpty() && password.isNotEmpty()
    }

    PlatformOperation.BackHandler(true) {
        PlatformOperation.moveToBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 50.dp)
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
        ) {
            this@Column.AnimatedVisibility(
                visible = alertVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(top = 6.dp, bottom = 6.dp, start = 25.dp, end = 25.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = alertText,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier
                .height(40.dp)
        )

        Image(
            painter = painterResource(Res.drawable.login_logo),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(200.dp),
            contentDescription = "Login Logo",
        )

        Spacer(
            modifier = Modifier
                .height(40.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(.7f)
                .height(45.dp)
                .align(Alignment.CenterHorizontally)
                .border(
                    width = 1.dp,
                    color = Color(0xFF969696),
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(start = 5.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
                    .semantics { contentType = ContentType.Username },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                decorationBox = { innerTextField ->
                    Box {
                        if (username.isEmpty()) {
                            Text("用户名", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                },
                value = username,
                onValueChange = {
                    username = it
                },
            )
        }

        Spacer(
            modifier = Modifier
                .height(15.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(.7f)
                .height(45.dp)
                .align(Alignment.CenterHorizontally)
                .border(
                    width = 1.dp,
                    color = Color(0xFF969696),
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(start = 5.dp, end = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
                    .semantics { contentType = ContentType.Password },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                visualTransformation = if (passwordVisible.value) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (password.isEmpty()) {
                            Text("密码", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                },
                value = password,
                onValueChange = {
                    password = it
                },
            )

            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable {
                        passwordVisible.value = !passwordVisible.value
                    },
            ) {
                Image(
                    painter = painterResource(passwordIconResource),
                    contentDescription = "Toggle Password Visibility",
                )
            }
        }

        Spacer(
            modifier = Modifier
                .height(80.dp)
        )

        LoadingButton(
            modifier = Modifier
                .width(170.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .background(animatedColor)
                .align(Alignment.CenterHorizontally),
            call = {
                keyboardController?.hide()
                authViewModel.login(username, password) { result ->
                    if (result is NetworkResult.Success) {
                        autoFillManager?.commit()
                    }
                }
            },
            text = "登录"
        )

        Text(
            text = "离线模式",
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(top = 18.dp)
                .align(Alignment.CenterHorizontally)
                .clickable {
                    keyboardController?.hide()
                    scope.launch {
                        authViewModel.enterOfflineMode()
                    }
                }
        )
    }
}
