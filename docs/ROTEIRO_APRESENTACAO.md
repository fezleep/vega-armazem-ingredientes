# Roteiro de apresentação

Este roteiro organiza uma demonstração retrospectiva. A gravação contínua solicitada no desafio é uma evidência distinta.

1. Mostre o problema e os cinco compartimentos. Cadastre farinha e aveia em C1, destaque a soma da capacidade e apresente os totais separados por categoria.
2. Mostre o contrato no Swagger. Explique o alias SECOS, as quantidades em BigDecimal e a identidade por nome normalizado, categoria e compartimento.
3. Faça uma reposição e uma retirada. Mostre o histórico com data, evento, quantidade, local e responsável.
4. Compare uma retirada às 23h59 com uma tentativa de trocar a categoria no mesmo dia e outra à meia-noite. Abra o teste com Clock fixado e descreva o fuso America/Sao_Paulo.
5. Percorra a transação de movimentação no código. Destaque a consulta escalar, o bloqueio pessimista, a leitura do saldo após o bloqueio e o registro do evento na mesma transação.
6. Rode o Maven Wrapper e mostre os testes concorrentes e de rollback. Em cada corrida, duas chamadas disputam o mesmo compartimento e o teste verifica respostas, saldo final e histórico.
7. Mostre o H2 em arquivo e como o mesmo estoque reaparece depois de reiniciar. Explique a estratégia de inicialização, as limitações do H2 e o que precisaria ser validado em um SGBD de produção.
8. Descreva o uso do Codex e quais verificações foram executadas, com base no registro de validação.
