// deletes the time tree graph
match (n)
where any (label IN labels(n) where label in ['Year','Month','Day'])
optional match (n)-[r]-(m)
delete n, r, m
