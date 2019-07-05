package me.roybailey.neo4k.reporting.config

import me.roybailey.neo4k.reporting.reportDefinition


/* ************************************************************
    CID REPORTS FOR CMT
   ************************************************************ */


object ClientReports {

    val ProspectsWithoutStrategy = reportDefinition {
        name = "ProspectsWithoutStrategy"
        description = "Used by CMT to show all prospects that tried to onboard but were not given a strategy"
        query {
            neo4j("""
match (u:User)--(on:Onboarding)--(cn:Conversation)--(rp:RiskProfile)
where rp.RECOMMENDED_STRATEGY is null
optional match (cn)-[strategySelectionAnswer:PROVIDED]-(:Answer {name:'strategySelectionAnswer'})
optional match (cn)-[titleNamesAnswer:PROVIDED]-(:Answer {name:'titleNamesAnswer'})
where strategySelectionAnswer.selectedStrategyType is null
return u.USER_STATUS as userStatus, u.EMAIL_ADDRESS as userEmail,
       titleNamesAnswer.fullname as fullname, titleNamesAnswer.firstname as firstname, titleNamesAnswer.lastname as lastname,
       u.PHONE_NUMBER as phoneNumber, u.CREATED_DATE_TIME as createdDateTime, u.UPDATED_DATE_TIME as updatedDateTime,
       rp.RECOMMENDED_STRATEGY as recommendedStrategy, strategySelectionAnswer.selectedStrategyType as selectedStrategy,
       cn.totalAnswers as totalAnswers, cn.lastAnswer as lastAnswer
order by u.updated desc
""".trimIndent())
        }
    }

    val Prospects = reportDefinition {
        name = "Prospects"
        description = "Used by CMT to show all prospects that tried to onboard but have not completed"
        query {
            neo4j("""
match (u:User)
where not(u.USER_STATUS = 'CLIENT')
optional match (u)--(on:Onboarding)--(cn:Conversation)
optional match (cn)--(rp:RiskProfile)
optional match (cn)-[titleNamesAnswer:PROVIDED]-(:Answer {name:'titleNamesAnswer'})
optional match (cn)-[strategySelectionAnswer:PROVIDED]-(:Answer {name:'strategySelectionAnswer'})
return u.USER_STATUS as userStatus, u.ONBOARDING_STATUS as userOnboardingStatus, u.EMAIL_ADDRESS as userEmail,
       titleNamesAnswer.fullname as fullname, titleNamesAnswer.firstname as firstname, titleNamesAnswer.lastname as lastname,
       u.PHONE_NUMBER as phoneNumber, u.CREATED_DATE_TIME as createdDateTime, u.UPDATED_DATE_TIME as updatedDateTime,
       rp.RECOMMENDED_STRATEGY as recommendedStrategy, strategySelectionAnswer.selectedStrategyType as selectedStrategy,
       cn.totalAnswers as totalAnswers, cn.lastAnswer as lastAnswer
order by u.updated desc, u.created desc
""".trimIndent())
        }
    }

