# Histórico de alterações

As mudanças relevantes do Prumo Gestão são registradas neste arquivo. As entradas descrevem o estado do projeto e não substituem as notas específicas de cada release.

O projeto ainda não possui uma primeira versão pública estável formalmente publicada.

## Em desenvolvimento

### Funcionalidades e estrutura existentes

- Aplicativo Android desenvolvido em Kotlin e Jetpack Compose.
- Painel gerencial com indicadores de caixa, vendas e estoque.
- Lançamentos financeiros com status pago e pendente.
- Cadastro e movimentação de produtos.
- Registro de vendas com reflexos relacionados em caixa e estoque.
- Cadastro de vendedores, metas e comissões.
- Registro de despesas fixas.
- Indicadores de DRE gerencial.
- Modo de privacidade visual para valores monetários nas telas compatíveis.
- Exportação da DRE em PDF e do fluxo de caixa em CSV.
- Backup manual e restauração por arquivo JSON versionado.
- Persistência local com Room e Preferences DataStore.
- Testes automatizados e workflow de compilação do APK de depuração.

## Convenção de versionamento

Quando houver releases, cada versão deverá registrar:
- número e data da versão;
- funcionalidades adicionadas;
- correções;
- alterações incompatíveis ou migrações;
- limitações conhecidas.

Recomenda-se utilizar versionamento semântico para o nome da versão, mantendo o `versionCode` Android crescente.


## Unreleased — preparação para publicação

### Segurança e integridade
- Backups exportados agora usam PBKDF2-HMAC-SHA256 e AES-256-GCM com senha definida pelo usuário; importação de backups legados permanece disponível.
- A derivação de chave é consistente entre versões Android, permitindo restaurar o arquivo em dispositivos com versões diferentes do sistema.
- Bloqueio opcional por biometria/credencial Android, com reautenticação no retorno ao app e proteção FLAG_SECURE.
- Modo de ocultação de valores aplicado também aos indicadores e detalhes financeiros de estoque.
- Validações de domínio centralizadas para impedir valores inválidos e operações que possam causar overflow.
- Operações de exclusão em lote e restauração tentam recuperar o estado anterior em caso de falha.
- Movimentações de estoque atualizam saldo e histórico dentro de uma transação Room.
- Adicionada proteção contra overflow em entradas e estornos de estoque.
- Limpeza de dados operacionais centralizada em transação Room.
- Validação estrutural ampliada para backups JSON, incluindo tamanho máximo, faixas, identificadores e referências.
- Removida permissão de notificações não utilizada e desabilitado tráfego HTTP em texto claro.

### Interface e qualidade
- Corrigida a propagação do estado de ocultação de valores na projeção diária do caixa.
- Corrigido o saldo inicial exibido no fluxo de caixa para respeitar o período selecionado.
- CI ampliado com Android Lint, testes instrumentados em emulador e análise CodeQL; Dependabot habilitado para Gradle e GitHub Actions.

### Documentação
- README reorganizado com escopo real da versão.
- Adicionadas diretrizes de usabilidade, acessibilidade e checklist de publicação.
