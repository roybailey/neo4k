package me.roybailey.neo4k.reporting

import me.roybailey.neo4k.reporting.config.AuditReports
import me.roybailey.neo4k.reporting.config.ClientReports


enum class ReportExamples(val report: ReportDefinition) {

    auditReports(AuditReports.DailyMetrics),
    clientAudit(AuditReports.ClientAudit),
    conversations(ClientReports.Conversations)

}
