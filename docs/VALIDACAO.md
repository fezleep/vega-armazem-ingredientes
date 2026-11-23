# Registro de validação

## Ambiente

Execução via WSL com OpenJDK 21.0.11, Maven Wrapper 3.3.4 e Maven 3.9.9. O PowerShell foi usado para orquestrar os comandos. A distribuição Java estava disponível no WSL. Foi usado `JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2` nas rodadas de verificação.

## Checkpoints

| Etapa | Comando e evidência |
| --- | --- |
| Dependências | `mvn -B -ntp dependency:go-offline` no WSL resolveu Spring Boot 3.5.16, springdoc 2.10.1 e as dependências do projeto. |
| Primeira compilação | `sh ./mvnw -B -ntp test` compilou fontes e testes. Quatro asserções de listas falharam apesar dos corpos HTTP corretos. As comparações foram ajustadas para usar o matcher de listas do Hamcrest. |
| Primeira verificação completa | `./mvnw -B -ntp clean verify` terminou com 21 testes, zero falhas, zero erros e zero ignorados. |
| Verificação final | `./mvnw -B -ntp clean verify` terminou com `BUILD SUCCESS` e 22 testes, zero falhas, zero erros e zero ignorados. Gerou `target/armazem-1.0.0.jar`. |
| JavaScript | `node --check src/main/resources/static/app.js` terminou com código 0. Node foi usado apenas para verificar a sintaxe durante o desenvolvimento. |
| Navegador | Microsoft Edge automatizado via Playwright, com o JAR servido no WSL e encaminhamento local temporário. Cadastro, disponibilidade, entrada, retirada, rejeição por saldo insuficiente, ordenação do histórico, Swagger e largura móvel de 390 px foram exercitados. O JSON OpenAPI tinha dez operações. |
| Reinício | Dois processos sucessivos via `java -jar target/armazem-1.0.0.jar` usaram o mesmo H2 em arquivo isolado. Os JSONs de compartimentos, ingredientes e histórico eram iguais antes e depois. Permaneceram os cinco compartimentos, quatro ingredientes, sete eventos e o último uso de C4. |
| Git | `git init -b main` inicializou o repositório. `git add` foi impedido pelo sandbox ao criar `.git/index.lock`. |

## Cenários da suíte

- Testes unitários de ocupação e capacidade projetada para a categoria.
- MockMvc e H2 em memória para confirmar os cinco compartimentos, alias, limites exatos, excesso de 0,001, validação, duplicidade, troca de categoria, saldo zerado, volumes, disponibilidades e histórico.
- Virada civil às 02:59:59Z e 03:00:00Z com relógio controlado, correspondentes a 23:59:59 e 00:00:00 em São Paulo.
- Rejeição de ordinal numérico de categoria e de ID fracionário no JSON de cadastro.
- Falha deliberada de uma restrição do banco ao inserir o histórico, com teste do saldo, do cadastro e do último uso após a falha. O H2 registra a violação esperada no log.
- Chamadas concorrentes iniciadas por barreira, sem transação envolvendo o método de teste. Em cada corrida, a base usa duas transações reais. A suíte exige um sucesso e uma rejeição 400 e confere saldo e histórico.

| Corrida | Resultado verificado na rodada final |
| --- | --- |
| Duas reposições de 100 kg sobre 500 kg no mesmo compartimento | Uma reposição, total de 600 kg e três eventos contando o estoque inicial. |
| Duas retiradas de 8 kg sobre saldo de 10 kg | Uma retirada, saldo de 2 kg e dois eventos contando o estoque inicial. |
| Categorias diferentes disputando compartimento vazio | Um cadastro, um ingrediente e um evento. |

O H2 foi exercitado sob concorrência local. O comportamento de fila, os timeouts e o isolamento precisam ser revalidados em um eventual SGBD de produção. A execução do Wrapper nativo do Windows com JDK instalado no Windows fica fora da validação deste ambiente.
