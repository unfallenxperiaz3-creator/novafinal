package com.unfallen.nova.book

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.unfallen.nova.R
import com.unfallen.nova.data.Categories
import com.unfallen.nova.data.DiaryEntry
import com.unfallen.nova.data.Dream
import com.unfallen.nova.data.Memory
import com.unfallen.nova.data.Profile
import com.unfallen.nova.data.TimeCapsule
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * "El libro de tu vida": convierte retrato, recuerdos, diario, sueños y cápsulas abiertas
 * en un PDF con aspecto de libro (tamaño A5).
 */
object LifeBook {

    data class Content(
        val userName: String,
        val profile: Profile,
        val memories: List<Memory>,
        val diary: List<DiaryEntry>,
        val dreams: List<Dream>,
        val capsules: List<TimeCapsule>,
        val prologue: String?,
        val includeDreams: Boolean
    )

    private val es = Locale.forLanguageTag("es-ES")
    private val longDate = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", es)
    private val monthYear = SimpleDateFormat("MMMM 'de' yyyy", es)

    private const val W = 420      // A5 en puntos
    private const val H = 595
    private const val M = 46f      // margen

    private fun String.cap() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(es) else it.toString() }

    /** Crea el PDF en la carpeta privada de la app y devuelve el archivo. */
    fun write(context: Context, c: Content): File {
        val dir = File(context.filesDir, "books").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // solo guardamos el último
        val stamp = SimpleDateFormat("yyyy-MM-dd", es).format(Date())
        val file = File(dir, "Libro-de-mi-vida-$stamp.pdf")

        val doc = PdfDocument()
        val w = Writer(doc)

        // ---------- Portada ----------
        w.cover(context, c)

        // ---------- Prólogo ----------
        if (!c.prologue.isNullOrBlank()) {
            w.chapter("Prólogo", "por NOVA")
            w.paragraph(c.prologue, w.quote)
        }

        // ---------- Quién soy ----------
        if (c.profile.text.isNotBlank() || c.memories.isNotEmpty()) {
            w.chapter("Quién soy", null)
            if (c.profile.text.isNotBlank()) {
                w.label("Mi retrato")
                w.paragraph(c.profile.text, w.body)
                w.gap(10f)
            }
            Categories.all.forEach { cat ->
                val items = c.memories.filter { it.category == cat }.sortedBy { it.createdAt }
                if (items.isNotEmpty()) {
                    w.label(cat)
                    items.forEach { w.paragraph("•  ${it.text}", w.body, after = 3f) }
                    w.gap(8f)
                }
            }
        }

        // ---------- Diario ----------
        val diary = c.diary.sortedBy { it.time }
        if (diary.isNotEmpty()) {
            w.chapter("Mi diario", "${diary.size} días escritos")
            var lastMonth = ""
            diary.forEach { e ->
                val month = monthYear.format(Date(e.time)).cap()
                if (month != lastMonth) {
                    lastMonth = month
                    w.label(month)
                }
                val mood = if (e.mood.isNotBlank()) "   ${e.mood}" else ""
                w.paragraph(longDate.format(Date(e.time)).cap() + mood, w.meta, after = 3f, keep = 40f)
                w.paragraph(e.text, w.body, after = 12f)
            }
        }

        // ---------- Sueños ----------
        val dreams = c.dreams.sortedBy { it.time }
        if (c.includeDreams && dreams.isNotEmpty()) {
            w.chapter("Mis sueños", "${dreams.size} sueños apuntados")
            dreams.forEach { d ->
                val feeling = if (d.feeling.isNotBlank()) "   ${d.feeling}" else ""
                w.paragraph(longDate.format(Date(d.time)).cap() + feeling, w.meta, after = 3f, keep = 40f)
                w.paragraph(d.text, w.body, after = 6f)
                if (d.interpretation.isNotBlank()) {
                    w.paragraph("NOVA: " + d.interpretation, w.quote, after = 14f)
                } else w.gap(8f)
            }
        }

        // ---------- Cápsulas ----------
        val opened = c.capsules.filter { it.opened }.sortedBy { it.createdAt }
        if (opened.isNotEmpty()) {
            w.chapter("Cartas a mi yo del futuro", null)
            opened.forEach { cap ->
                w.paragraph(
                    "Escrita el ${longDate.format(Date(cap.createdAt))} · abierta el ${longDate.format(Date(cap.openAt))}",
                    w.meta, after = 4f, keep = 40f
                )
                w.paragraph(cap.text, w.body, after = 6f)
                if (cap.novaNote.isNotBlank()) w.paragraph("NOVA: " + cap.novaNote, w.quote, after = 14f)
                else w.gap(8f)
            }
        }

        // ---------- Cierre ----------
        w.newPage()
        w.gap(H * 0.35f)
        w.paragraph("Continuará…", w.chapterTitle, align = Layout.Alignment.ALIGN_CENTER, after = 8f)
        w.paragraph(
            "Libro creado con NOVA el ${longDate.format(Date())}.",
            w.meta, align = Layout.Alignment.ALIGN_CENTER
        )
        w.finish()

        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Copia el PDF a la carpeta Descargas del móvil (Android 10+). Devuelve true si lo consigue. */
    fun saveToDownloads(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ================================================================== maquetación

    private class Writer(val doc: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var y = M
        private var pageNo = 0
        private val width = (W - 2 * M).toInt()
        private val bottom = H - M - 14f

        private val ink = Color.rgb(28, 26, 40)
        private val accent = Color.rgb(91, 61, 245)
        private val grey = Color.rgb(120, 118, 135)
        private val paper = Color.rgb(252, 251, 248)

        val chapterTitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); textSize = 22f; color = ink
        }
        val subtitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); textSize = 11f; color = grey
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 8.5f; color = accent
            letterSpacing = 0.12f
        }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.SERIF; textSize = 10.5f; color = ink
        }
        val quote = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); textSize = 10.5f; color = Color.rgb(60, 52, 110)
        }
        val meta = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.SANS_SERIF; textSize = 8.5f; color = grey
        }
        private val footer = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.SERIF; textSize = 8f; color = grey; textAlign = Paint.Align.CENTER
        }

        fun newPage() {
            finish()
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            page = p
            canvas = p.canvas
            canvas.drawColor(paper)
            y = M
        }

        fun finish() {
            val p = page ?: return
            if (pageNo > 1) canvas.drawText("$pageNo", W / 2f, H - 24f, footer)
            doc.finishPage(p)
            page = null
        }

        fun gap(h: Float) {
            if (page == null) newPage()
            y += h
            if (y > bottom) newPage()
        }

        fun chapter(title: String, sub: String?) {
            newPage()
            y += 40f
            paragraph(title, chapterTitle, after = 4f)
            if (sub != null) paragraph(sub, subtitle, after = 0f)
            val p = Paint().apply { color = accent; strokeWidth = 1.2f }
            y += 10f
            canvas.drawLine(M, y, M + 40f, y, p)
            y += 22f
        }

        fun label(text: String) {
            paragraph(text.uppercase(es), labelPaint, after = 6f, keep = 50f)
        }

        /**
         * Escribe un bloque de texto partiéndolo entre páginas si hace falta.
         * [keep] = espacio mínimo que debe quedar para empezarlo en esta página.
         */
        fun paragraph(
            text: String,
            paint: TextPaint,
            after: Float = 8f,
            keep: Float = 0f,
            align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
        ) {
            if (page == null) newPage()
            if (keep > 0 && y + keep > bottom) newPage()
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(align)
                .setLineSpacing(0f, 1.3f)
                .setIncludePad(false)
                .build()
            var line = 0
            while (line < layout.lineCount) {
                val top = layout.getLineTop(line)
                var end = line
                while (end + 1 < layout.lineCount && y + (layout.getLineBottom(end + 1) - top) <= bottom) end++
                val h = (layout.getLineBottom(end) - top).toFloat()
                if (y + h > bottom && line == end && y > M) {
                    newPage(); continue
                }
                canvas.save()
                canvas.clipRect(M, y, M + width, y + h)
                canvas.translate(M, y - top)
                layout.draw(canvas)
                canvas.restore()
                y += h
                line = end + 1
                if (line < layout.lineCount) newPage()
            }
            y += after
        }

        /** Portada oscura con el alien. */
        fun cover(context: Context, c: Content) {
            newPage()
            canvas.drawColor(Color.rgb(6, 8, 14))
            val bmp = decodeAvatar(context)
            if (bmp != null) {
                val dst = RectF(0f, 0f, W.toFloat(), H * 0.72f)
                val srcRatio = bmp.width.toFloat() / bmp.height
                val dstRatio = dst.width() / dst.height()
                val src = if (srcRatio > dstRatio) {
                    val w = (bmp.height * dstRatio).toInt()
                    Rect((bmp.width - w) / 2, 0, (bmp.width + w) / 2, bmp.height)
                } else {
                    val h = (bmp.width / dstRatio).toInt()
                    val top = ((bmp.height - h) * 0.3f).toInt()
                    Rect(0, top, bmp.width, top + h)
                }
                canvas.drawBitmap(bmp, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                val fade = Paint().apply {
                    shader = LinearGradient(0f, H * 0.42f, 0f, H * 0.72f, Color.TRANSPARENT, Color.rgb(6, 8, 14), Shader.TileMode.CLAMP)
                }
                canvas.drawRect(0f, H * 0.42f, W.toFloat(), H * 0.72f + 1, fade)
                bmp.recycle()
            }
            val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); textSize = 26f; color = Color.WHITE
                textAlign = Paint.Align.CENTER
            }
            val sub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); textSize = 12f; color = Color.rgb(180, 170, 255)
                textAlign = Paint.Align.CENTER
            }
            val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.SANS_SERIF; textSize = 9f; color = Color.rgb(150, 155, 175)
                textAlign = Paint.Align.CENTER; letterSpacing = 0.15f
            }
            val name = c.userName.trim()
            canvas.drawText("El libro de", W / 2f, H * 0.76f, sub)
            canvas.drawText(if (name.isNotBlank()) name else "mi vida", W / 2f, H * 0.76f + 34f, title)
            canvas.drawText(range(c), W / 2f, H * 0.76f + 60f, sub)
            canvas.drawText("ESCRITO CON NOVA", W / 2f, H - 40f, small)
        }

        private fun range(c: Content): String {
            val times = c.diary.map { it.time } + c.dreams.map { it.time } + c.memories.map { it.createdAt }
            val first = times.minOrNull() ?: System.currentTimeMillis()
            val y1 = Calendar.getInstance().apply { timeInMillis = first }.get(Calendar.YEAR)
            val y2 = Calendar.getInstance().get(Calendar.YEAR)
            return if (y1 == y2) "$y2" else "$y1 – $y2"
        }

        private fun decodeAvatar(context: Context): Bitmap? = try {
            // inScaled = false: sin esto Android la agranda según la pantalla y puede quedarse sin memoria
            val opts = BitmapFactory.Options().apply { inSampleSize = 2; inScaled = false }
            BitmapFactory.decodeResource(context.resources, R.drawable.nova_avatar, opts)
        } catch (e: Throwable) {
            null
        }
    }
}
