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

### Experiência e desempenho
- Adicionada seleção persistente de tema claro, escuro ou automático.
- Removidos links legais duplicados na tela de configurações.
- Criptografia, restauração e operações de arquivo de backup movidas para dispatchers de trabalho, evitando bloquear a interface.

### Segurança e integridade
- Backups exportados agora usam PBKDF2-HMAC-SHA256 e AES-256-GCM com senha definida pelo usuário; importação de backups legados permanece disponível.
- A derivação de chave é consistente entre versões Android, permitindo restaurar o arquivo em dispositivos com versões diferentes do sistema.
- Bloqueio opcional por biometria/credencial Android, com reautenticação no retorno ao app e proteção FLAG_SECURE.
- Modo de ocultação de valores aplicado também aos indicadores e detalhes financeiros de estoque.
- Validações de domínio centralizadas para impedir valores inválidos e operações que possam causar overflow.
- Operações de exclusão em lote e restauração usam registro persistente de recuperação com AtomicFile, permitindo restaurar o snapshot anterior após falha ou encerramento do processo.
- A inicialização verifica o registro de recuperação antes de exibir as telas financeiras; falhas de recuperação mantêm os dados ocultos e permitem nova tentativa.
- A autenticação biométrica foi vinculada a uma operação criptográfica AES-GCM protegida pelo Android Keystore, com fallback separado para a credencial do sistema.
- Movimentações de estoque atualizam saldo e histórico dentro de uma transação Room.
- Adicionada proteção contra overflow em entradas e estornos de estoque.
- Limpeza de dados operacionais centralizada em transação Room.
- Validação estrutural ampliada para backups JSON, incluindo tamanho máximo, faixas, identificadores e referências.
- Removida permissão de notificações não utilizada e desabilitado tráfego HTTP em texto claro.

### Interface e qualidade
- Corrigida a propagação do estado de ocultação de valores na projeção diária do caixa.
- Corrigido o saldo inicial exibido no fluxo de caixa para respeitar o período selecionado.
- CI ampliado com Android Lint, testes instrumentados em emulador e análise CodeQL; Dependabot habilitado para Gradle e GitHub Actions.
- Adicionados testes instrumentados para persistência DataStore e recuperação de backup interrompido.

### Documentação
- README reorganizado com escopo real da versão.
- Adicionadas diretrizes de usabilidade, acessibilidade e checklist de publicação.
### Loop 1 — experiência de primeiro acesso
- Adicionada orientação inicial para negócios sem registros, com atalhos para registrar a primeira entrada ou venda e opção de explorar dados de exemplo.
- Incluídas identificações de interface para apoiar testes automatizados dos atalhos de primeiro acesso.

### Loop 2 — venda rápida e comprovante compartilhável
- Adicionado formulário de venda rápida separado do formulário completo, com descrição, cliente, quantidade, total, forma de pagamento e situação do recebimento.
- Permite vincular opcionalmente um produto do estoque; quando vinculado, valida a disponibilidade e registra o custo calculado a partir do custo unitário cadastrado.
- Mantém a gravação da venda e os efeitos relacionados em caixa/estoque no fluxo transacional existente do Room.
- O DAO retorna o identificador persistido da venda e o repositório devolve o registro com esse ID; o ViewModel emite o evento de sucesso somente após a gravação.
- Adicionado comprovante textual com número real da venda, data, cliente, item, quantidade, total e pagamento.
- O compartilhamento utiliza `Intent.ACTION_SEND` e o Android Sharesheet. O usuário escolhe o destino e confirma o envio; não há integração direta com a API do WhatsApp nem envio automático.
- Incluído teste unitário para o conteúdo do comprovante.
- A venda rápida atual registra um item por venda. Carrinho com múltiplos produtos permanece como evolução futura.
