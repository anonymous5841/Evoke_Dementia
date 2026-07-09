package com.example.myapplication.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.components.BackIconButton
import com.example.myapplication.ui.components.PersonCard
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowTextField
import com.example.myapplication.ui.theme.GreenTheme

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            BackIconButton(
                onBack
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .width(280.dp)      // Change this to whatever width you need
                        .height(50.dp)
                ) {

                    ShadowTextField(
                        modifier = Modifier.fillMaxSize()
                            .offset(
                                y = (3).dp
                            ),
                        value = "",
                        onValueChange = {},
                        placeholder = "Search by name",
                        height = 50.dp,
                        cornerRadius = 15.dp
                    )

                    // Bottom drawable
                    Icon(
                        painter = painterResource(R.drawable.ic_green),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(
                                x = (20).dp,
                                y = (-5).dp)
                            .size(150.dp),
                        tint = Color.Unspecified
                    )

                    // Top drawable (placed over the first one)
                    Icon(
                        painter = painterResource(R.drawable.ic_yellowsearch),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 50.dp)
                            .offset(
                                x = (10).dp,
                                y = (-13).dp)
                            .size(25.dp),
                        tint = Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                ShadowButton(

                    width = 66.dp,

                    height = 48.dp,

                    color = MaterialTheme.colorScheme.secondary,

                    cornerRadius = 16.dp,

                    onClick = onSearch

                ) {

                    // Replace with your search drawable later
                    Icon(
                        painter = painterResource(R.drawable.location_icon),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(35.dp))

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