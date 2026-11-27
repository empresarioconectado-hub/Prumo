# Modelo de dados

## Persistência

O banco local é gerenciado pelo Room. A definição das entidades está em `app/src/main/java/com/example/model/PrumoModels.kt`, e o acesso é centralizado em `PrumoDao.kt`.

Nome do arquivo do banco: `prumo_gestao.db`.

## Entidades

### `cash_entries` — lançamentos de caixa

| Campo | Tipo lógico | Descrição |
| --- | --- | --- |
| id | Long | Identificador local. |
| description | String | Descrição do lançamento. |
| amountCents | Long | Valor em centavos. |
| type | String | Entrada ou saída. |
| category | String | Categoria financeira. |
| account | String | Conta utilizada. |
| paymentMethod | String | Forma de pagamento. |
| status | String | Pago ou pendente. |
| dateMillis | Long | Data em milissegundos. |
| saleId | Long? | Vínculo opcional com uma venda. |

### `stock_items` — produtos

Armazena SKU, nome, categoria, unidade, quantidade, estoque mínimo, prazo de reposição, média diária de vendas, custo e preço unitários, quantidades vendidas e perdas e quantidade inicial.

Valores monetários são mantidos em centavos. Quantidades são representadas por números inteiros.

### `stock_movements` — histórico de estoque

Registra produto relacionado, SKU, nome do produto, tipo de movimentação, quantidade, custo unitário, observação de referência e data.

### `sale_records` — vendas

Armazena descrição, cliente, vendedor, quantidade, receita bruta, custo, percentuais de imposto e comissão, data da venda, produto relacionado e informações de pagamento.

### `seller_profiles` — vendedores

Armazena nome, meta mensal e percentual de comissão.

### `fixed_expenses` — despesas fixas

Armazena descrição, categoria, valor em centavos e data.

## Configurações

As configurações do negócio são armazenadas separadamente pelo Preferences DataStore:
- nome do negócio;
- saldo inicial em centavos;
- limite mínimo de caixa em centavos;
- taxa padrão de imposto.

As preferências não são uma tabela Room.

## Relacionamentos lógicos

O esquema utiliza identificadores locais e vínculos mantidos pela camada de aplicação. Entre os vínculos relevantes:
- um lançamento de caixa pode apontar para uma venda por `saleId`;
- uma venda pode apontar para um produto por `stockItemId`;
- uma movimentação de estoque registra o identificador do produto por `stockItemId`.

Esses vínculos são usados pela camada de repositório para manter caixa, vendas e estoque coerentes.

## Convenções financeiras

- Valores monetários são armazenados em centavos inteiros (`Long`).
- Percentuais são armazenados como números decimais (`Double`).
- A conversão para apresentação ocorre na camada de interface/exportação.
- Evite alterar campos monetários para ponto flutuante ou realizar cálculos financeiros com `Float`/ `Double` sem uma justificativa explícita.

## Migrações

As alterações de estrutura do banco devem ser acompanhadas por uma migração Room versionada em `PrumoDatabase.kt`.

Regras para alterações:
1. Atualizar a versão do banco.
2. Implementar a migração da versão anterior para a nova.
3. Preservar os dados existentes.
4. Adicionar testes para a migração e para os novos campos.
5. Atualizar este documento e o formato de backup, quando aplicável.

## Backup

O formato de backup JSON possui um identificador (`format = prumo-backup`) e uma versão de esquema (`schemaVersion`). A estrutura é descrita em [Backup e restauração](backup-e-restauracao.md).
