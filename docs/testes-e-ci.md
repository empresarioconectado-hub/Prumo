# Testes e integração contínua

## Objetivo

O projeto mantém testes automatizados para apoiar a evolução das regras de negócio e utiliza GitHub Actions para verificar alterações em Pull Requests e branches.

## Tipos de teste

### Testes locais (unitários)

Localização: `app/src/test/`

Executados pelo Gradle com:

```bash
./gradlew test
```

O conjunto pode incluir testes JUnit e testes executados com Robolectric para cenários que dependem de APIs Android.

### Testes instrumentados

Localização: `app/src/androidTest/`

Esses testes são executados em dispositivo ou emulador Android configurado:

```bash
./gradlew connectedAndroidTest
```

A execução depende de um dispositivo/emulador disponível e não faz parte do fluxo de build principal descrito abaixo.

## Integração contínua

Workflow: [Android Build and Tests](../.github/workflows/build-apk.yml)

O workflow é acionado em:
- push;
- Pull Request;
- execução manual (`workflow_dispatch`).

Etapas:
1. checkout do repositório;
2. configuração do JDK 21;
3. preparação do Gradle Wrapper;
4. execução de `./gradlew test`;
5. geração de chave de assinatura de depuração temporária;
6. compilação com `./gradlew assembleDebug`;
7. publicação do APK como artefato do workflow.

O artefato `prumo-debug-apk` tem retenção de 14 dias.

## O que o CI confirma

Uma execução bem-sucedida confirma que:
- os testes incluídos no comando `test` passaram;
- o APK de depuração foi compilado;
- o artefato esperado foi gerado e carregado.

O workflow atual não comprova:
- funcionamento em todos os modelos de aparelhos;
- comportamento em produção;
- segurança por auditoria independente;
- aprovação na Google Play;
- execução de testes instrumentados, salvo se houver etapa específica adicionada ao workflow.

## Recomendações para novas alterações

Inclua ou atualize testes para:
- cálculos monetários e arredondamentos;
- projeções de caixa;
- ponto de reposição e movimentações;
- integração entre venda, caixa e estoque;
- exclusão de registros vinculados;
- migrações Room;
- exportação e restauração de backup;
- validação de formulários.

Antes de solicitar revisão:
1. execute os testes localmente;
2. gere o APK de depuração;
3. confira o resultado do GitHub Actions;
4. teste manualmente os fluxos afetados em um dispositivo ou emulador;
5. atualize a documentação correspondente.


## Pipeline atual

O workflow `.github/workflows/build-apk.yml` executa:
1. testes unitários do variant debug;
2. Android Lint;
3. geração do APK debug;
4. publicação do APK e do relatório de lint como artefatos temporários;
5. testes instrumentados em emulador Android.

A aprovação do pipeline comprova somente os cenários automatizados executados. Não substitui testes exploratórios, testes em aparelhos físicos, revisão de segurança ou validação de release assinado.
