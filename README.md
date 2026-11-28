# Prumo Gestão

Aplicativo Android para organização financeira e operacional de pequenos negócios. O Prumo reúne o acompanhamento de caixa, vendas, estoque e indicadores gerenciais em uma interface única, com armazenamento local e recursos de exportação e backup.

<p align="center">
  <strong>Controle financeiro e operacional com informações organizadas em um só lugar.</strong>
</p>

<p align="center">
  <a href="https://github.com/empresarioconectado-hub/Prumo/actions/workflows/build-apk.yml"><img src="https://github.com/empresarioconectado-hub/Prumo/actions/workflows/build-apk.yml/badge.svg?branch=main" alt="Android Build and Tests"></a>
  <img src="https://img.shields.io/badge/Plataforma-Android-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Linguagem-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
</p>

## Visão geral

O Prumo foi desenvolvido para apoiar a rotina de pequenos negócios que precisam acompanhar entradas e saídas, registrar vendas, controlar produtos e entender o resultado operacional. Os dados de trabalho são mantidos no próprio dispositivo Android.

O aplicativo é uma ferramenta de gestão gerencial. Não substitui ERP fiscal, sistema contábil, emissão de documentos fiscais ou orientação profissional.

## Funcionalidades

| Área | Recursos disponíveis |
| --- | --- |
| **Início** | Visão consolidada dos principais indicadores financeiros e operacionais. |
| **Caixa** | Registro de entradas e saídas, contas, categorias, formas de pagamento e situação dos lançamentos; acompanhamento de saldo e projeção de caixa. |
| **Estoque** | Cadastro de produtos, saldo, custo, preço, estoque mínimo, ponto de reposição e histórico de movimentações. |
| **Vendas** | Registro de vendas, cliente, vendedor, quantidade, receita, custo, impostos e comissão; integração da venda com caixa e estoque. |
| **DRE gerencial** | Indicadores de receita, custos, margem de contribuição, despesas e resultado gerencial. |
| **Equipe** | Cadastro de vendedores, metas e percentuais de comissão. |
| **Despesas fixas** | Registro e acompanhamento de despesas por categoria. |
| **Privacidade visual** | Opção para ocultar valores monetários nas telas compatíveis. |
| **Relatórios** | Exportação da DRE gerencial em PDF e dos lançamentos mensais de caixa em CSV. |
| **Backup** | Exportação e restauração manual dos dados em arquivo JSON versionado. |

Na primeira utilização, o usuário pode iniciar com registros próprios ou carregar dados demonstrativos fictícios. A demonstração só é inserida em uma base sem registros e não substitui dados existentes nem configurações já informadas.

A disponibilidade de cada recurso deve ser conferida na versão instalada. Funcionalidades em planejamento não são consideradas parte da versão atual.

## Tecnologias

- **Kotlin** — linguagem principal.
- **Jetpack Compose e Material 3** — interface declarativa.
- **Room** — persistência relacional local.
- **Preferences DataStore** — armazenamento das configurações do negócio.
- **Kotlin Coroutines e Flow** — operações assíncronas e observação de dados.
- **AndroidX Lifecycle e ViewModel** — estado e ciclo de vida da interface.
- **Gradle Kotlin DSL** — configuração e automação de build.
- **JUnit e Robolectric** — testes automatizados.

Versões e dependências estão centralizadas em [gradle/libs.versions.toml](gradle/libs.versions.toml).

## Requisitos

| Item | Configuração |
| --- | --- |
| Plataforma | Android |
| Versão mínima | Android 7.0 (API 24) |
| SDK de compilação | API 36 |
| Java para o ambiente de CI | JDK 21 |
| Sistema de build | Gradle 9.3.1 |
| Identificador do aplicativo | `br.com.prumoplanilhas.gestao` |

Para desenvolvimento, utilize Android Studio compatível com o Android Gradle Plugin e com o SDK configurado no projeto.

## Como obter o código

```bash
git clone https://github.com/empresarioconectado-hub/Prumo.git
cd Prumo
```

## Preparação do ambiente

