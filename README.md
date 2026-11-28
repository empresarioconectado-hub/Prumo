# Prumo Gestão

Aplicativo Android de gestão financeira e operacional para pequenos negócios. O Prumo reúne fluxo de caixa, vendas, estoque, despesas, metas comerciais e indicadores gerenciais em uma experiência local-first.

<p align="center">
  <strong>Clareza financeira para decisões melhores no dia a dia.</strong>
</p>

<p align="center">
  <a href="https://github.com/empresarioconectado-hub/Prumo/actions/workflows/build-apk.yml"><img src="https://github.com/empresarioconectado-hub/Prumo/actions/workflows/build-apk.yml/badge.svg?branch=main" alt="Android Build and Tests"></a>
  <img src="https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white" alt="Android API 24 ou superior">
  <img src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin e Jetpack Compose">
  <img src="https://img.shields.io/badge/Armazenamento-Local-2563EB" alt="Armazenamento local">
</p>

## Sobre o produto

O Prumo foi concebido para micro e pequenos negócios que precisam registrar movimentações, acompanhar compromissos financeiros, controlar produtos e visualizar resultados sem depender de uma plataforma de gestão remota.

A versão atual utiliza armazenamento local no dispositivo. O aplicativo não oferece integração bancária, sincronização multiusuário ou emissão fiscal. Seus relatórios são gerenciais e não substituem escrituração contábil, ERP fiscal ou orientação profissional.

## Funcionalidades implementadas

| Módulo | Recursos |
| --- | --- |
| Início | Indicadores consolidados, alertas de caixa e estoque, visão mensal e projeção de caixa. |
| Caixa | Entradas e saídas, categorias, contas, meios de pagamento, situação de pagamento, filtros e histórico mensal. |
| Estoque | Produtos, custo, preço, saldo, estoque mínimo, ponto de reposição, sugestão de compra e histórico de movimentações. |
| Vendas | Cliente, vendedor, quantidade, receita, custo, impostos, comissão e situação do recebimento. |
| Integração da venda | Uma venda atualiza o estoque, o histórico de movimentações e o lançamento vinculado no caixa dentro de uma transação Room. |
| DRE gerencial | Receita bruta e líquida, tributos estimados, CMV, comissões, margem de contribuição, despesas fixas e resultado operacional gerencial. |
| Equipe | Cadastro de vendedores, metas mensais, comissões e ranking de vendas. |
| Despesas fixas | Lançamentos classificados por categoria para análise mensal. |
| Privacidade visual | Ocultação de valores monetários nas telas compatíveis. |
| Relatórios | Exportação de DRE gerencial em PDF e lançamentos do caixa em CSV. |
| Backup | Exportação e restauração manual de JSON versionado, protegido por senha com AES-256-GCM. |

A lista descreve recursos presentes no código. Itens como integração bancária, sincronização em nuvem, cobrança automática, emissão fiscal, leitura de código de barras e anexos de comprovantes não fazem parte desta versão. O bloqueio opcional por biometria ou credencial do Android está disponível, mas depende de configuração segura no próprio dispositivo.

## Tecnologias

- Kotlin e Android Gradle Plugin.
- Jetpack Compose e Material 3.
- Room para persistência relacional local.
- Preferences DataStore para preferências do negócio.
- Kotlin Coroutines, Flow e AndroidX Lifecycle.
- JUnit e Robolectric para testes locais.
- GitHub Actions para automação de testes, análise estática e build de depuração.

As versões estão centralizadas em [gradle/libs.versions.toml](gradle/libs.versions.toml).

## Requisitos de desenvolvimento

| Item | Configuração do projeto |
| --- | --- |
| Plataforma | Android |
| Versão mínima | Android 7.0 (API 24) |
| Compile SDK | API 36 |
| Target SDK | API 36 |
| JDK do CI | 21 |
| Gradle | Conforme `gradle/wrapper/gradle-wrapper.properties` |
| Application ID | `br.com.prumoplanilhas.gestao` |
| Namespace | `com.example` |

Utilize Android Studio e Android SDK compatíveis com as versões configuradas no projeto.

