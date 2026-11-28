# Arquitetura da aplicação

## Objetivo

O Prumo Gestão utiliza uma arquitetura Android organizada em camadas para separar interface, estado de apresentação, regras de aplicação e persistência. A estrutura atual privilegia execução local, fluxo reativo de dados e operações de escrita centralizadas no repositório.

## Visão em camadas

```text
┌─────────────────────────────────────────────┐
│                Interface                     │
│       Jetpack Compose / Material 3           │
│  Início · Caixa · Estoque · Vendas e DRE     │
└──────────────────────┬──────────────────────┘
                       │ eventos / estado
┌──────────────────────▼──────────────────────┐
│              ViewModel                       │
│       PrumoViewModel / StateFlow              │
│   agrega dados e coordena ações da tela       │
└──────────────────────┬──────────────────────┘
                       │ chamadas / Flow
┌──────────────────────▼──────────────────────┐
│              Repository                      │
│              PrumoRepository                  │
│     regras de aplicação e coordenação         │
└───────────────┬──────────────────┬───────────┘
                │                  │
┌───────────────▼────────┐  ┌──────▼───────────┐
│          Room          │  │ Preferences      │
│ Database / DAO / SQL   │  │ DataStore        │
│ dados operacionais     │  │ configurações    │
└────────────────────────┘  └──────────────────┘
```

O diagrama representa o fluxo principal da aplicação. Não implica a existência de uma API ou serviço remoto ativo.

## Componentes

### Interface

Localização: `app/src/main/java/com/example/ui/`

A interface é construída com Jetpack Compose e Material 3. As telas consomem estado do ViewModel e encaminham ações do usuário para operações de aplicação. O fluxo de venda rápida está em `components/QuickSaleDialog.kt`; o comprovante é formatado por `util/SaleReceiptFormatter.kt`.

Principais áreas:
- `screens/OverviewScreen.kt`: visão geral.
- `screens/CashFlowScreen.kt`: fluxo de caixa.
- `screens/StockScreen.kt`: estoque.
- `screens/SalesDreScreen.kt`: vendas e DRE.
- `components/`: formulários, diálogos, gráficos e componentes reutilizáveis.
- `theme/`: cores, tipografia e tema.

### ViewModel

Localização: `app/src/main/java/com/example/ui/viewmodel/PrumoViewModel.kt`

O ViewModel:
- observa os fluxos emitidos pelo repositório;
- consolida os dados necessários para a apresentação;
- calcula indicadores e projeções gerenciais;
- mantém o estado da tela;
- coordena operações disparadas pela interface por meio de coroutines.

### Repositório

Localização: `app/src/main/java/com/example/data/PrumoRepository.kt`

O repositório centraliza o acesso aos DAOs e às configurações. Também coordena operações que precisam manter registros relacionados consistentes, como registrar uma venda e refletir seus efeitos no caixa e no estoque.

### Persistência

- **Room:** entidades operacionais e consultas SQL.
- **Preferences DataStore:** configurações do negócio, como nome, saldo inicial, alerta mínimo e taxa padrão.
- **PrumoBackupManager:** serialização e validação do backup JSON.

### Exportação

Localização: `app/src/main/java/com/example/util/PrumoReportExporter.kt`

A camada de exportação produz:
- PDF com o resumo mensal da DRE gerencial;
- CSV com os lançamentos do fluxo de caixa do mês selecionado.

## Fluxo de leitura

1. A tela coleta o estado do ViewModel.
2. O ViewModel combina os fluxos de dados do repositório.
3. O repositório observa os dados do Room e do DataStore.
4. Alterações persistidas são propagadas pelos fluxos para atualizar a interface.

## Fluxo de escrita

1. O usuário confirma uma operação na interface.
2. A tela encaminha os dados ao ViewModel.
3. O ViewModel executa a operação em uma coroutine.
4. O repositório aplica as regras e chama o DAO ou DataStore.
5. O Room/DataStore persiste a alteração e os fluxos atualizam as telas.

## Fluxo de confirmação e compartilhamento da venda

1. A interface envia os dados da venda rápida ao ViewModel.
2. O ViewModel solicita ao repositório a gravação.
3. O DAO persiste a venda e suas alterações relacionadas na transação Room e retorna o identificador gerado pelo banco.
4. O repositório devolve o registro com o ID persistido.
5. Somente após o sucesso, o ViewModel emite o evento `saleSavedEvents`.
6. A interface apresenta o comprovante formatado por `saleReceiptText` e oferece compartilhamento por `Intent.ACTION_SEND` com o Android Sharesheet.

A abertura do comprovante não é antecipada à persistência. O ID exibido é o identificador retornado pelo Room, e não um número provisório criado na interface. O envio para WhatsApp ou outro destino depende da escolha e confirmação do usuário no aplicativo de destino.
## Persistência e transações

Operações que envolvem mais de uma tabela utilizam métodos transacionais do DAO. A movimentação manual de estoque atualiza saldo e histórico na mesma transação, valida quantidade positiva, impede saída acima do saldo e utiliza aritmética com detecção de overflow. A limpeza das tabelas operacionais também é executada em uma transação única. A venda mantém a atualização de caixa, estoque e histórico em uma transação Room.

A persistência do banco Room e as preferências do DataStore são mecanismos distintos. Não se deve presumir que uma transação Room englobe alterações do DataStore. Para restauração e limpeza completa, o PrumoBackupManager grava um snapshot anterior com AtomicFile, executa as alterações e remove o registro somente após concluir todas as gravações. Se o processo for encerrado no intervalo, a inicialização tenta restaurar o snapshot antes de abrir a interface. Se o registro não puder ser lido ou recuperado, a interface financeira permanece bloqueada e oferece nova tentativa.

## Decisões técnicas

- Valores monetários operacionais são armazenados em centavos inteiros (`Long`) para evitar imprecisão binária típica de ponto flutuante.
- Consultas observáveis utilizam `Flow`.
- Operações de persistência são suspensas e executadas fora do fluxo principal da interface.
- O banco local possui migração versionada.
- O backup JSON possui campo de versão de esquema.
- A restauração e a limpeza completa gravam um registro de recuperação em noBackupFilesDir antes de alterar Room e DataStore.
- MainActivity verifica e processa esse registro antes de exibir as telas financeiras.

## Evolução

Alterações de arquitetura devem preservar:
- integridade entre vendas, caixa e estoque;
- compatibilidade de migração do banco;
- possibilidade de exportar e restaurar os dados;
- separação entre interface e persistência;
- testes para regras financeiras e operações transacionais.


## Recuperação entre Room e DataStore

Room e Preferences DataStore continuam sendo armazenamentos independentes; não existe uma transação distribuída entre eles. A aplicação utiliza um protocolo de recuperação compensatória com AtomicFile para restauração de backup e limpeza completa. O snapshot anterior inclui dados operacionais e preferências locais necessárias à reversão. O registro é temporário, fica em noBackupFilesDir e é removido após a conclusão.

O teste instrumentado PrumoBackupRecoveryInstrumentedTest simula um registro deixado por uma operação interrompida e verifica a recuperação de dados e preferências. Ainda são necessários testes exploratórios em aparelhos reais, inclusive encerramento forçado em diferentes pontos da operação e recuperação de registro corrompido.
