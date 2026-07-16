package com.sky31.gongmultiplatform.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.sky31.gongmultiplatform.ui.viewModel.AuthViewModel
import com.sky31.gongmultiplatform.ui.viewModel.ConfigViewModel
import com.sky31.gongmultiplatform.ui.viewModel.LoginOptions
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.TokenState
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.password_invisible
import gongmultiplatform.composeapp.generated.resources.password_visible
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun AuthorizationDialog() {
    val state = rememberDialogState()
    val scope = rememberCoroutineScope()

    val viewModel = getKoin().get<AuthViewModel>()
    val configViewModel: ConfigViewModel = getKoin().get<ConfigViewModel>()

    val keyboardController = LocalSoftwareKeyboardController.current
    val autoFillManager = LocalAutofillManager.current

    val visible by TokenState.isExpired.collectAsState()
    val username by viewModel.username.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val authConfig by configViewModel.authConfig.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var errorMsgVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val passwordIconResource = remember(passwordVisible) {
        if (passwordVisible) Res.drawable.password_visible else Res.drawable.password_invisible
    }

    LaunchedEffect(visible) {
        if(authConfig.reauthentication) {
            if(visible) {
                password = ""
                errorMsg = ""
                errorMsgVisible = false
                state.show()
            } else {
                state.hide()
            }
        }
    }

    LaunchedEffect(authState.errorMessage) {
        if(authState.errorMessage != null) {
            errorMsg = authState.errorMessage!!
            errorMsgVisible = true
        }
    }

    NotificationDialog(
        state = state,
        modifier = Modifier
            .fillMaxWidth(0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "重新验证",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(bottom = 30.dp)
            )

            username?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
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
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    visualTransformation = if(passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                            passwordVisible = !passwordVisible
                        },
                ) {
                    Image(
                        painter = painterResource(passwordIconResource),
                        contentDescription = "password",
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingButton(
                    text = "验证",
                    call = {
                        keyboardController?.hide()
                        username?.let {
                            viewModel.login(
                                username = it,
                                password = password,
                                options = LoginOptions(needNavigation = false)
                            ) { result ->
                                when(result) {
                                    is NetworkResult.Success -> {
                                        TokenState.refreshed()
                                        autoFillManager?.commit()
                                    }
                                    is NetworkResult.Error -> {
                                        errorMsg = result.message
                                        errorMsgVisible = true
                                    }
                                }
                            }
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if(errorMsgVisible) {
                    Text(
                        text = errorMsg,
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Text(
                    text = "切换账号",
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                viewModel.logout()
                                TokenState.refreshed()
                            }
                        }
                )
            }

            Text(
                text = "暂时离线模式",
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clickable {
                        scope.launch {
                            viewModel.enterOfflineMode(needNavigation = false)
                            state.hide()
                        }
                    }
            )
        }
    }
}
