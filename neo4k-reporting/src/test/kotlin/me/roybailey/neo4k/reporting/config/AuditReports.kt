package me.roybailey.neo4k.reporting.config

import me.roybailey.neo4k.reporting.reportDefinition


/* ************************************************************
    NON-CID REPORTS FOR DATA ANALYST (RAW AUDIT CSV DUMPS)
   ************************************************************ */

object AuditReports {

    val DailyMetrics = reportDefinition {
        name = "Daily Metrics"
        description = "Aggregates daily metrics across all accounts and investments"
        query {
            sql("""
SELECT '01. Number of accounts opened' as metric, COUNT(*) as total FROM CRM_SVC.CUSTOMERS WHERE  CUSTOMER_TYPE!= 'PROSPECT'
UNION
SELECT '02. Number of accounts closed' as metric, COUNT(*) as total FROM CRM_SVC.CUSTOMERS WHERE  CUSTOMER_TYPE= 'CLIENT_ACCOUNT_CLOSED'
UNION
SELECT '03. Number of active accounts' as metric, COUNT(*) as total FROM CRM_SVC.CUSTOMERS WHERE  CUSTOMER_TYPE= 'CLIENT'
UNION
SELECT '04. Number of accounts funded' as metric, COUNT(DISTINCT(USER_ID)) as total FROM INVESTMENT_MGMT_SVC.INVESTMENT I WHERE I.STATUS='ACTIVE' AND I.FUNDED_DATE IS NOT NULL
UNION
SELECT '05. Total Cash Inflow (exc. Transfers) GBP' as metric, SUM(CASH_AMOUNT) as total FROM INVESTMENT_MGMT_SVC.CASH_TRANSACTIONS WHERE
        TRANSACTION_TYPE_DESCRIPTION IN ('PAYMENT', 'REMITTANCE')
        AND TRANSACTION_TYPE='CREDIT'
UNION
SELECT '06. Total Cash Inflow (exc. Transfers) CHF' as metric, 1.33 * SUM(CASH_AMOUNT) as total FROM INVESTMENT_MGMT_SVC.CASH_TRANSACTIONS WHERE
        TRANSACTION_TYPE_DESCRIPTION IN ('PAYMENT', 'REMITTANCE')
        AND TRANSACTION_TYPE='CREDIT'
UNION
SELECT '07. Assets under management GBP' as metric, ROUND(SUM(TOTAL)) as total
  FROM (SELECT SUM(DEALT_BALANCE) AS TOTAL FROM INVESTMENT_MGMT_SVC.CASH_ACCOUNT
        UNION ALL
        SELECT SUM(ASSETS_VALUE) AS TOTAL FROM INVESTMENT_MGMT_SVC.PORTFOLIO
       )
UNION
SELECT '08. Assets under management CHF' as metric, ROUND(1.33 * SUM(TOTAL)) as total
  FROM (SELECT SUM(DEALT_BALANCE) AS TOTAL FROM INVESTMENT_MGMT_SVC.CASH_ACCOUNT
        UNION ALL
        SELECT SUM(ASSETS_VALUE) AS TOTAL FROM INVESTMENT_MGMT_SVC.PORTFOLIO
       )
""".trimIndent())
        }
    }


    val RegistrationAudit = reportDefinition {
        name = "Registration Audit"
        description = "Registration token and status audit"
        query {
            sql("""
select
    USER_GUID,
    TOKEN_CREATED_DATE_TIME,
    REGISTRATION_STATUS,
    CREATED_DATE_TIME,
    UPDATED_DATE_TIME
from REGISTRATION_SVC.USER_REGISTRATION
""".trimIndent())
        }
    }


    val OnboardingAudit = reportDefinition {
        name = "Onboarding Audit"
        description = "Onboarding status audit"
        query {
            sql("""
select
    USER_ID,
    MARKET_CODE,
    CV.CREATED_DATE_TIME,
    CV.UPDATED_DATE_TIME,
    RISK_PROFILE_ADVICE_ID,
    STATUS,
    CONVERSATION_VERSION,
    QUESTION_ID,
    IS_SKIPPED,
    IS_LOCKED,
    ANSWER_TYPE,
    ANSWERED_AT
from onboarding_svc.onboarding_user_identity oui
left outer join onboarding_svc.conversations CV on CV.ONBOARDING_ID=oui.ID
left outer join onboarding_svc.conversation_questions cq on cq.conversation_id=cv.ID
""".trimIndent())
        }
    }


    val ProductAudit = reportDefinition {
        name = "Product Audit"
        description = "Product audit report"
        query {
            neo4j("""
match (p:Product)--(a:Asset)
optional match (m:Mandate)--(c:Customer:Active:Client)--(i:Investment:Active)--(p)
return p.productId as productId,
p.strategy as strategy,
p.style as style,
a.name as asset,
a.shareval as shareval,
count(distinct c) as totalCustomers,
count(distinct i) as totalInvestments,
sum(i.totalAssetValue) as totalAssetValue
order by totalAssetValue desc
""".trimIndent())
        }
    }


