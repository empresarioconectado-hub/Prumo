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

A interface é construída com Jetpack Compose e Material 3. As telas consomem estado do ViewModel e encaminham ações do usuário para operações de aplicação.

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

## Persistência e transações

Operações que envolvem mais de uma tabela utilizam métodos transacionais do DAO quando necessário. O objetivo é evitar estados parciais em operações como movimentações de estoque e substituição dos dados durante a restauração do backup.

A persistência do banco Room e as preferências do DataStore são mecanismos distintos. Não se deve presumir que uma transação Room englobe alterações do DataStore.

## Decisões técnicas

- Valores monetários operacionais são armazenados em centavos inteiros (`Long`) para evitar imprecisão binária típica de ponto flutuante.
- Consultas observáveis utilizam `Flow`.
- Operações de persistência são suspensas e executadas fora do fluxo principal da interface.
- O banco local possui migração versionada.
- O backup JSON possui campo de versão de esquema.

## Evolução

Alterações de arquitetura devem preservar:
- integridade entre vendas, caixa e estoque;
- compatibilidade de migração do banco;
- possibilidade de exportar e restaurar os dados;
- separação entre interface e persistência;
- testes para regras financeiras e operações transacionais.
