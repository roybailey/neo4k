// creates time tree indexes
CREATE INDEX ON :Year(value);
CREATE INDEX ON :Month(value);
CREATE INDEX ON :Day(value);

// create time tree with day depth
WITH range(2016, 2020) AS years, range(1,12) AS months
FOREACH(year IN years |
  CREATE (y:Year {value: year})
  FOREACH(month IN months |
    CREATE (m:Month {value: month})
    MERGE (y)-[:CONTAINS]->(m)
    FOREACH(day IN (CASE
                      WHEN month IN [1,3,5,7,8,10,12] THEN range(1,31)
                      WHEN month = 2 THEN
                        CASE
                          WHEN year % 4 <> 0 THEN range(1,28)
                          WHEN year % 100 = 0 AND year % 400 = 0 THEN range(1,29)
                          ELSE range(1,28)
                        END
                      ELSE range(1,30)
                    END) |
      CREATE (d:Day {value: day})
      MERGE (m)-[:CONTAINS]->(d))))
