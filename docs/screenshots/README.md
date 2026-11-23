# Capturas da aplicação

As seis imagens desta pasta foram capturadas no Microsoft Edge com a aplicação em execução e dados fictícios. A captura do Swagger mostra a porta temporária usada durante a validação.

Inicie o servidor em uma pasta com um banco limpo ou use um banco separado com o argumento `--spring.datasource.url=jdbc:h2:file:./data/demonstracao`. Abra `http://localhost:8080/` e use nomes de ingredientes e responsáveis fictícios. Use uma janela de pelo menos 1280 px para a visão geral.

| Arquivo | Como reproduzir |
| --- | --- |
| `01-dashboard.png` | Cadastre Farinha de Trigo com 400 kg em C1, Água com 175 L em C2, Fermento fresco com 60 kg em C3 e Aveia com 80 kg em C4. Retire os 80 kg de aveia para mostrar a restrição de troca. Capture os totais e os cinco cards. |
| `02-cadastro.png` | Registre um ingrediente e capture o formulário com a confirmação. |
| `03-movimentacao.png` | Reponha 25,5 kg de farinha. Abra a retirada, preencha 10 kg e um responsável de demonstração, capture o diálogo e confirme. |
| `04-historico.png` | Navegue a Histórico e capture as linhas de entrada e saída com data, quantidade e responsável. |
| `05-validacao.png` | Tente retirar 999 kg de farinha. Capture a mensagem de saldo insuficiente no diálogo. |
| `06-swagger.png` | Abra `http://localhost:8080/swagger-ui/index.html` e expanda o cadastro para mostrar o contrato. |

Salve os PNG nesta pasta, enquadre apenas a aplicação e revise os arquivos antes de compartilhar.
