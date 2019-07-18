package me.roybailey.neo4k.reporting

import com.google.common.net.MediaType
import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.dsl.QueryStatement
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.OutputStream


data class ReportColumn(
        val name: String,
        val type: String = String::class.java.simpleName,
        val width: Int = 4,
        val format: String = ""
)

open class ReportDefinition(
        val reportName: String,
        val query: QueryStatement,
        val columns: List<ReportColumn> = emptyList()
)

data class ReportOutput(
        val contentType: String = MediaType.CSV_UTF_8.toString(),
        val outputName: String,
        val outputStream: OutputStream = ByteArrayOutputStream()
)


interface ReportRunner {

    fun runReport(report: ReportDefinition, visitor: ReportVisitor)
}


/**
 *
 */
class Neo4kReportRunner(val neo4jService: Neo4jService) : ReportRunner {

    private val logger = KotlinLogging.logger {}


    fun getSafeValue(value: Any?): Any = when (value) {
        null -> ""
        is Number -> value
        else -> value.toString()
    }


    override fun runReport(report: ReportDefinition, visitor: ReportVisitor) {

        //val visitor = CompositeReportVisitor(this::processNeo4jColumns, suppliedVisitor)::reportVisit
        var ctx = ReportContext(
                evt = ReportEvent.START_REPORT,
                name = report.reportName,
                meta = report.columns,
                row = -1, column = -1)
        ctx = visitor(ctx)

        neo4jService.query(report.query.query) { record ->

            ctx = visitor(ctx.copy(evt = ReportEvent.START_ROW, row = ctx.row + 1, column = -1))
            // first row will generate default column meta data if it doesn't exist in definition
            if (ctx.meta.isEmpty()) {
                val columnNames = record.keys().map { ReportColumn(it) }
                ctx = ctx.copy(meta = columnNames)
            }

            ctx.meta.forEachIndexed { cdx, column ->

                val name = column.name
                var value = record[name]
                if (value == null) {
                    logger.warn { "ReportRunner couldn't find value for $name in row ${ctx.row} " }
                }
                ctx = ctx.copy(evt = ReportEvent.DATA)
                ctx = visitor(ctx.copy(column = ctx.column + 1, name = name, value = getSafeValue(value)))
            }
            ctx = visitor(ctx.copy(evt = ReportEvent.END_ROW))
        }
        ctx = visitor(ctx.copy(evt = ReportEvent.END_REPORT, name = report.reportName))
    }
}