    val Conversations = reportDefinition {
        name = "Conversations"
        description = "Used by CMT to show all conversation answers"
        query {
            neo4j("""
match (u:User)--(on:Onboarding)--(cn:Conversation)
optional match (cn)--(rp:RiskProfile)

optional match (cn)-[annualIncomeAnswer:PROVIDED]-(:Answer {name:'annualIncomeAnswer'})
optional match (cn)-[annualExpensesAnswer:PROVIDED]-(:Answer {name:'annualExpensesAnswer'})
optional match (cn)-[oneOffExpensesAnswer:PROVIDED]-(:Answer {name:'oneOffExpensesAnswer'})
optional match (cn)-[existingInvestmentsAnswer:PROVIDED]-(:Answer {name:'existingInvestmentsAnswer'})
optional match (cn)-[financialInvestmentsExpAnswer:PROVIDED]-(:Answer {name:'financialInvestmentsExpAnswer'})
optional match (cn)-[emotionalExperienceAnswer:PROVIDED]-(:Answer {name:'emotionalExperienceAnswer'})
optional match (cn)-[lossAversionAnswer:PROVIDED]-(:Answer {name:'lossAversionAnswer'})
optional match (cn)-[uncertaintyAversionAnswer:PROVIDED]-(:Answer {name:'uncertaintyAversionAnswer'})
optional match (cn)-[investmentTemperamentAnswer:PROVIDED]-(:Answer {name:'investmentTemperamentAnswer'})
optional match (cn)-[personalExpectationsAnswer:PROVIDED]-(:Answer {name:'personalExpectationsAnswer'})
optional match (cn)-[initialInvestmentAnswer:PROVIDED]-(:Answer {name:'initialInvestmentAnswer'})
optional match (cn)-[horizonAnswer:PROVIDED]-(:Answer {name:'horizonAnswer'})
optional match (cn)-[objectiveAnswer:PROVIDED]-(:Answer {name:'objectiveAnswer'})
optional match (cn)-[strategySelectionAnswer:PROVIDED]-(:Answer {name:'strategySelectionAnswer'})

return u.ONBOARDING_STATUS as onboardingStatus, u.USER_STATUS as userStatus,
       cn.UPDATED_DATE_TIME as updatedDateTime, cn.totalAnswers as totalAnswers, cn.lastAnswer as lastAnswer,
       annualIncomeAnswer.annualIncome as annualIncome,
       annualExpensesAnswer.annualExpenses as annualExpenses,
       oneOffExpensesAnswer.oneOffExpenses as oneOffExpenses,
       existingInvestmentsAnswer.existingInvestments as existingInvestments,
       financialInvestmentsExpAnswer.hasExperience as hasExperience,
       financialInvestmentsExpAnswer.hasIndustryKnowledge as hasIndustryKnowledge,
       financialInvestmentsExpAnswer.hasInvestmentExperience as hasInvestmentExperience,
       financialInvestmentsExpAnswer.hasAcknowledged as hasAcknowledged,
       financialInvestmentsExpAnswer.validAnswer as validAnswer,
       emotionalExperienceAnswer.emotionalExperience as emotionalExperience,
       investmentTemperamentAnswer.investmentTemperament as investmentTemperament,
       personalExpectationsAnswer.personalExpectations as personalExpectations,
       lossAversionAnswer.lossAmount as lossAmount,
       uncertaintyAversionAnswer.gainAmount as gainAmount,
       horizonAnswer.horizon as horizon,
       objectiveAnswer.objective as objective,
       initialInvestmentAnswer.initialInvestment as initialInvestment,
       rp.RECOMMENDED_STRATEGY as recommendedStrategy,
       strategySelectionAnswer.selectedStrategyType as selectedStrategy,
       rp.EXTRA_DATA as extraData,
       rp.REQUEST as request,
       rp.RESPONSE as response

order by totalAnswers desc
""".trimIndent())
        }
    }


    val StrategyMismatch = reportDefinition {
        name = "StrategyMismatch"
        description = """
                    Shows customers with different investment product or holding in different fund product to their selected strategy.
                    It must pull the selected strategy from the customer, the product from active investments,
                    and resolve the product from fund holdings (asset positions).
                    Any mismatch is highlighted alongside the strategy review status and secondary case.
                    """.trimIndent()
        query {
            neo4j("""
match (c:Customer:Active:Client)-[:SELECTED_STRATEGY]->(ss:Strategy),
      (c)--(e:Email),
      (c)--(i:Investment:Active)--(ip:Product)
optional match (i)--(:Portfolio)--(pos:AssetPosition)--(:Asset)--(pp:Product)
optional match (c)--(pc:PegaCase)--(:PegaLabelType {name:'Strategy Review'})
with c,ss,e,i,ip,pos,pp,pc
where (ss.name <> ip.strategy)
   or (pos is not null and pp.strategy is null)
   or (ss.name <> pp.strategy)
return e.email as userEmail,
       c.fullname as fullname,
       i.INVESTMENT_TYPE as investmentType,
       ss.name as selectedStrategy,
       ip.strategy as imsProductStrategy,
       pp.strategy as imsAssetPositionStrategy,
       c.reviewStatus as crmReviewStatus,
       pc.pegaId as pegaReviewId,
       c.firstname as firstname,
       c.lastname as lastname,
       c.mobile as phoneNumber,
       c.CREATED_DATE_TIME as createdDateTime,
       c.UPDATED_DATE_TIME as updatedDateTime
""".trimIndent())
        }
    }


