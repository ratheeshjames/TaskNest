package com.example.data.backup

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.domain.model.TaskItem
import com.example.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Generates multi-page formatted PDF reports of the user's tasks using Android's native
 * [PdfDocument] API and supports both exporting to a SAF [Uri] and printing via [PrintManager].
 */
@Singleton
class PdfReportGenerator @Inject constructor(
  @ApplicationContext private val context: Context,
  private val taskRepository: TaskRepository
) {
  companion object {
    private const val PAGE_WIDTH = 595 // A4 width in PostScript points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in PostScript points
    private const val MARGIN = 42f
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  suspend fun exportPdfToUri(targetUri: Uri) = withContext(Dispatchers.IO) {
    val tasks = taskRepository.getAllTasksSnapshot().filter { it.deletedAt == null }
    context.contentResolver.openOutputStream(targetUri, "wt")?.use { outputStream ->
      writeTasksPdfToStream(tasks, outputStream)
    } ?: error("Unable to open PDF output stream")
  }

  fun printTaskReport(activity: Activity) {
    val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "TaskNest_Report_${System.currentTimeMillis()}"

    printManager.print(
      jobName,
      object : PrintDocumentAdapter() {
        private var cachedTasks: List<TaskItem> = emptyList()

        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback?,
          extras: Bundle?
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
          }
          scope.launch {
            cachedTasks = taskRepository.getAllTasksSnapshot().filter { it.deletedAt == null }
            val info = PrintDocumentInfo.Builder("tasknest_tasks_report.pdf")
              .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
              .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
              .build()
            callback?.onLayoutFinished(info, true)
          }
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor?,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback?
        ) {
          if (destination == null) {
            callback?.onWriteFailed("Destination descriptor missing")
            return
          }
          try {
            FileOutputStream(destination.fileDescriptor).use { out ->
              writeTasksPdfToStream(cachedTasks, out)
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
          }
        }
      },
      null
    )
  }

  fun writeTasksPdfToStream(tasks: List<TaskItem>, outputStream: OutputStream) {
    val pdfDocument = PdfDocument()
    try {
      val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(14, 107, 80)
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 11f
      }
      val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 12.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(80, 90, 86)
        textSize = 10f
      }
      val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(215, 224, 220)
        strokeWidth = 1f
      }

      var pageNumber = 1
      var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      var currentPage = pdfDocument.startPage(pageInfo)
      var canvas: Canvas = currentPage.canvas
      var cursorY = MARGIN + 20f

      val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm"))
      val completedCount = tasks.count { it.isCompleted }
      val pendingCount = tasks.size - completedCount

      canvas.drawText("TaskNest — Offline Task Report", MARGIN, cursorY, headerPaint)
      cursorY += 18f
      canvas.drawText(
        "Generated $timestamp • ${tasks.size} total ($pendingCount active, $completedCount completed)",
        MARGIN,
        cursorY,
        subHeaderPaint
      )
      cursorY += 14f
      canvas.drawLine(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY, dividerPaint)
      cursorY += 22f

      if (tasks.isEmpty()) {
        canvas.drawText("No tasks recorded yet.", MARGIN, cursorY, titlePaint)
      } else {
        tasks.forEach { task ->
          if (cursorY > PAGE_HEIGHT - MARGIN - 65f) {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            cursorY = MARGIN + 20f
          }

          val checkboxPrefix = if (task.isCompleted) "[x]" else "[ ]"
          val titleLine = "$checkboxPrefix ${task.title.take(75)}"
          canvas.drawText(titleLine, MARGIN, cursorY, titlePaint)
          cursorY += 15f

          val metaParts = mutableListOf<String>()
          metaParts.add("List: ${task.listName ?: "Inbox"}")
          if (task.priority.level > 0) metaParts.add("Priority: ${task.priority.label}")
          task.dueDate?.let { d -> metaParts.add("Due: $d${task.dueTime?.let { " $it" } ?: ""}") }
          if (task.subtasks.isNotEmpty()) {
            metaParts.add("Subtasks: ${task.completedSubtaskCount}/${task.subtasks.size}")
          }
          if (task.tags.isNotEmpty()) {
            metaParts.add("Tags: ${task.tags.joinToString(", ") { "#${it.name}" }}")
          }

          canvas.drawText(metaParts.joinToString("  •  ").take(105), MARGIN + 16f, cursorY, metaPaint)
          cursorY += 14f

          if (task.notes.isNotBlank()) {
            val cleanNote = task.notes.replace("\n", " ").take(95)
            canvas.drawText("Notes: $cleanNote", MARGIN + 16f, cursorY, metaPaint)
            cursorY += 14f
          }

          canvas.drawLine(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY, dividerPaint)
          cursorY += 16f
        }
      }

      pdfDocument.finishPage(currentPage)
      pdfDocument.writeTo(outputStream)
    } finally {
      pdfDocument.close()
    }
  }
}
