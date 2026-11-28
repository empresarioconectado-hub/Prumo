# Compilação e publicação

## Identificação do aplicativo

| Propriedade | Valor |
| --- | --- |
| Nome | Prumo Gestão |
| Application ID | `br.com.prumoplanilhas.gestao` |
| Namespace Kotlin | `com.example` |
| Versão inicial configurada | `1.0` |
| Version code inicial | `1` |
| Min SDK | 24 |
| Target SDK | 36 |
| Compile SDK | 36 |

O `applicationId` identifica o aplicativo instalado e publicado. Depois de uma distribuição pública, alterações nesse identificador podem impedir a atualização sobre instalações existentes.

## Ambiente de build

- Android SDK com API 36.
- JDK 21 para reproduzir o ambiente de CI.
- Gradle 9.3.1, conforme `gradle-wrapper.properties`.
- Android Studio compatível com as versões de plugins configuradas.

As versões das dependências estão em `gradle/libs.versions.toml`.

## Build de depuração

Se o Gradle Wrapper ainda não estiver disponível no clone, gere os scripts usando Gradle 9.3.1:

```bash
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
chmod +x gradlew
```

Execute:

```bash
./gradlew test
./gradlew assembleDebug
```

Saída esperada:

```text
app/build/outputs/apk/debug/app-debug.apk
```

O APK de depuração utiliza assinatura de desenvolvimento e não deve ser distribuído como versão oficial.

## Assinatura de release

O build define uma configuração de assinatura release baseada em arquivo keystore e variáveis de ambiente. Os valores sensíveis não devem ser versionados.

Variáveis utilizadas:
- `KEYSTORE_PATH`: caminho do keystore;
- `STORE_PASSWORD`: senha do keystore;
- `KEY_PASSWORD`: senha da chave.

O workflow permite gerar o AAB por execução manual com `build_release=true`, desde que estejam configuradas as secrets:
- `RELEASE_KEYSTORE_BASE64`: conteúdo do keystore codificado em Base64;
- `RELEASE_STORE_PASSWORD`: senha do keystore;
- `RELEASE_KEY_PASSWORD`: senha da chave.

O alias esperado é `upload`. O workflow grava o keystore apenas no diretório temporário do runner e publica o AAB como artefato temporário. Não configure essas secrets até que a chave de upload de produção esteja criada e armazenada com backup seguro.

O alias configurado no Gradle é `upload`.

Antes de gerar uma versão release:
1. obtenha ou crie o keystore de upload em ambiente controlado;
2. mantenha pelo menos uma cópia de segurança segura do keystore;
3. configure as credenciais no ambiente de build;
4. nunca envie o keystore ou as senhas para o Git;
5. valide a assinatura e a instalação do pacote final.

A configuração atual não habilita minificação no build release. Avalie regras R8/ProGuard e testes de regressão antes de ativá-la.

## Versionamento

Para cada versão distribuída:
- incremente `versionCode`;
- atualize `versionName`;
- registre as mudanças em [CHANGELOG.md](../CHANGELOG.md);
- execute testes e build;
- valide instalação e atualização sobre a versão anterior;
- gere e arquive os artefatos e informações de assinatura em local seguro.

## Validação automatizada

O workflow atual executa testes unitários, Android Lint, build debug e testes instrumentados em emulador. Um workflow CodeQL analisa Kotlin/Java e o Dependabot monitora atualizações de dependências. Um job separado e manual gera o AAB assinado somente quando solicitado e quando as secrets de assinatura estiverem configuradas.

## Checklist de publicação

- [ ] Revisão do código e aprovação do Pull Request.
- [ ] GitHub Actions aprovado no commit de release.
- [ ] Testes funcionais em dispositivos reais e emuladores.
- [ ] Teste de migração a partir da versão anterior.
- [ ] Teste de exportação e restauração do backup.
- [ ] Verificação de permissões e declarações de dados da loja.
- [ ] Política de Privacidade publicada em URL pública e revisada.
- [ ] Termos de Uso revisados.
- [ ] Canal de contato do responsável definido e monitorado.
- [ ] Ícone, nome, capturas de tela e descrição da loja revisados.
- [ ] Keystore e credenciais protegidos.
- [ ] APK/AAB assinado e validado.
- [ ] Notas da versão preparadas.

## Situação

A existência de um workflow que compila o APK de depuração não significa que o aplicativo esteja pronto para publicação. A distribuição pública depende das verificações técnicas, operacionais, legais e de loja listadas acima.


## Bloqueios que não podem ser resolvidos apenas pelo GitHub

- A assinatura de produção depende de keystore e senhas que devem ser criados e guardados pelo proprietário.
- A política de privacidade precisa de URL pública e contato verdadeiro do responsável.
- A classificação, o público-alvo, o formulário de segurança de dados e os testes de publicação dependem da Play Console.
- Testes de hardware, TalkBack, biometria e comportamento em dispositivos reais exigem dispositivos ou emuladores executados.