    val ClientAudit = reportDefinition {
        name = "Client Audit"
        description = "Client audit report"
        query {
            neo4j("""
match (c:Customer)
optional match (c)-[:SELECTED_STRATEGY]->(ss:Strategy)
optional match (c)--(i:Investment:Active)--(it:InvestmentType)
optional match (c)--(pc:PegaCase)
optional match (c)-[login]-(uatc:Audit {name: 'USER_AUTHORISATION_TOKEN_CREATED'})
optional match (c)-[email]-(es:Audit {name: 'EMAIL_SENT'})
optional match (c)-[terms]-(taca:Audit {name: 'TERMS_AND_CONDITIONS_ACCEPTED'})
optional match (c)-[fact]-(fspdf:Audit {name: 'FACT_SHEET_PDF_DOWNLOAD'})
optional match (c)-[kiid]-(kiidpdf:Audit {name: 'KIID_PDF_DOWNLOAD'})
return
    c.userId as userId,
    c.CREATED_DATE_TIME as createdDateTime,
    c.UPDATED_DATE_TIME as updatedDateTime,
    c.customerType as customerType,
    c.discountCode as discountCode,
    c.taxDomicile as taxDomicile,
    c.status as status,
    c.reviewDateTime as reviewDateTime,
    c.reviewStatus as reviewStatus,
    toInteger(left(c.dob,4)) as yearOfBirth,
    c.isUbsEmployee as isUbsEmployee,
    c.external as isExternal,
    ss.name as selectedStrategy,
    count(distinct i) as countInvestments,
    apoc.coll.sort(collect(distinct it.name)) as investmentTypes,
    sum(i.totalAssetValue) as totalAssetValue,
    sum(i.totalAvailableBalance) as totalAvailableBalance,
    sum(i.totalDealtBalance) as totalDealtBalance,
    sum(i.totalSettledBalance) as totalSettledBalance,
    count(pc) as openPegaCases,
    login.latest as lastLogin, COALESCE(toInteger(login.total),0) as totalLogins,
    terms.latest as lastTermsAndConditions, COALESCE(toInteger(terms.total),0) as totalTermsAndConditions,
    email.latest as lastEmailSent, COALESCE(toInteger(email.total),0) as totalEmailsSent,
    fact.latest as lastFactSheetDownload, COALESCE(toInteger(fact.total),0) as totalFactSheetDownload,
    kiid.latest as lastKiidDownload, COALESCE(toInteger(kiid.total),0) as totalKiidDownload
""".trimIndent())
        }
    }


    val InvestmentAudit = reportDefinition {
        name = "Investment Audit"
        description = "Investment audit report"
        query {
            neo4j("""
match (i:Investment)--(it:InvestmentType)
optional match (i)--(c:Customer)
optional match (i)--(p:Product)--(ass:Asset)
optional match (ddi:DirectDebitInstruction)-[:RELATES_TO]->(i)
return
    c.userId as userId, c.CREATED_DATE_TIME as clientCreatedDateTime,
    it.name as investmentType,
    i.investmentId as investmentId,
    i.CREATED_DATE_TIME as createdDateTime,
    i.FUNDED_DATE as fundedDate,
    i.STATUS as status,
    i.SHARE_val as shareval,
    i.MANDATE_ID as mandateId,
    i.totalAssetValue as totalAssetValue,
    i.totalAvailableBalance as totalAvailableBalance,
    i.totalDealtBalance as totalDealtBalance,
    i.totalSettledBalance as totalSettledBalance,
    p.productId as productId,
    p.name as productName,
    p.style as productStyle,
    ass.assetId as assetId,
    ass.name as assetName,
    ddi.instructionId as ddiInstructionId,
    ddi.REFERENCE_NUMBER as ddiReferenceNumber
order by userId, investmentType, productName
""".trimIndent())
        }
    }


    val UserAudit = reportDefinition {
        name = "User Audit"
        description = "User data extract"
        query {
            neo4j("""
match (u:User)
return
    u.userId as userId,
    u.USER_STATUS as userStatus,
    u.ONBOARDING_STATUS as onboardingStatus,
    u.REVIEW_STATUS as reviewStatus,
    toInteger(u.IS_LOCKED) as isLocked,
    u.CREATED_DATE_TIME as createdDateTime,
    u.UPDATED_DATE_TIME as updatedDateTime
""".trimIndent())
        }
    }


    val AnswerAudit = reportDefinition {
        name = "Answers Audit"
        description = "Answers audit report"
        query {
            sql("""
SELECT OUI.USER_ID as USER_ID,
    C.ONBOARDING_ID,
    CQ.CONVERSATION_ID,
    CQ.CREATED_DATE_TIME,
    CQ.QUESTION_ID,
    CQ.ANSWER_TYPE,
    DBMS_LOB.substr(answer_json,4000) AS ANSWER,RECOMMENDED_STRATEGY,rpo.updated_date_time
FROM ONBOARDING_SVC.ONBOARDING_USER_IDENTITY OUI
JOIN ONBOARDING_SVC.CONVERSATIONS C ON OUI.ID=C.ONBOARDING_ID
JOIN ONBOARDING_SVC.CONVERSATION_QUESTIONS CQ ON C.ID=CQ.CONVERSATION_ID
LEFT OUTER JOIN RISK_PROFILING_SVC.RISK_PROFILING_OUTPUT RPO ON C.RISK_PROFILE_ADVICE_ID= RPO.ID
WHERE CQ.ANSWER_TYPE is not null
  AND CQ.ANSWER_TYPE in (
    'horizonAnswer',
    'objectiveAnswer',
    'financialInvestmentsExpAnswer',
    'personalExpectationsAnswer',
    'lossAversionAnswer',
    'uncertaintyAversionAnswer',
    'emotionalExperienceAnswer',
    'investmentTemperamentAnswer',
    'strategySelectionAnswer',
    'genderAnswer',
    'maritalStatusAnswer',
    'industryAnswer',
    'idConfirmationAnswer',
    'fatcaDeclarationAnswer',
    'sourceOfWealthAnswer',
    'finalConfirmationAnswer',
    'investmentTypeAnswer',
    'isaFormAndConfirmationAnswer',
    'kbaInitiateAnswer',
    'kbaQuizAnswer'
)
ORDER BY USER_ID, CQ.QUESTION_ID
""".trimIndent())
        }
    }


}



