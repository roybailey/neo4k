package me.roybailey.neo4k.reporting.visitor

import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.datatable.DataTable
import me.roybailey.neo4k.reporting.ReportContext
import me.roybailey.neo4k.reporting.ReportEvent
import mu.KotlinLogging
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.String.valueOf


class PdfReportVisitor(
        val reportName: String,
        val writer: OutputStream = ByteArrayOutputStream()
) {

    private val logger = KotlinLogging.logger {}

    // Capture the data
    val data = mutableListOf<MutableList<Any?>>()
    val listColumns = mutableListOf<Any?>()
    val listValues = mutableListOf<Any?>()

    fun reportVisit(ctx: ReportContext): ReportContext = when (ctx.evt) {
        ReportEvent.START_REPORT -> {
            logger.info("$reportName ${ctx.evt}")
            ctx
        }
        ReportEvent.DATA -> {
            if (ctx.row == 0) {
                listColumns += ctx.name
            }
            listValues += valueOf(ctx.value)
            ctx
        }
        ReportEvent.END_ROW -> {
            if (ctx.row == 0) {
                data += listColumns.toMutableList()
            }
            while(listValues.size < listColumns.size)
                listValues += ""
            data += listValues.toMutableList()
            listValues.clear()
            ctx
        }
        ReportEvent.END_REPORT -> {
            logger.info("$reportName ${ctx.evt}")
            writePdfReport()
            ctx
        }
        else -> ctx
    }

    fun writePdfReport() {

        // Initialize Document
        val doc = PDDocument()
        val page = PDPage()

        // Create a landscape page
        page.mediaBox = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
        doc.addPage(page)
        // Initialize table
        val margin = 10f
        val tableWidth = page.mediaBox.width - 2 * margin
        val yStartNewPage = page.mediaBox.height - 2 * margin
        val bottomMargin = 0f

        val dataTable = BaseTable(
                yStartNewPage,
                yStartNewPage,
                bottomMargin,
                tableWidth,
                margin,
                doc,
                page,
                true,
                true)
        val table = DataTable(dataTable, page)
        table.addListToTable(data, DataTable.HASHEADER)

        dataTable.draw()
        doc.save(writer)
        doc.close()
    }

    override fun toString(): String = writer.toString()
}

