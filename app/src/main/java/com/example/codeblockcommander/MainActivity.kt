package com.example.codeblockcommander

import MainMenu
import WorkPlace
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.codeblockcommander.ui.theme.CodeBlockCommanderTheme

import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            CodeBlockCommanderTheme {
                NavHost(
                    navController = navController,
                    startDestination = "MainMenu"
                ){
                    composable("MainMenu"){
                        MainMenu(navController)

                    }
                    composable("WorkPlace"){
                        WorkPlace(navController)
                    }
                    composable("screen_3"){

                    }
                }
                StartProgram()
            }
        }
    }
}



@Composable
fun StartProgram() {

}




@Composable
fun EditorToolbar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.primary),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { /* Назад в меню */ }) {
            Icon(Icons.Default.ArrowBack, "Назад")
        }

        Text("Мой проект", color = Color.White)

        Row {
            IconButton(onClick = { /* Режим отладки */ }) {
                //Icon(Icons.Default.BugReport, "Отладка")
            }
            IconButton(onClick = { /* Запуск */ }) {
                //Icon(Icons.Default.PlayArrow, "Запуск")
            }
        }
    }
}

@Composable
fun ExecutionPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text("Консоль выполнения:", modifier = Modifier.padding(8.dp))
        // Здесь будет вывод программы
    }
}


























@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScaffoldExample() {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title= { Text("METANIT.COM", fontSize = 22.sp)},
                navigationIcon={ IconButton({ }) { Icon(Icons.Filled.Menu, contentDescription = "Меню")}},
                actions={
                    IconButton({ }) { Icon(Icons.Filled.Info, contentDescription = "О приложении")}
                    IconButton({ }) {Icon(Icons.Filled.Search, contentDescription = "Поиск")}
                },
                colors= TopAppBarDefaults.topAppBarColors(containerColor = Color.DarkGray,
                    titleContentColor = Color.LightGray,
                    navigationIconContentColor = Color.LightGray,
                    actionIconContentColor = Color.LightGray))
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.DarkGray,
                contentColor = Color.LightGray
            ){
                IconButton(onClick = {  }) { Icon(Icons.Filled.Favorite, contentDescription = "Избранное")}
                Spacer(Modifier.weight(1f, true))
                IconButton(onClick = {  }) { Icon(Icons.Filled.Info, contentDescription = "Справка")}
            }
        }
    ){
        Text("Hello METANIT.COM", fontSize = 28.sp, modifier = Modifier.padding(it))
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello 123 $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CodeBlockCommanderTheme {
        Greeting("Android")
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MyTest(){
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Small Top App Bar")
                    Text("Hello guys!!!")
                }
            )
        },
    ) {
    }

}

@Composable
fun ListItem(name: String, prof: String, modifier: Modifier = Modifier){
    Card(
        modifier = Modifier
    ){

            Text(text = "Hello $name")

    }
}