# Tema, usabilidade e acessibilidade

## Direção visual

O Prumo utiliza Jetpack Compose com Material 3 e uma identidade visual baseada em tons de verde, superfícies neutras e tipografia própria. Há esquemas claro e escuro, com seleção automática conforme a configuração do sistema.

A interface deve priorizar leitura rápida, hierarquia de informação e consistência entre os módulos. Valores financeiros precisam ter destaque sem competir com alertas, estados e ações primárias.

## Regras de consistência visual

- Utilizar cores semânticas do MaterialTheme em vez de cores avulsas sempre que possível.
- Reservar vermelho para saídas, erros e situações críticas; verde para entradas e estados positivos; amarelo/âmbar para atenção.
- Não comunicar status somente pela cor: sempre exibir rótulo ou ícone com descrição acessível.
- Manter escala tipográfica consistente e permitir ampliação de fonte do sistema.
- Usar alvos de toque com dimensão mínima recomendada de 48 dp.
- Manter espaçamento, raio de borda, elevação e estados de foco consistentes.
- Evitar excesso de cartões, bordas e informações repetidas em uma mesma tela.
- Tratar telas vazias com explicação breve e uma ação principal contextual.

## Usabilidade financeira

- Diferenciar saldo realizado, saldo projetado, valores a pagar e valores a receber.
- Exibir período e critérios dos indicadores próximos aos valores.
- Informar claramente quando um número é estimado, como tributos, comissões ou projeções.
- Confirmar ações destrutivas e explicar o impacto antes de excluir ou substituir dados.
- Evitar que uma venda vinculada seja alterada por um formulário de caixa independente.
- Exibir erros de validação junto ao campo que precisa de correção.
- Preservar os dados digitados quando uma operação falhar.
- Não assumir que todas as contas ou categorias pré-configuradas representam a realidade de cada negócio.
- Exibir estados de carregamento, sucesso, erro e ausência de dados.
- Ao ativar o bloqueio, exigir credencial segura do Android e impedir capturas/visualização do conteúdo em recentes.
- Ao retornar do segundo plano, solicitar novamente autenticação antes de exibir dados.

## Acessibilidade

Antes de cada release, verificar:
- contraste de texto e elementos não textuais nos temas claro e escuro;
- descrições de conteúdo para ícones que transmitam informação;
- navegação por TalkBack e ordem de foco;
- suporte a tamanho de fonte ampliado e display size;
- alvos de toque e espaçamento entre controles;
- campos com rótulos persistentes e mensagens de erro compreensíveis;
- gráficos acompanhados por valores ou resumo textual;
- estados não dependentes exclusivamente de cor;
- layouts em telas estreitas, tablets e orientação suportada.

## Melhorias de produto recomendadas

As funcionalidades abaixo são candidatas de evolução e não devem ser anunciadas como implementadas até existir código, testes e documentação correspondente:

1. **Contas a pagar e receber avançadas:** recorrência, parcelamento, baixa parcial, vencimento e alertas.
2. **Conciliação de caixa:** saldo por conta, conferência de fechamento e trilha de ajustes.
3. **Comprovantes:** anexos locais associados a lançamentos e vendas.
4. **Fornecedores e clientes:** cadastro, histórico, limites e visão de relacionamento.
5. **Compras e reposição:** pedido de compra, fornecedor, custo de aquisição e entrada de estoque.
6. **Inventário:** contagem física, divergências e ajuste auditável.
7. **Relatórios comparativos:** mês contra mês, margem por produto, despesas por categoria e evolução do caixa.
8. **Segurança de acesso:** bloqueio local com biometria/PIN, timeout e proteção de conteúdo sensível.
9. **Backup protegido:** criptografia autenticada, validação de integridade e fluxo de recuperação testado.
10. **Sincronização opcional:** somente após definir autenticação, isolamento por negócio, conflitos, criptografia em trânsito e em repouso e política de retenção.

## Critério de aceite de UX

Uma melhoria de interface só deve ser considerada concluída após:
- revisão visual em tema claro e escuro;
- teste em pelo menos um telefone compacto e um telefone de tela grande;
- verificação de fonte ampliada;
- validação TalkBack dos fluxos críticos;
- teste de formulários com dados inválidos e teclado aberto;
- confirmação de que a privacidade visual não deixa valores expostos em componentes secundários.
