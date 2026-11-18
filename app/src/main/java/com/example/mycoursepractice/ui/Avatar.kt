package com.example.mycoursepractice.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.widget.ImageView
import android.widget.TextView
import java.io.File

/**
 * 头像：有图片就画成圆形，没有就回退到「姓名首字」的圆形字块。
 *
 * 图片是用户从系统相册选的那一张，复制到应用私有目录后又缩到 512px 以内，
 * 这里只负责按目标尺寸解码 + 圆裁，不做任何网络请求。
 */
object Avatar {

    private const val MAX_STORE_PX = 512

    /** 把用户选的图片收进私有目录，返回落盘路径。 */
    fun store(ctx: Context, source: Bitmap, target: File): String {
        val scale = MAX_STORE_PX.toFloat() / maxOf(source.width, source.height)
        val out = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }
        target.parentFile?.mkdirs()
        target.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return target.absolutePath
    }

    fun decode(path: String?, targetPx: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(path, opts) ?: return null
        return raw.circle(targetPx)
    }

    /** 圆角方形：按短边缩放、居中裁方，再套 26% 圆角遮罩。 */
    fun Bitmap.circle(targetPx: Int): Bitmap {
        val size = targetPx.coerceAtLeast(1)
        val side = minOf(width, height).toFloat()
        val left = ((width - side) / 2f)
        val top = ((height - side) / 2f)

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(this@circle, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        // 让 shader 先把短边方区缩放到目标直径，再平移到左上角
        val matrix = android.graphics.Matrix().apply {
            val s = size / side
            setScale(s, s)
            postTranslate(-left * s, -top * s)
        }
        paint.shader.setLocalMatrix(matrix)

        val radius = size * 0.26f
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)
        return out
    }

    /** 统一入口：有图显示图，没图显示首字，两者只会显示一个。 */
    fun bind(ctx: Context, image: ImageView, monogram: TextView, path: String?, initial: String, sizeDp: Float) {
        val px = ctx.dp(sizeDp)
        val bitmap = decode(path, px)
        if (bitmap != null) {
            image.setImageBitmap(bitmap)
            image.showIf(true)
            monogram.showIf(false)
        } else {
            image.setImageDrawable(null)
            image.showIf(false)
            monogram.showIf(true)
            monogram.text = initial
        }
    }

    fun tintOf(color: Int): Int = color or Color.TRANSPARENT
}
