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
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var fileRef: StorageReference
    private lateinit var fileName: String

    private var currentPage = 0
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    companion object {
        private const val FOLDER_NAME = "AndroidNativo" // Nombre de la carpeta
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_preview)

        imageView = findViewById(R.id.imageView)
        textView = findViewById(R.id.textView)
        closeButton = findViewById(R.id.closeButton)
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        progressBar = findViewById(R.id.progressBar)

        fileName = intent.getStringExtra("fileName") ?: return
        fileRef = FirebaseStorage.getInstance().reference.child(FOLDER_NAME).child(fileName)

        previewFile()

        closeButton.setOnClickListener {
            finish()
        }

        prevPageButton.setOnClickListener {
            showPreviousPage()
        }

        nextPageButton.setOnClickListener {
            showNextPage()
        }
    }

    private fun previewFile() {
        progressBar.visibility = View.VISIBLE
        fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            try {
                when {
                    fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") -> {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        textView.visibility = View.GONE
                        prevPageButton.visibility = View.GONE
                        nextPageButton.visibility = View.GONE
                        pdfRenderer?.close()
                    }

                    fileName.endsWith(".txt") -> {
                        val text = String(bytes, Charsets.UTF_8)
                        textView.text = text
                        textView.visibility = View.VISIBLE
                        imageView.visibility = View.GONE
                        prevPageButton.visibility = View.GONE
                        nextPageButton.visibility = View.GONE
                        pdfRenderer?.close()
                    }

                    fileName.endsWith(".pdf") -> {
                        val file = File(cacheDir, fileName)
                        file.writeBytes(bytes)
                        displayPdf(file)
                        imageView.visibility = View.VISIBLE
                        textView.visibility = View.GONE
                        prevPageButton.visibility = View.VISIBLE
                        nextPageButton.visibility = View.VISIBLE
                    }

                    else -> {
                        textView.text = "Tipo de archivo no soportado"
                        textView.visibility = View.VISIBLE
                        imageView.visibility = View.GONE
                        prevPageButton.visibility = View.GONE
                        nextPageButton.visibility = View.GONE
                        pdfRenderer?.close()
                    }
                }
            } catch (e: Exception) {
                textView.text = "Error al procesar el archivo: ${e.message}"
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                prevPageButton.visibility = View.GONE
                nextPageButton.visibility = View.GONE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
            .addOnFailureListener { e ->
                textView.text = "Error: ${e.message}"
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                prevPageButton.visibility = View.GONE
                nextPageButton.visibility = View.GONE
                progressBar.visibility = View.GONE
            }
    }

    private fun displayPdf(file: File) {
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)

            if (pdfRenderer?.pageCount ?: 0 > 0) {
                currentPage = 0
                showPage(currentPage)
            } else {
                textView.text = "El PDF está vacío"
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                prevPageButton.visibility = View.GONE
                nextPageButton.visibility = View.GONE
            }
        } catch (e: Exception) {
            textView.text = "Error al procesar el PDF: ${e.message}"
            textView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
            prevPageButton.visibility = View.GONE
            nextPageButton.visibility = View.GONE
        }
    }

    private fun showPage(pageIndex: Int) {
        pdfRenderer?.let { pdfRenderer ->
            if (pageIndex < 0 || pageIndex >= pdfRenderer.pageCount) return

            val page = pdfRenderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            imageView.setImageBitmap(bitmap)
            page.close()

            currentPage = pageIndex
            updateNavigationButtons()
        }
    }

    private fun showPreviousPage() {
        if (currentPage > 0) {
            showPage(currentPage - 1)
        }
    }

    private fun showNextPage() {
        if (currentPage < (pdfRenderer?.pageCount ?: 0) - 1) {
            showPage(currentPage + 1)
        }
    }

    private fun updateNavigationButtons() {
        prevPageButton.isEnabled = currentPage > 0
        nextPageButton.isEnabled = currentPage < (pdfRenderer?.pageCount ?: 0) - 1
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
    }
}
