package com.example.restoooo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.restoooo.adapter.MenuList
import com.example.restoooo.model.Menu
import com.example.restoooo.ui.theme.RestooooTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RestooooTheme {
                val menuList = remember { mutableStateListOf<Menu>() }
                val context = LocalContext.current
                
                val loadMenuData = {
                    firestore.collection("menu").get().addOnSuccessListener { documents ->
                        menuList.clear()
                        for (document in documents) {
                            val menu = document.toObject(Menu::class.java)
                            menu.id = document.id
                            menuList.add(menu)
                        }
                    }.addOnFailureListener {
                        Toast.makeText(context, "Gagal ambil data: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                val updateMenu = { menu: Menu ->
                    firestore.collection("menu").document(menu.id).set(menu)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Menu updated! slay. ✨", Toast.LENGTH_SHORT).show()
                            loadMenuData()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Update gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                val deleteMenu = { menu: Menu ->
                    firestore.collection("menu").document(menu.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Menu deleted. Bye! 👋", Toast.LENGTH_SHORT).show()
                            loadMenuData()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Hapus gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        loadMenuData()
                    }
                }

                LaunchedEffect(Unit) {
                    loadMenuData()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RestoMainScreen(
                        menuList = menuList,
                        onAddMenuClick = {
                            val intent = Intent(this, InputMenuActivity::class.java)
                            launcher.launch(intent)
                        },
                        onUpdateMenu = { updateMenu(it) },
                        onDeleteMenu = { deleteMenu(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoMainScreen(
    menuList: List<Menu>,
    onAddMenuClick: () -> Unit,
    onUpdateMenu: (Menu) -> Unit,
    onDeleteMenu: (Menu) -> Unit
) {
    var selectedMenu by remember { mutableStateOf<Menu?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Restoooo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMenuClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Menu", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "Vibe Makan Hari Ini ✨",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            if (menuList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Makanannya belum ada, Bestie. 😭", color = MaterialTheme.colorScheme.outline)
                        Text("Ayo tambah sekarang!", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                MenuList(
                    menuList = menuList,
                    onMenuClick = { menu ->
                        selectedMenu = menu
                        showEditSheet = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showEditSheet && selectedMenu != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            EditMenuContent(
                menu = selectedMenu!!,
                onSave = { updatedMenu ->
                    onUpdateMenu(updatedMenu)
                    showEditSheet = false
                },
                onDeleteClick = {
                    showDeleteDialog = true
                }
            )
        }
    }

    if (showDeleteDialog && selectedMenu != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Menu? 🥺", fontWeight = FontWeight.Bold) },
            text = { Text("Yakin mau hapus '${selectedMenu?.nama}'? Nanti nggak bisa balik lagi lho.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMenu(selectedMenu!!)
                        showDeleteDialog = false
                        showEditSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus Aja")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Gak Jadi")
                }
            }
        )
    }
}

@Composable
fun EditMenuContent(
    menu: Menu,
    onSave: (Menu) -> Unit,
    onDeleteClick: () -> Unit
) {
    var nama by remember { mutableStateOf(menu.nama) }
    var harga by remember { mutableStateOf(menu.harga.toString()) }
    var deskripsi by remember { mutableStateOf(menu.deskripsi) }
    var gambarUrl by remember { mutableStateOf(menu.gambar) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Edit Vibe ✏️", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            IconButton(
                onClick = onDeleteClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus")
            }
        }

        // Preview Image small
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            if (gambarUrl.isNotEmpty()) {
                AsyncImage(
                    model = gambarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No image URL", color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        OutlinedTextField(
            value = nama,
            onValueChange = { nama = it },
            label = { Text("Food Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = harga,
            onValueChange = { harga = it },
            label = { Text("Price") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = gambarUrl,
            onValueChange = { gambarUrl = it },
            label = { Text("Image URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = deskripsi,
            onValueChange = { deskripsi = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(16.dp)
        )

        Button(
            onClick = {
                val hargaDouble = harga.toDoubleOrNull() ?: 0.0
                onSave(menu.copy(nama = nama, harga = hargaDouble, deskripsi = deskripsi, gambar = gambarUrl))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Changes ✨", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}
