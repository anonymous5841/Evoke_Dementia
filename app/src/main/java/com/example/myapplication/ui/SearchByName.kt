package com.example.myapplication.ui

// SearchByScreen.kt

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.BackIconButton
import com.example.myapplication.ui.components.PersonCard
import com.example.myapplication.ui.components.SearchFieldWithIcon
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowTextField
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import androidx.compose.runtime.*


class PersonListScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreenTheme {

                PersonListContent()

            }
        }
    }
}

data class PersonItem(
    val name: String,
    val image: ImageBitmap? = null
)

@Composable
fun PersonListContent(

    people: List<PersonItem> = List(20) {
        PersonItem(name = "Name")
    },

    onBack: () -> Unit = {},

    onSearch: () -> Unit = {},

    onPersonClick: (PersonItem) -> Unit = {}

) {

    val appColors = AppTheme.colors
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = appColors.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {

            Spacer(modifier = Modifier.height(18.dp))

            BackIconButton(
                onBack
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),   // ← needed so there's room to push into
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween             ) {

                SearchFieldWithIcon(
                    value = searchQuery,
                    onValueChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.width(12.dp))

                ShadowButton(

                    width = 66.dp,

                    height = 51.dp,

                    color = appColors.popupText,

                    cornerRadius = 16.dp,

                    onClick = onSearch

                ) {

                    // Replace with your search drawable later
                    Icon(
                        painter = painterResource(R.drawable.location_icon),
                        contentDescription = "Search",
                        tint = appColors.pagesText,
                        )
                }
            }

            Spacer(modifier = Modifier.height(37.dp))

            LazyVerticalGrid(

                columns = GridCells.Fixed(2),

                horizontalArrangement = Arrangement.spacedBy(10.dp),

                verticalArrangement = Arrangement.spacedBy(22.dp),

                modifier = Modifier.fillMaxSize()

            ) {

                items(people) { person ->

                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        PersonCard(

                            name = person.name,

                            image = person.image,

                            modifier = Modifier,

                            onClick = {

                                onPersonClick(person)

                            }
                        )
                    }
                }
            }
        }
    }
}
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PersonListPreview() {

    GreenTheme {

        PersonListContent(

            people = List(12) {

                PersonItem(
                    name = "Name"
                )
            }

        )

    }

}