package me.roybailey.neo4k.reporting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class ReportDslTest {

    @Test
    fun `test reporter dsl cypher sample`() {

        val actual = reportDefinition {
            name = "Test Neo4j Report"
            description = "Test Report using NEO4J Target"
            query {
                neo4j("""
                    match (n:Test {status: 'ACTIVE'})
                    return n.name as name, n.age as age, n.release as release, n.ID as ID
                    order by name
                    limit 100
                    """.trimIndent())
            }
        }

        val params = mapOf<String, Any>()

        assertThat(actual.reportName).isEqualTo("Test Neo4j Report")
        assertThat(actual.query.description).isEqualTo("Test Report using NEO4J Target")
        assertThat(actual.query.query).isEqualTo("""
                    match (n:Test {status: 'ACTIVE'})
                    return n.name as name, n.age as age, n.release as release, n.ID as ID
                    order by name
                    limit 100
                    """.trimIndent())
        assertThat(actual.columns).hasSize(0)
    }


    @Test
    fun `test reporter dsl sql sample`() {

        val actual = reportDefinition {
            name = "Test SQL Report"
            description = "Test Report using Primary Target"
            query {
                sql("""
SELECT 'Total Cash Inflow (exc. Transfers) CHF' as metric, 1.33 * SUM(CASH_AMOUNT) as total FROM INVESTMENT_MGMT_SVC.CASH_TRANSACTIONS WHERE
        TRANSACTION_TYPE_DESCRIPTION IN ('PAYMENT', 'REMITTANCE')
        AND TRANSACTION_TYPE='CREDIT'
UNION
SELECT 'Assets under management GBP' as metric, ROUND(SUM(TOTAL)) as total
  FROM (SELECT SUM(DEALT_BALANCE) AS TOTAL FROM INVESTMENT_MGMT_SVC.CASH_ACCOUNT
        UNION ALL
        SELECT SUM(ASSETS_VALUE) AS TOTAL FROM INVESTMENT_MGMT_SVC.PORTFOLIO
       )
UNION
SELECT 'Assets under management CHF' as metric, ROUND(1.33 * SUM(TOTAL)) as total
  FROM (SELECT SUM(DEALT_BALANCE) AS TOTAL FROM INVESTMENT_MGMT_SVC.CASH_ACCOUNT
        UNION ALL
        SELECT SUM(ASSETS_VALUE) AS TOTAL FROM INVESTMENT_MGMT_SVC.PORTFOLIO
       )
                    """.trimIndent())
            }
        }

        val params = mapOf<String, Any>()

        assertThat(actual.reportName).isEqualTo("Test SQL Report")
        assertThat(actual.query.description).isEqualTo("Test Report using Primary Target")
        assertThat(actual.query.query).isEqualTo("""
SELECT 'Total Cash Inflow (exc. Transfers) CHF' as metric, 1.33 * SUM(CASH_AMOUNT) as total FROM INVESTMENT_MGMT_SVC.CASH_TRANSACTIONS WHERE
        TRANSACTION_TYPE_DESCRIPTION IN ('PAYMENT', 'REMITTANCE')
        AND TRANSACTION_TYPE='CREDIT'
UNION
SELECT 'Assets under management GBP' as metric, ROUND(SUM(TOTAL)) as total
  FROM (SELECT SUM(DEALT_BALANCE) AS TOTAL FROM INVESTMENT_MGMT_SVC.CASH_ACCOUNT
        UNION ALL
        SELECT SUM(ASSETS_VALUE) AS TOTAL FROM INVESTMENT_MGMT_SVC.PORTFOLIO
       )
UNION
SELECT 'Assets under management CHF' as metric, ROUND(1.33 * SUM(TOTAL)) as total
  FROM (SELECT SUM(DEALT_BALANCE) AS TOTAL FROM INVESTMENT_MGMT_SVC.CASH_ACCOUNT
        UNION ALL
        SELECT SUM(ASSETS_VALUE) AS TOTAL FROM INVESTMENT_MGMT_SVC.PORTFOLIO
       )
                    """.trimIndent())
        assertThat(actual.columns).isEqualTo(listOf<String>())
    }
}

