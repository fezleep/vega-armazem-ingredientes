# Decisões técnicas

1. Inicializar o projeto e definir o esquema e o contrato.
2. Implementar cadastro e movimentações sob bloqueio de compartimento. Verificar compilação.
3. Validar contrato, virada civil, atomicidade e concorrência com H2.
4. Integrar dashboard estática à API e exercitar os fluxos.
5. Documentar evidências, revisar o pacote e preparar o Git local.

O Spring Boot 3.5.16 e o springdoc 2.10.1 seguem a [matriz de compatibilidade do springdoc](https://springdoc.org/v2/). Java 21 é o alvo do compilador.

O saldo atual é derivado dos ingredientes com quantidade positiva. O último tipo e a data de uso ficam no compartimento. Para um compartimento vazio sem restrição diária, capacidade, unidade e espaço livre dependem do tipo consultado. A listagem geral devolve esses campos como nulos nessa situação. Consultas de disponibilidade projetam a capacidade do tipo solicitado.

O nome é aparado e normalizado com Locale.ROOT para a chave única. O texto exibido preserva a caixa original. A API aceita SECOS como alias de SECO. As movimentações preservam os nomes e códigos da operação como valores históricos.

A escrita usa isolamento READ COMMITTED e bloqueio pessimista da linha do compartimento. As rotas de movimentação consultam primeiro apenas o ID escalar do compartimento, obtêm o bloqueio e então carregam o ingrediente e os saldos. O instante é lido após o bloqueio, para que a espera na virada do dia conte no calendário correto. A gravação do histórico usa a mesma transação.

O esquema SQL idempotente cria tabelas e insere os cinco compartimentos ausentes sem redefinir linhas existentes. O Hibernate valida o esquema. Alterações futuras de esquema exigiriam migrações versionadas. O H2 atende à execução local. Uma implantação em outro SGBD precisa revalidar bloqueios, isolamento e tempos de espera.
