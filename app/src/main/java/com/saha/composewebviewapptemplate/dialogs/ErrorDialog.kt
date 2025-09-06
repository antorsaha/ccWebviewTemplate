package com.saha.composewebviewapptemplate.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saha.composewebviewapptemplate.R


@Composable
fun ErrorDialog(
    title: String = "",
    message: String,
    isCancellable: Boolean = true,
    onDismissRequest: () -> Unit,
    actionButtonText: String? = null,
    onActionButtonClick: (() -> Unit)? = null
) {

    Dialog(
        onDismissRequest = onDismissRequest, properties = DialogProperties(
            dismissOnBackPress = isCancellable,
            dismissOnClickOutside = isCancellable,
            usePlatformDefaultWidth = true
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            //.weight(1f, false),
            shape = MaterialTheme.shapes.small, colors = CardColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Color.Black,
                disabledContentColor = MaterialTheme.colorScheme.background,
                disabledContainerColor = MaterialTheme.colorScheme.background
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (title.isNotEmpty()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                actionButtonText?.let {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onActionButtonClick?.invoke()
                        }
                    ) {
                        Text(actionButtonText)
                    }
                }


                /*if (positiveButtonText != null) {
                    HeightGap(16.dp)

                    CustomButton(
                        title = positiveButtonText
                    ) {
                        onPositiveButtonClick?.invoke()
                    }
                }*/
            }
        }
    }

}