    val StrategyReview = reportDefinition {
        name = "StrategyReview"
        description = "Shows customers with active strategy reviews or next due dates."
        query {
            neo4j("""
match (c:Customer:Active:Client),
      (c)--(e:Email)
optional match (c)-[:LATEST]-(sr:StrategyReview)
optional match (c)--(pc:PegaCase)--(:PegaLabelType {name:'Strategy Review'})
with c,e,sr,pc,apoc.date.add(apoc.date.parse(c.reviewDateTime,'ms','yyyy/MM/dd HH:mm:ss'), 'ms', 300, 'd') < timestamp() as dueSoon
where sr.STATUS = 'ACTIVE' or dueSoon
return e.email as userEmail,
       c.fullname as fullname,
       apoc.date.format(apoc.date.add(apoc.date.parse(c.reviewDateTime,'s','yyyy/MM/dd HH:mm:ss'), 's', 365, 'd'),'s','yyyy/MM/dd') as reviewDueDate,
       apoc.date.format(apoc.date.parse(c.reviewDateTime,'s','yyyy/MM/dd HH:mm:ss'),'s','yyyy/MM/dd') as crmReviewDateTime,
       c.reviewStatus as crmReviewStatus,
       sr.STATUS as cmsReviewStatus,
       pc.pegaId as pegaReviewId,
       c.firstname as firstname,
       c.lastname as lastname,
       c.mobile as phoneNumber,
       c.CREATED_DATE_TIME as createdDateTime,
       c.UPDATED_DATE_TIME as updatedDateTime
order by c.reviewDateTime asc
""".trimIndent())
        }
    }


    val UnfundedInvestments = reportDefinition {
        name = "StrategyReview"
        description = "Shows customers with Unfunded Investments"
        query {
            neo4j("""
match (c:Customer:Active:Client)--(i:Investment:Active)
where i.FUNDED_DATE is null
with distinct c
match (c)--(e:Email)
optional match (c)--(isa:Investment:Active)--(:InvestmentType {name: 'ISA'})
optional match (c)--(core:Investment:Active)--(:InvestmentType {name: 'CORE'})
with e,c,isa,core
optional match (c)--(pcStrategyReview:PegaCase)--(:PegaLabelType {name:'Strategy Review'})
optional match (c)--(pcInboundISA:PegaCase)--(:PegaLabelType {name:'Inbound ISA'})
optional match (c)--(pcOutboundCall:PegaCase)--(:PegaLabelType {name:'Outbound Phone Call'})
return e.email as userEmail,
       c.fullname as fullname,
       c.firstname as firstname,
       c.lastname as lastname,
       c.mobile as phoneNumber,
       core.investmentId as coreId,
       core.CREATED_DATE_TIME as coreCreatedDateTime,
       core.UPDATED_DATE_TIME as coreUpdatedDateTime,
       core.FUNDED_DATE as coreFundedDate,
       isa.investmentId as isaId,
       isa.CREATED_DATE_TIME as isaCreatedDateTime,
       isa.UPDATED_DATE_TIME as isaUpdatedDateTime,
       isa.FUNDED_DATE as isaFundedDate,
       pcStrategyReview.pegaId as pegaStrategyReview,
       pcInboundISA.pegaId as pegaInboundISA,
       pcOutboundCall.pegaId as pegaOutboundCall
order by c.created asc
""".trimIndent())
        }
    }
}



