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

O conjunto inclui testes JUnit e testes executados com Robolectric para cenários que dependem de APIs Android. Entre as regras cobertas por `PrumoLogicTest`, estão conversão monetária, cálculos de estoque e DRE, arredondamento de valores financeiros e conteúdo do comprovante de venda.

### Testes instrumentados

Localização: `app/src/androidTest/`

Esses testes são executados em dispositivo ou emulador Android configurado. A suíte instrumentada inclui testes de transações Room, regras de venda/estoque, preferências DataStore e recuperação a partir de registro persistente:

```bash
./gradlew connectedAndroidTest
```

A execução depende de um dispositivo/emulador disponível. O workflow do GitHub Actions também executa testes instrumentados em um emulador Android após a aprovação da etapa de testes unitários, lint e build debug.

## Integração contínua

Workflow: [Android Build and Tests](../.github/workflows/build-apk.yml)

O workflow é acionado em:
- push;
- Pull Request;
- execução manual (`workflow_dispatch`).

Etapas:
1. checkout do repositório;
2. configuração do JDK 21;
3. preparação do Gradle Wrapper a partir da versão declarada no projeto;
4. execução de `testDebugUnitTest`, com repetição para falhas transitórias;
5. execução de `lintDebug` e publicação do relatório de lint;
6. geração de chave de depuração temporária;
7. compilação com `assembleDebug` e publicação do APK;
8. execução de testes instrumentados em emulador Android, condicionada ao sucesso da etapa anterior.

O workflow também possui uma execução manual opcional para gerar AAB assinado. Esse job exige as secrets de assinatura de produção descritas em [Compilação e publicação](build-e-publicacao.md).

O artefato `prumo-debug-apk` e o relatório de lint têm retenção de 14 dias.

## O que o CI confirma

Uma execução bem-sucedida confirma que:
- os testes incluídos no comando `test` passaram;
- o APK de depuração foi compilado;
- o artefato esperado foi gerado e carregado;
- a suíte instrumentada configurada no workflow concluiu, quando o job de emulador também estiver marcado como aprovado.

O workflow inclui testes instrumentados em emulador, mas não comprova:
- funcionamento em todos os modelos de aparelhos;
- comportamento em produção;
- segurança por auditoria independente;
- aprovação na Google Play;
- validação de todos os fluxos de interface e cenários de uso real.

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
5. testes instrumentados em emulador Android, incluindo PrumoDaoInstrumentedTest, SettingsDataStoreInstrumentedTest e PrumoBackupRecoveryInstrumentedTest.

A aprovação do pipeline comprova somente os cenários automatizados executados. Não substitui testes exploratórios, testes em aparelhos físicos, revisão de segurança ou validação de release assinado. O emulador do workflow usa uma imagem Android API 30 mais leve, com animações desativadas e repetição para falhas transitórias; a compatibilidade de publicação ainda deve ser validada em versões e aparelhos representativos.


## Segurança da cadeia de dependências

- Dependabot verifica semanalmente atualizações de Gradle e GitHub Actions.
- CodeQL analisa o código Kotlin/Java em Pull Requests, na branch principal e em agenda semanal.
- A revisão de dependências removeu aliases e integrações Firebase que não eram utilizadas pelo aplicativo Android.
