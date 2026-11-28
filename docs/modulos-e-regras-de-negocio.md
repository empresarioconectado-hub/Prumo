# Módulos e regras de negócio

Este documento descreve o escopo funcional implementado no código atual do Prumo Gestão. Os indicadores são destinados ao acompanhamento interno do negócio.

## 1. Visão geral

A tela inicial reúne informações de caixa, vendas, estoque e resultado do período. Os indicadores são calculados a partir dos registros cadastrados e das configurações do negócio.

### Primeira utilização e dados de demonstração

A instalação começa com uma base operacional vazia. O usuário pode cadastrar seus próprios dados ou carregar um conjunto fictício para conhecer o painel, o caixa, o estoque e a DRE.

A carga de demonstração é não destrutiva:
- só é permitida quando não existem registros operacionais;
- não apaga lançamentos, vendas, produtos, movimentações, vendedores ou despesas;
- preserva as configurações que o usuário já informou;
- impede chamadas concorrentes na mesma instância do repositório;
- informa ao usuário quando a base já contém registros e não permite a carga.

Para voltar a uma base vazia, a remoção de dados continua sendo uma ação separada, explícita e confirmada pelo usuário nas configurações. Dados demonstrativos são fictícios e não devem ser usados como registros reais do negócio.

## 2. Fluxo de caixa

O módulo de caixa trabalha com lançamentos de entrada e saída.

Cada lançamento pode conter:
- descrição;
- valor em centavos;
- tipo (entrada ou saída);
- categoria;
- conta;
- forma de pagamento;
- situação (pago ou pendente);
- data;
- vínculo com uma venda, quando aplicável.

O painel diferencia valores realizados e pendentes e apresenta projeções com base nos lançamentos disponíveis.

### Regras relevantes

- Lançamentos gerados por vendas possuem vínculo com o registro de venda.
- O repositório impede a edição ou exclusão isolada de um lançamento vinculado a uma venda, preservando a consistência do conjunto.
- A atualização da situação de um recebimento vinculado é propagada à situação de pagamento da venda por operação transacional.
- O saldo projetado é um indicador estimado, não uma confirmação de disponibilidade bancária.

## 3. Estoque

O módulo mantém cadastro de produtos e histórico de movimentações.

Dados principais:
- SKU;
- nome e categoria;
- unidade;
- quantidade atual;
- estoque mínimo;
- prazo de reposição;
- média diária de vendas;
- custo unitário;
- preço unitário;
- quantidade vendida e perdas acumuladas.

### Ponto de reposição

O ponto de reposição considera a demanda média durante o prazo de reposição acrescida do estoque mínimo:

`pontoDeReposicao = teto(mediaDiaria × prazoDeReposicao) + estoqueMinimo`

O status do item é classificado como:
- normal: quantidade acima do ponto de reposição;
- repor: quantidade positiva no limite ou abaixo do ponto de reposição;
- crítico: quantidade igual ou inferior a zero.

O custo estimado do estoque e o valor potencial de venda são indicadores calculados a partir das quantidades e dos valores unitários cadastrados.

### Movimentações

O histórico registra operações como compra, venda, perda e ajuste. Operações que alteram o saldo devem manter o histórico correspondente.

## 4. Vendas

O registro de venda contempla:
- descrição;
- cliente;
- vendedor;
- quantidade;
- receita bruta;
- custo;
- percentual de imposto;
- percentual de comissão;
- data;
- produto relacionado, quando aplicável;
- forma de pagamento, conta e situação.

Quando uma venda é registrada com vínculo a um item de estoque, o sistema atualiza o saldo e registra a movimentação correspondente. O repositório também mantém o lançamento de caixa associado à venda.

A exclusão ou alteração de uma venda deve respeitar os vínculos com caixa e estoque, evitando que um lançamento relacionado seja manipulado isoladamente.

## 5. Vendedores e metas

O cadastro de vendedores mantém nome, meta mensal e percentual de comissão. O painel calcula o volume de vendas atribuído e o progresso em relação à meta, conforme os registros existentes.

## 6. Despesas fixas

O módulo registra despesas com descrição, categoria, valor e data. Os dados alimentam os indicadores gerenciais do período.

## 7. DRE gerencial

A DRE apresentada pelo aplicativo é um resumo gerencial calculado com os dados informados pelo usuário.

Os principais componentes são:
- receita bruta;
- impostos sobre vendas;
- receita líquida;
- custo de mercadorias ou serviços;
- comissões;
- margem de contribuição;
- despesas fixas;
- resultado operacional (EBITDA gerencial);
- resultado líquido gerencial.

Os percentuais de impostos e comissões são aplicados conforme os valores e parâmetros cadastrados. A classificação e a qualidade dos dados de origem influenciam diretamente o resultado.

**Importante:** a DRE do aplicativo não equivale à escrituração contábil oficial, não apura tributos de forma definitiva e não substitui o trabalho de profissional contábil.

## 8. Configurações do negócio

As preferências incluem:
- nome do negócio;
- saldo inicial;
- valor mínimo para alerta de caixa;
- taxa padrão de imposto.

Essas preferências são armazenadas no DataStore.

## 9. Privacidade visual

O modo de privacidade oculta valores monetários nas telas que implementam essa opção. Trata-se de uma proteção visual para uso cotidiano, não de criptografia, autenticação ou bloqueio de acesso aos dados.

## 10. Relatórios e backup

- PDF: resumo da DRE gerencial do mês selecionado.
- CSV: lançamentos de caixa do mês selecionado.
- JSON: backup e restauração dos registros operacionais e configurações.

Os detalhes estão em [Backup e restauração](backup-e-restauracao.md).

## 11. Funcionalidades fora do escopo atual

Os itens abaixo não devem ser considerados disponíveis até que exista implementação e validação correspondente:
- autenticação ou bloqueio biométrico/PIN;
- sincronização em nuvem dos registros financeiros;
- integração bancária;
- contas recorrentes e parcelamentos;
- transferências entre contas com tratamento específico na DRE;
- notificações programadas por WorkManager;
- anexos de comprovantes;
- recibos compartilháveis dedicados.