## Obter o código

```bash
git clone https://github.com/empresarioconectado-hub/Prumo.git
cd Prumo
```

O repositório mantém a configuração do Gradle Wrapper. Caso os scripts executáveis do wrapper não estejam presentes em um clone específico, gere-os com uma instalação compatível do Gradle, respeitando a distribuição indicada em `gradle-wrapper.properties`.

## Comandos de validação

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Para testes instrumentados em emulador ou dispositivo:

```bash
./gradlew connectedDebugAndroidTest
```

O APK de depuração é gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

O APK debug serve para desenvolvimento e validação. Não distribua esse artefato como versão de produção.

## Estrutura

```text
app/src/main/java/com/example/
├── data/       # Room, DAO, repositório, DataStore e backup
├── model/      # Entidades e modelos financeiros
├── ui/         # Compose, telas, componentes, tema e ViewModel
└── util/       # Formatação e exportação de relatórios
docs/           # Documentação funcional, técnica e de publicação
.github/        # Workflows, templates de issues e Pull Requests
gradle/         # Catálogo de dependências e configuração do wrapper
```

## Documentação do projeto

- [Arquitetura](docs/arquitetura.md)
- [Módulos e regras de negócio](docs/modulos-e-regras-de-negocio.md)
- [Modelo de dados](docs/modelo-de-dados.md)
- [Backup e restauração](docs/backup-e-restauracao.md)
- [Segurança](docs/seguranca.md)
- [Testes e integração contínua](docs/testes-e-ci.md)
- [Tema, usabilidade e acessibilidade](docs/usabilidade-e-acessibilidade.md)
- [Preparação e publicação](docs/build-e-publicacao.md)
- [Ficha do Google Play](docs/google-play-listing.md)
- [Checklist de publicação](docs/checklist-publicacao.md)
- [Política de Privacidade](docs/politica-de-privacidade.md)
- [Termos de Uso](docs/termos-de-uso.md)
- [Histórico de alterações](CHANGELOG.md)
- [Contribuição](CONTRIBUTING.md)
- [Relato de vulnerabilidades](SECURITY.md)

## Integridade financeira

O sistema utiliza centavos inteiros para representar valores monetários. As operações que relacionam vendas, caixa e estoque são executadas por métodos transacionais do Room. Movimentações manuais de estoque também devem atualizar o saldo e o histórico na mesma transação, com verificação de quantidade e proteção contra overflow aritmético.

O backup valida formato, versão, tamanho, campos, faixas numéricas, identificadores duplicados e referências entre registros antes de substituir as tabelas Room. As preferências são persistidas pelo DataStore, separado do banco Room; por isso, a restauração completa ainda exige testes específicos de falha entre as duas persistências.

## Privacidade e segurança

O manifesto desativa o backup automático do Android e não declara permissão de notificações que não seja utilizada pela versão atual. O modo de ocultação de valores é apenas visual; não equivale a criptografia. O bloqueio opcional usa a biometria ou credencial do dispositivo e protege a visualização do app em capturas e na tela de recentes enquanto ativado.

Os novos backups são protegidos por senha com PBKDF2 e AES-256-GCM. A senha não é armazenada pelo aplicativo; mantenha-a em local seguro. Backups antigos em JSON sem criptografia continuam aceitos para migração. A criptografia protege o arquivo exportado, não o banco local do aplicativo.

Consulte [Segurança](docs/seguranca.md) e [Política de Privacidade](docs/politica-de-privacidade.md).

## Situação de publicação

Um build verde é necessário, mas não suficiente para publicar. A distribuição exige validação funcional em dispositivos reais, testes de atualização e restauração, revisão de segurança e privacidade, assinatura de release, materiais da loja e confirmação das declarações do Google Play.

O checklist atualizado está em [docs/checklist-publicacao.md](docs/checklist-publicacao.md). Não considere a versão pronta enquanto houver itens obrigatórios pendentes.

## Licença

O repositório não contém uma licença de software. Até que o responsável defina e publique uma licença, não se deve presumir permissão de uso, modificação ou redistribuição do código.
