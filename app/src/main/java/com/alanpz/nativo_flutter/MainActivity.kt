package com.alanpz.nativo_flutter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storageRef: StorageReference
    private lateinit var uploadButton: Button
    private lateinit var logoutButton: Button
    private lateinit var fileListView: ListView

    companion object {
        private const val PICK_FILE_REQUEST = 1
        private const val PREVIEW_REQUEST_CODE = 2
        private const val FOLDER_NAME = "AndroidNativo" // Nombre de la carpeta

        // Credenciales predefinidas
        private const val PREDEFINED_EMAIL = "alan@gmail.com"
        private const val PREDEFINED_PASSWORD = "Fnaf4."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        storageRef = FirebaseStorage.getInstance().reference.child(FOLDER_NAME) // Accede a la carpeta "AndroidNativo"
        uploadButton = findViewById(R.id.uploadButton)
        logoutButton = findViewById(R.id.logoutButton)
        fileListView = findViewById(R.id.fileListView)

        // Intentar iniciar sesión automáticamente
        autoSignIn()

        // Mostrar archivos en Firebase Storage
        listFiles()

        // Subir archivos a Firebase Storage
        uploadButton.setOnClickListener {
            // Abre el selector de archivos
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"  // Permite seleccionar cualquier tipo de archivo
            startActivityForResult(intent, PICK_FILE_REQUEST)
        }

        // Cerrar sesión
        logoutButton.setOnClickListener {
            auth.signOut()
            // Redirigir a una pantalla de inicio de sesión o pantalla principal
            // Por ejemplo, podrías reiniciar la actividad o cerrar la aplicación
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun autoSignIn() {
        auth.signInWithEmailAndPassword(PREDEFINED_EMAIL, PREDEFINED_PASSWORD)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login exitoso
                    showToast("Bienvenido")
                } else {
                    showToast("Error en el inicio de sesión: ${task.exception?.message}")
                    // Puedes redirigir a una pantalla de error o mostrar un mensaje
                }
            }
    }

    // Mostrar archivos en Firebase Storage
    private fun listFiles() {
        val listRef = storageRef // Accede a la carpeta "AndroidNativo"

        listRef.listAll()
            .addOnSuccessListener { result ->
                val fileNames = result.items.map { it.name }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileNames)
                fileListView.adapter = adapter

                // Manejar clics en los ítems de la lista
                fileListView.setOnItemClickListener { _, _, position, _ ->
                    val selectedFileName = fileNames[position]
                    val fileRef = storageRef.child(selectedFileName) // Accede al archivo en la carpeta
                    openPreviewActivity(fileRef, selectedFileName)
                }
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    // Abrir PreviewActivity para visualizar el archivo
    private fun openPreviewActivity(fileRef: StorageReference, fileName: String) {
        val intent = Intent(this, PreviewActivity::class.java).apply {
            putExtra("fileName", fileName)
        }
        startActivityForResult(intent, PREVIEW_REQUEST_CODE)
    }

    // Obtener el nombre del archivo desde la URI
    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return it.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment ?: "unknown"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri: Uri = data.data ?: return
            val fileName = getFileName(uri)
            val fileRef = storageRef.child(fileName) // Subir archivo a la carpeta "AndroidNativo"

            // Subir el archivo
            fileRef.putFile(uri)
                .addOnSuccessListener {
                    showToast("Archivo subido exitosamente: $fileName")
                    // Actualizar la lista de archivos después de subir
                    listFiles()
                }
                .addOnFailureListener { e ->
                    showToast("Error al subir archivo: ${e.message}")
                }
        }
    }

    // Mostrar mensaje usando Toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
