INSERT INTO compartimento (id, codigo)
SELECT s.id, s.codigo FROM (VALUES (1,'C1'),(2,'C2'),(3,'C3'),(4,'C4'),(5,'C5')) s(id,codigo)
WHERE NOT EXISTS (SELECT 1 FROM compartimento c WHERE c.id = s.id);
