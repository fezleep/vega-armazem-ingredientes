'use strict';

const porId = id => document.getElementById(id);
const categorias = { SECO: ['Seco', 'kg'], LIQUIDO: ['Líquido', 'L'], REFRIGERADO: ['Refrigerado', 'kg'] };
const numero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 });
const data = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'medium', timeZone: 'America/Sao_Paulo' });
const cadastro = porId('form-cadastro');
const ajuste = porId('form-movimento');
const dialogo = porId('movimentar');
let movimento;
let pedidoDisponibilidade;
let pedidoHistorico;
let cadastrando = false;

function elemento(tag, texto, classe) {
  const node = document.createElement(tag);
  if (texto !== undefined) node.textContent = texto;
  if (classe) node.className = classe;
  return node;
}

function mensagem(id, texto = '', classe = '') {
  const node = porId(id);
  node.textContent = texto;
  node.className = classe;
}

async function api(rota, opcoes) {
  let resposta;
  try {
    resposta = await fetch(rota, opcoes);
  } catch (erro) {
    if (erro.name === 'AbortError') throw erro;
    throw new Error('API indisponível. Verifique se o servidor está em execução.');
  }
  const corpo = await resposta.json().catch(() => null);
  if (!resposta.ok || corpo === null) {
    const detalhes = Object.entries(corpo?.fields ?? {}).map(([campo, texto]) => campo + ': ' + texto);
    throw new Error([corpo?.message ?? 'Resposta inesperada da API.', ...detalhes].join(' '));
  }
  return corpo;
}

function enviar(rota, corpo) {
  return api(rota, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(corpo)
  });
}

function formatar(valor, unidade) {
  return numero.format(valor) + (unidade ? ' ' + unidade : '');
}

function renderTotais(totais) {
  porId('totais').replaceChildren(...totais.map(item => {
    const [nome, unidade] = categorias[item.type];
    const card = elemento('div', undefined, 'total');
    card.append(elemento('span', nome), elemento('strong', formatar(item.totalQuantity, unidade)));
    return card;
  }));
}

function renderCompartimentos(compartimentos) {
  porId('compartimentos').replaceChildren(...compartimentos.map(item => {
    const card = elemento('article', undefined, 'card');
    const bloqueado = item.bloqueadoParaOutraCategoria;
    const categoria = categorias[item.tipoAtual ?? (bloqueado ? item.ultimoTipoUtilizado : '')]?.[0];
    const status = item.tipoAtual ? 'Ocupado' : bloqueado ? 'Vazio · troca bloqueada' : 'Vazio';
    const percentual = item.capacidade ? item.quantidadeTotal / item.capacidade * 100 : 0;
    const barra = elemento('progress');
    barra.max = 100;
    barra.value = percentual;
    barra.setAttribute('aria-label', 'Ocupação de ' + item.codigo);
    card.append(elemento('h3', item.codigo), elemento('span', status, bloqueado ? 'badge bloqueado' : 'badge'),
      elemento('p', categoria ?? 'Categoria livre', 'muted'),
      elemento('strong', formatar(item.quantidadeTotal, item.unidade) + ' · ' + numero.format(percentual) + '%'),
      barra, elemento('p', item.espacoLivre === null ? 'Espaço conforme a categoria'
        : 'Livre: ' + formatar(item.espacoLivre, item.unidade), 'muted'));
    return card;
  }));
}

function linha(valores) {
  const row = elemento('tr');
  valores.forEach(valor => row.append(elemento('td', valor)));
  return row;
}

function vazio(id, colunas, texto) {
  const row = linha([texto]);
  row.firstChild.colSpan = colunas;
  porId(id).replaceChildren(row);
}

function renderIngredientes(ingredientes) {
  if (!ingredientes.length) {
    vazio('ingredientes', 4, 'Nenhum ingrediente cadastrado.');
    return;
  }
  porId('ingredientes').replaceChildren(...ingredientes.map(item => {
    const row = linha([item.nome, item.compartimentoCodigo, formatar(item.quantidade, item.unidade)]);
    const acoes = row.insertCell();
    for (const [rota, rotulo] of [['entradas', 'Entrada'], ['retiradas', 'Retirada']]) {
      const botao = elemento('button', rotulo, 'secondary');
      botao.setAttribute('aria-label', rotulo + ' de ' + item.nome);
      botao.disabled = rota === 'retiradas' && item.quantidade === 0;
      botao.addEventListener('click', () => abrirMovimento(item, rota, rotulo));
      acoes.append(botao);
    }
    return row;
  }));
}

async function carregarHistorico() {
  pedidoHistorico?.abort();
  const pedido = new AbortController();
  pedidoHistorico = pedido;
  const parametros = new URLSearchParams({ sortBy: porId('ordenacao').value, order: porId('ordem').value });
  mensagem('historico-feedback', 'Carregando histórico…');
  try {
    const eventos = await api('/historico?' + parametros, { signal: pedido.signal });
    if (pedido.signal.aborted) return;
    if (eventos.length) {
      porId('movimentos').replaceChildren(...eventos.map(m => linha([
        data.format(new Date(m.data)), m.operacao === 'SAIDA' ? 'Saída' : 'Entrada', m.ingredienteNome,
        m.compartimentoCodigo, formatar(m.quantidade, categorias[m.tipo][1]), m.responsavel
      ])));
    } else {
      vazio('movimentos', 6, 'Nenhuma movimentação registrada.');
    }
    mensagem('historico-feedback');
  } catch (erro) {
    if (!pedido.signal.aborted) mensagem('historico-feedback', erro.message, 'erro');
  }
}

