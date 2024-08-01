package com.alanpz.nativo_flutter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var closeButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var fileRef: StorageReference
    private lateinit var fileName: String

    companion object {
        private const val FOLDER_NAME = "AndroidNativo" // Nombre de la carpeta
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_preview)

        imageView = findViewById(R.id.imageView)
        textView = findViewById(R.id.textView)
        closeButton = findViewById(R.id.closeButton)
        progressBar = findViewById(R.id.progressBar)

        fileName = intent.getStringExtra("fileName") ?: return
        // Accede a la carpeta "AndroidNativo"
        fileRef = FirebaseStorage.getInstance().reference.child(FOLDER_NAME).child(fileName)

        previewFile()

        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun previewFile() {
        // Mostrar ProgressBar mientras se carga el archivo
        progressBar.visibility = View.VISIBLE
        fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            try {
                when {
                    fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") -> {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        textView.visibility = View.GONE
                    }

                    fileName.endsWith(".txt") -> {
                        val text = String(bytes, Charsets.UTF_8)
                        textView.text = text
                        textView.visibility = View.VISIBLE
                        imageView.visibility = View.GONE
                    }

                    fileName.endsWith(".pdf") -> {
                        val file = File(cacheDir, fileName)
                        file.writeBytes(bytes)
                        displayPdf(file)
                        imageView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }

                    else -> {
                        textView.text = "Tipo de archivo no soportado"
                        textView.visibility = View.VISIBLE
                        imageView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                textView.text = "Error al procesar el archivo: ${e.message}"
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
            } finally {
                // Ocultar ProgressBar cuando termine la carga
                progressBar.visibility = View.GONE
            }
        }
            .addOnFailureListener { e ->
                textView.text = "Error: ${e.message}"
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                progressBar.visibility = View.GONE
            }
    }

    private fun displayPdf(file: File) {
        val parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(parcelFileDescriptor)
        if (pdfRenderer.pageCount > 0) {
            val page = pdfRenderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            imageView.setImageBitmap(bitmap)
            page.close()
        }
        pdfRenderer.close()
    }
}
