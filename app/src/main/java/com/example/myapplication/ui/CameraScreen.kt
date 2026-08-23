package com.example.myapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.BackIconButton
import com.example.myapplication.ui.components.CameraPreviewPlaceholder
import com.example.myapplication.ui.components.GlowCaptureButton
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource

class CameraPreviewScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreenTheme {
                CameraPreviewContent()
            }
        }
    }
}


@Composable
fun CameraPreviewContent(
    onBack: () -> Unit = {},
    onCapture: () -> Unit = {},
    onGallery: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val appColors = AppTheme.colors

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * ---------------------------------------------------------
             * RESPONSIVE DIMENSIONS
             * ---------------------------------------------------------
             */

            // Horizontal margin for the whole screen
            val horizontalPadding = (maxWidth * 0.06f)
                .coerceIn(20.dp, 32.dp)

            // Space above back button
            val topSpacing = (maxHeight * 0.025f)
                .coerceIn(12.dp, 20.dp)

            // Space between back button and camera preview
            val backToCameraSpacing = (maxHeight * 0.035f)
                .coerceIn(20.dp, 32.dp)

            /*
             * Camera preview should occupy most of the available
             * vertical space, but must leave room for the controls.
             */
            val cameraHeight = (maxHeight * 0.58f)
                .coerceIn(360.dp, 520.dp)

            // Space after camera preview
            val bottomSpacing = (maxHeight * 0.035f)
                .coerceIn(20.dp, 32.dp)


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {

                /*
                 * -----------------------------------------------------
                 * BACK BUTTON
                 * -----------------------------------------------------
                 */

                Spacer(
                    modifier = Modifier.height(topSpacing)
                )

                BackIconButton(
                    onBack = onBack
                )


                /*
                 * -----------------------------------------------------
                 * BACK BUTTON → CAMERA
                 * -----------------------------------------------------
                 */

                Spacer(
                    modifier = Modifier.height(backToCameraSpacing)
                )


                /*
                 * -----------------------------------------------------
                 * CAMERA PREVIEW
                 * -----------------------------------------------------
                 */

                CameraPreviewPlaceholder(
                    height = cameraHeight
                )


                /*
                 * -----------------------------------------------------
                 * CAMERA ACTIONS
                 * -----------------------------------------------------
                 */

                CameraActions(
                    onCapture = onCapture,
                    onGallery = onGallery,
                    onRefresh = onRefresh
                )


                /*
                 * -----------------------------------------------------
                 * BOTTOM SPACE
                 * -----------------------------------------------------
                 */

                Spacer(
                    modifier = Modifier.height(bottomSpacing)
                )
            }
        }
    }
}

@Composable
private fun CameraActions(
    onCapture: () -> Unit,
    onGallery: () -> Unit,
    onRefresh: () -> Unit
) {

    val appColors = AppTheme.colors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                170.dp
            )
    ) {

        /*
         * Responsive sizes
         */

        val sidePadding = (maxWidth * 0.06f)
            .coerceIn(16.dp, 28.dp)

        val iconButtonSize = (maxWidth * 0.17f)
            .coerceIn(58.dp, 65.dp)

        val iconSize = (maxWidth * 0.145f)
            .coerceIn(48.dp, 55.dp)

        val captureSize = (maxWidth * 0.23f)
            .coerceIn(78.dp, 90.dp)

        val labelSpacing = (maxWidth * 0.025f)
            .coerceIn(6.dp, 10.dp)


        /*
         * ---------------------------------------------------------
         * GALLERY
         * ---------------------------------------------------------
         */

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = sidePadding,
                    top = 18.dp
                )
                .clickable {
                    onGallery()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            IconButton(
                onClick = {
                    onGallery()
                },
                modifier = Modifier.size(
                    iconButtonSize
                )
            ) {

                Icon(
                    painter = painterResource(
                        R.drawable.gallery_icon
                    ),
                    contentDescription = stringResource(
                        R.string.gallery_icon
                    ),
                    modifier = Modifier.size(
                        iconSize
                    ),
                    tint = appColors.backButton
                )
            }

            Spacer(
                modifier = Modifier.height(
                    labelSpacing
                )
            )

            Text(
                text = stringResource(
                    R.string.gallery
                ),
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
            )
        }


        /*
         * ---------------------------------------------------------
         * CAPTURE
         * ---------------------------------------------------------
         */

        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            GlowCaptureButton(
                size = captureSize,
                onClick = onCapture
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = stringResource(
                    R.string.capture
                ),
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
            )
        }


        /*
         * ---------------------------------------------------------
         * REFRESH
         * ---------------------------------------------------------
         */

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    end = sidePadding,
                    top = 18.dp
                )
                .clickable {
                    onRefresh()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            IconButton(
                onClick = {
                    onRefresh()
                },
                modifier = Modifier.size(
                    iconButtonSize
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(
                        R.string.refresh_icon
                    ),
                    modifier = Modifier.size(
                        iconSize
                    ),
                    tint = appColors.backButton
                )
            }

            Spacer(
                modifier = Modifier.height(
                    labelSpacing
                )
            )

            Text(
                text = stringResource(
                    R.string.refresh
                ),
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun CameraPreviewScreenPreview() {

    GreenTheme {
        CameraPreviewContent()
    }

}