O workflow de integração contínua prepara o Gradle Wrapper a partir da versão definida em `gradle/wrapper/gradle-wrapper.properties`. Caso os scripts do wrapper não estejam presentes no clone, gere-os com uma instalação local do Gradle 9.3.1:

```bash
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
chmod +x gradlew
```

Configure o Android SDK pelo Android Studio ou pelas variáveis de ambiente do seu sistema. Em seguida, valide o ambiente:

```bash
./gradlew --version
```

## Compilação e testes

Executar os testes locais:

```bash
./gradlew test
```

Gerar o APK de depuração:

```bash
./gradlew assembleDebug
```

O artefato de depuração é gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para instalar em um dispositivo conectado com depuração USB habilitada:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O workflow [Android Build and Tests](.github/workflows/build-apk.yml) executa os testes unitários, compila o APK de depuração e publica o artefato temporário do GitHub Actions. O artefato do workflow tem retenção configurada de 14 dias.

## Estrutura do projeto

```text
Prumo/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── data/          # Room, DAO, repositório, backup e configurações
│       │   │   ├── model/         # Entidades e modelos de domínio
│       │   │   ├── ui/            # Compose, telas, componentes e ViewModel
│       │   │   └── util/          # Formatação e exportação de relatórios
│       │   └── res/               # Recursos Android
│       ├── test/                  # Testes locais automatizados
│       └── androidTest/           # Testes instrumentados
├── docs/                          # Documentação funcional e técnica
├── .github/workflows/             # Integração contínua
├── gradle/                        # Catálogo e configuração do Gradle
└── README.md
```

## Documentação

- [Arquitetura da aplicação](docs/arquitetura.md)
- [Módulos e regras de negócio](docs/modulos-e-regras-de-negocio.md)
- [Modelo de dados](docs/modelo-de-dados.md)
- [Backup e restauração](docs/backup-e-restauracao.md)
- [Segurança](docs/seguranca.md)
- [Testes e integração contínua](docs/testes-e-ci.md)
- [Compilação e publicação](docs/build-e-publicacao.md)
- [Política de Privacidade](docs/politica-de-privacidade.md)
- [Termos de Uso](docs/termos-de-uso.md)
- [Histórico de alterações](CHANGELOG.md)
- [Como contribuir](CONTRIBUTING.md)
- [Como reportar vulnerabilidades](SECURITY.md)

## Privacidade e armazenamento

Os registros operacionais são persistidos localmente por meio do Room, enquanto as preferências do negócio são armazenadas pelo DataStore. O aplicativo oferece backup manual em JSON. O backup automático do Android está desativado na configuração atual do manifesto.

Os arquivos exportados passam a estar sob controle de quem os salva ou compartilha. Proteja o dispositivo e mantenha os backups em local seguro.

Consulte a [Política de Privacidade](docs/politica-de-privacidade.md) para conhecer o tratamento de dados descrito para esta versão.

## Escopo e limitações

- Os indicadores e relatórios são gerenciais e dependem dos dados cadastrados.
- O aplicativo não realiza integração bancária nesta versão.
- O backup é uma operação manual; o usuário deve manter cópias atualizadas.
- O PDF da DRE é um demonstrativo gerencial, não um documento fiscal ou escrituração contábil.
- A aprovação do build automatizado não substitui testes funcionais em dispositivos Android reais.
- A documentação de privacidade deve ser revisada pelo responsável pelo produto antes de qualquer distribuição pública, incluindo a definição de um canal de contato válido.

Consulte [docs/modulos-e-regras-de-negocio.md](docs/modulos-e-regras-de-negocio.md) para o escopo funcional e [docs/build-e-publicacao.md](docs/build-e-publicacao.md) para os cuidados de distribuição.

## Contribuição

Contribuições são bem-vindas. Antes de abrir uma alteração, leia o [guia de contribuição](CONTRIBUTING.md), descreva o problema, mantenha o escopo objetivo e execute os testes aplicáveis.

## Licença

Este repositório ainda não contém um arquivo de licença. Portanto, o código não deve ser tratado como software de código aberto com permissões de uso, modificação ou redistribuição previamente concedidas. A licença deverá ser definida pelo responsável pelo projeto antes de anunciar oficialmente os termos de distribuição.