async function atualizar() {
  porId('atualizar').disabled = true;
  mensagem('status', 'Atualizando estoque…');
  const historico = carregarHistorico();
  try {
    const [compartimentos, totais, ingredientes] = await Promise.all([
      api('/compartimentos'), api('/ingredientes/volume'), api('/ingredientes')
    ]);
    renderCompartimentos(compartimentos);
    renderTotais(totais);
    renderIngredientes(ingredientes);
    mensagem('status', 'Estoque atualizado.');
  } catch (erro) {
    mensagem('status', erro.message, 'erro');
  } finally {
    await Promise.all([historico, consultarDisponibilidade()]);
    porId('atualizar').disabled = false;
  }
}

async function consultarDisponibilidade() {
  pedidoDisponibilidade?.abort();
  const pedido = new AbortController();
  pedidoDisponibilidade = pedido;
  const { tipo, quantidade, compartimentoId } = cadastro.elements;
  const anterior = compartimentoId.value;
  const salvar = cadastro.querySelector('[type=submit]');
  compartimentoId.replaceChildren(new Option('Selecione', ''));
  salvar.disabled = true;
  if (!tipo.value || !quantidade.value || !quantidade.validity.valid) {
    mensagem('compatibilidade', 'Informe categoria e quantidade válida.', 'muted');
    return;
  }
  mensagem('compatibilidade', 'Consultando compartimentos…', 'muted');
  const parametros = new URLSearchParams({ tipo: tipo.value, quantidade: quantidade.value });
  try {
    const itens = await api('/compartimentos/disponiveis?' + parametros, { signal: pedido.signal });
    if (pedido.signal.aborted) return;
    for (const item of itens) {
      compartimentoId.add(new Option(item.codigo + ' · ' + formatar(item.espacoLivre, item.unidade) + ' livres', item.id));
    }
    if (itens.some(item => String(item.id) === anterior)) compartimentoId.value = anterior;
    salvar.disabled = cadastrando || !itens.length;
    mensagem('compatibilidade', itens.length ? 'Selecione um compartimento compatível.' :
      'Nenhum compartimento comporta a entrada informada.', 'muted');
  } catch (erro) {
    if (!pedido.signal.aborted) mensagem('compatibilidade', erro.message, 'erro');
  }
}

function abrirMovimento(item, rota, rotulo) {
  movimento = { item, rota };
  ajuste.reset();
  mensagem('titulo-movimento', rotulo + ' de ' + item.nome);
  mensagem('saldo-atual', item.compartimentoCodigo + ' · saldo atual: ' + formatar(item.quantidade, item.unidade));
  mensagem('movimento-feedback');
  dialogo.showModal();
}

cadastro.addEventListener('submit', async evento => {
  evento.preventDefault();
  if (cadastrando) return;
  const dados = Object.fromEntries(new FormData(cadastro));
  const botao = cadastro.querySelector('[type=submit]');
  cadastrando = true;
  botao.disabled = true;
  mensagem('cadastro-feedback', 'Registrando cadastro…');
  try {
    const item = await enviar('/ingredientes', {
      ...dados, quantidade: Number(dados.quantidade), compartimentoId: Number(dados.compartimentoId)
    });
    cadastro.reset();
    mensagem('cadastro-feedback', item.nome + ' cadastrado em ' + item.compartimentoCodigo + '.', 'sucesso');
    cadastrando = false;
    await atualizar();
  } catch (erro) {
    cadastrando = false;
    mensagem('cadastro-feedback', erro.message, 'erro');
    await consultarDisponibilidade();
  }
});

ajuste.addEventListener('submit', async evento => {
  evento.preventDefault();
  const botao = ajuste.querySelector('[type=submit]');
  if (botao.disabled) return;
  const dados = Object.fromEntries(new FormData(ajuste));
  const { item, rota } = movimento;
  botao.disabled = true;
  porId('cancelar').disabled = true;
  mensagem('movimento-feedback', 'Registrando movimentação…');
  try {
    const saldo = await enviar('/ingredientes/' + item.id + '/' + rota,
      { ...dados, quantidade: Number(dados.quantidade) });
    dialogo.close();
    await atualizar();
    mensagem('status', 'Movimentação #' + saldo.movimentacaoId + ' registrada. Saldo: '
      + formatar(saldo.quantidade, item.unidade) + '.', 'sucesso');
  } catch (erro) {
    mensagem('movimento-feedback', erro.message, 'erro');
  } finally {
    botao.disabled = false;
    porId('cancelar').disabled = false;
  }
});

dialogo.addEventListener('cancel', evento => {
  if (porId('cancelar').disabled) evento.preventDefault();
});
porId('cancelar').addEventListener('click', () => dialogo.close());
porId('atualizar').addEventListener('click', atualizar);
porId('ordenacao').addEventListener('change', carregarHistorico);
porId('ordem').addEventListener('change', carregarHistorico);
cadastro.elements.tipo.addEventListener('change', consultarDisponibilidade);
cadastro.elements.quantidade.addEventListener('input', consultarDisponibilidade);
atualizar();
