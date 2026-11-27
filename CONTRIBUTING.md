# Guia de contribuição

Obrigado pelo interesse em contribuir com o Prumo Gestão.

## Antes de começar

- Consulte o README e os documentos da pasta `docs/`.
- Verifique se já existe uma issue ou Pull Request relacionado.
- Para mudanças relevantes, abra uma issue descrevendo o problema e a solução proposta antes de iniciar o desenvolvimento.
- Não publique dados reais de clientes, informações financeiras, backups ou credenciais.

## Organização do trabalho

1. Crie uma branch a partir de `main`.
2. Utilize nomes objetivos, por exemplo:
   - `feat/descricao-curta`
   - `fix/descricao-curta`
   - `docs/descricao-curta`
   - `test/descricao-curta`
3. Mantenha cada Pull Request focado em um conjunto coerente de alterações.
4. Não inclua arquivos gerados, keystores, credenciais ou configurações locais.

## Padrões de implementação

- Utilize Kotlin e mantenha o estilo existente do projeto.
- Prefira nomes claros e responsabilidades bem delimitadas.
- Mantenha a interface desacoplada da persistência.
- Centralize operações de dados no repositório/DAO.
- Utilize transações Room para operações que alterem registros relacionados.
- Armazene valores monetários em centavos inteiros.
- Evite operações bloqueantes na thread principal.
- Preserve acessibilidade e consistência visual nas telas Compose.
- Não introduza dependências sem necessidade técnica clara.

## Banco de dados e backup

Toda alteração de esquema deve incluir:
- incremento da versão do banco;
- migração Room correspondente;
- testes de migração;
- revisão do backup JSON;
- atualização de `docs/modelo-de-dados.md` e `docs/backup-e-restauracao.md`, quando aplicável.

Não remova dados ou altere o significado de campos existentes sem documentar a estratégia de migração.

## Testes obrigatórios

Antes de abrir o Pull Request, execute:

```bash
./gradlew test
./gradlew assembleDebug
```

Quando a alteração afetar componentes Android ou fluxos de interface, execute também os testes instrumentados disponíveis e valide manualmente em emulador ou dispositivo.

Inclua testes para regras financeiras, limites, arredondamentos e operações transacionais.

## Pull Requests

Um Pull Request deve conter:
- objetivo e contexto da alteração;
- resumo das mudanças;
- comportamento anterior e novo, quando relevante;
- testes executados e seus resultados;
- impactos em banco de dados, backup, permissões ou privacidade;
- capturas de tela para mudanças visuais, quando úteis.

Use o modelo de Pull Request disponível em `.github/PULL_REQUEST_TEMPLATE.md`.

## Segurança

Não inclua segredos, dados pessoais, dumps de banco ou backups. Para vulnerabilidades, siga o procedimento de [SECURITY.md](SECURITY.md), em vez de abrir uma issue pública com detalhes exploráveis.
