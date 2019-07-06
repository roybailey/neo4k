package me.roybailey.neo4k.reporting.visitor

import me.roybailey.neo4k.reporting.ReportContext
import me.roybailey.neo4k.reporting.ReportEvent
import me.roybailey.neo4k.reporting.SimpleReportVisitor
import mu.KotlinLogging
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.Double.parseDouble
import java.lang.String.valueOf


class ExcelReportVisitor(
        reportName: String,
        val writer: OutputStream = ByteArrayOutputStream(),
        val workbook: HSSFWorkbook = HSSFWorkbook()
) : SimpleReportVisitor(reportName) {

    override fun reportVisit(ctx: ReportContext): ReportContext {
        val result = super.reportVisit(ctx)
        when (ctx.evt) {
            ReportEvent.END_REPORT -> {
                writeExcelReport()
            }
        }
        return result
    }

    fun writeExcelReport() {

        val createHelper = workbook.creationHelper
        val sheet = workbook.createSheet(reportName)

        var rdx = 0
        var row = sheet.createRow(rdx++)
        listColumns.forEachIndexed { idx, name ->

            logger.info { "col=$idx now=${sheet.getColumnWidth(idx)} len=${valueOf(name).length} cal=${listColumnWidths[idx]} def=${sheet.defaultColumnWidth}" }

            // seems inconsistent to calculate new widths for different platforms
            // so using the defaultWidth as more reliable measure of ~7 characters
            val colWidth = sheet.getColumnWidth(idx)
            var newMaxWidth = colWidth
            var calcMaxWidth = Math.min(listColumnWidths[idx], 64)
            while(calcMaxWidth > 7) {
                newMaxWidth += colWidth
                calcMaxWidth -= 7
            }
            sheet.setColumnWidth(idx, newMaxWidth)

            val cell = row.createCell(idx, CellType.STRING)
            cell.setCellValue(valueOf(name))
        }

        data.forEachIndexed { idx, values ->
            row = sheet.createRow(rdx++)
            values.values.forEachIndexed { idx, value ->
                when(value) {
                    is Number, Int, Long, Float, Double -> {
                        val cell = row.createCell(idx, CellType.NUMERIC)
                        cell.setCellValue(parseDouble(valueOf(value)))
                    }
                    else -> {
                        val cell = row.createCell(idx, CellType.STRING)
                        cell.setCellValue(valueOf(value))
                    }
                }
            }
        }

        sheet.createFreezePane(0,1)
    }

    override fun toString(): String = writer.toString()
}

