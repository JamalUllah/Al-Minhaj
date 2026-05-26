package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfSampleGenerator {
    fun getSamplePdfFile(context: Context): File {
        val file = File(context.cacheDir, "sample_arabic_textbook.pdf")
        if (file.exists()) {
            return file
        }

        val pdfDocument = PdfDocument()

        // Page 1: Title Page
        var pageInfo = PdfDocument.PageInfo.Builder(600, 850, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        drawPageBackground(canvas)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7A5933") // Natural Tones main wood-brown
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("كِتَابُ العِلْمِ وَالأَدَبِ", 300f, 250f, paint)
        
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        canvas.drawText("مَطْبُوعَةُ المَدْرَسَةِ لِلطَّلَبَةِ النُّجَبَاءِ", 300f, 320f, paint)

        paint.textSize = 14f
        paint.color = Color.parseColor("#857365") // Natural mud brown-gray
        canvas.drawText("[ Arabic Scanned-Style PDF Book ]", 300f, 500f, paint)
        canvas.drawText("(Tap 'Extract Text' to run Gemini OCR)", 300f, 550f, paint)
        
        pdfDocument.finishPage(page)

        // Page 2: Grammar - Al-Ajurrumiyya Passage
        pageInfo = PdfDocument.PageInfo.Builder(600, 850, 2).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        drawPageBackground(canvas)

        paint.color = Color.parseColor("#212121")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT

        canvas.drawText("بَابُ مَعْرِفَةِ عَلَامَاتِ الإِعْرَابِ", 540f, 100f, paint)

        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        
        val linesPage2 = listOf(
            "لِلرَّفْعِ أَرْبَعُ عَلَامَاتٍ: الضَّمَّةُ، وَالوَاوُ، وَالأَلِفُ، وَالنُّونُ.",
            "فَأَمَّا الضَّمَّةُ فَتَكُونُ عَلَامَةً لِلرَّفْعِ فِي أَرْبَعَةِ مَوَاضِعَ:",
            "الِاسْمِ المُفْرَدِ، وَجَمْعِ التَّكْسِيرِ، وَجَمْعِ المُؤَنَّثِ السَّالِمِ،",
            "وَالفِعْلِ المُضَارِعِ الَّذِي لَمْ يَتَّصِلْ بِآخِرِهِ شَيْءٌ.",
            "وَأَمَّا الوَاوُ فَتَكُونُ عَلَامَةً لِلرَّفْعِ فِي مَوْضِعَيْنِ:",
            "فِي جَمْعِ المُذَكَّرِ السَّالِمِ، وَفِي الأَسْمَاءِ الخَمْسَةِ.",
            "وَهِيَ: أَبُوكَ وَأَخُوكَ وَحَمُوكَ وَفُوكَ وَذُو مَالٍ."
        )

        var y = 170f
        for (line in linesPage2) {
            canvas.drawText(line, 540f, y, paint)
            y += 45f
        }

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.GRAY
        paint.textSize = 12f
        canvas.drawText("صفحة ٢", 300f, 800f, paint)

        pdfDocument.finishPage(page)

        // Page 3: Wisdom Words & Urdu Translation sample
        pageInfo = PdfDocument.PageInfo.Builder(600, 850, 3).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        drawPageBackground(canvas)

        paint.color = Color.parseColor("#516F52") // Soft natural sage green
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("حِكْمَة ومَوْعِظَة", 540f, 110f, paint)

        paint.textSize = 13f
        paint.color = Color.parseColor("#201A17") // Deep dark warm charcoal
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)

        val linesPage3 = listOf(
            "العِلْمُ صَيْدٌ وَالكِتَابَةُ قَيْدُهُ، قَيِّدْ صُيُودَكَ بِالحِبَالِ الوَاثِقَةِ.",
            "مَنْ لَمْ يَذُقْ مُرَّ التَّعَلُّمِ سَاعَةً، تَجَرَّعَ ذُلَّ الجَهْلِ طُولَ حَيَاتِهِ.",
            "تَعَلَّمْ فَلَيْسَ المَرْءُ يُولَدُ عَالِمًا، وَلَيْسَ أَخُو عِلْمٍ كَمَنْ هُوَ جَاهِلٌ.",
            "",
            "ترجمہ اردو (Urdu Translation Example):",
            "علم ایک شکار ہے اور لکھنا اس کی قید ہے۔ اپنے شکار کو مضبوط رسیوں سے باندھ لو۔",
            "جس نے ایک لمحہ کے لیے سیکھنے کی کڑواہٹ کا مزہ نہیں چکھا، وہ ساری زندگی جہالت کی ذلت پیتا رہے گا۔"
        )

        y = 190f
        for (line in linesPage3) {
            if (line.isNotEmpty()) {
                canvas.drawText(line, 540f, y, paint)
            }
            y += 45f
        }

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.GRAY
        paint.textSize = 12f
        canvas.drawText("صفحة ٣", 300f, 800f, paint)

        pdfDocument.finishPage(page)

        // Write to file
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        return file
    }

    private fun drawPageBackground(canvas: Canvas) {
        // Create an organic old cream manuscript/paper canvas
        canvas.drawColor(Color.parseColor("#F5EFE9"))
        
        // Draw a light elegant border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#EADDD2")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(25f, 25f, 575f, 825f, borderPaint)
        
        // Inner double border (looks like classical printing)
        borderPaint.strokeWidth = 0.8f
        borderPaint.color = Color.parseColor("#C8B59E")
        canvas.drawRect(30f, 30f, 570f, 820f, borderPaint)
    }
}
