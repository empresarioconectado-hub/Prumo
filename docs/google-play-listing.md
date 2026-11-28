# Preparação da ficha do Google Play — Prumo Gestão

Este documento é um rascunho factual da ficha. Não publicar textos com afirmações que ainda não tenham sido verificadas no AAB final.

## Identificação

- **Nome sugerido:** Prumo Gestão
- **Título curto sugerido:** Prumo Gestão Financeira
- **Categoria sugerida:** Negócios
- **Tipo:** aplicativo Android de gestão financeira e operacional para pequenos negócios
- **Público:** proprietários e operadores de pequenos negócios
- **Application ID:** `br.com.prumoplanilhas.gestao`
- **Target SDK:** 36 (Android 16)
- **Site de suporte:** definir e publicar pelo mantenedor
- **Contato do desenvolvedor:** definir e monitorar antes da publicação
- **Política de Privacidade:** `https://github.com/empresarioconectado-hub/Prumo/blob/main/docs/politica-de-privacidade.md` (provisória; requer identificação e contato do controlador)

## Descrição curta (até 80 caracteres)

Organize caixa, vendas, estoque e resultados do seu pequeno negócio.

## Descrição completa sugerida

O Prumo Gestão ajuda pequenos negócios a organizar informações financeiras e operacionais em um só aplicativo.

Com o Prumo, você pode:

- Registrar entradas e saídas do caixa.
- Acompanhar vendas, recebimentos e indicadores comerciais.
- Cadastrar produtos, custos, preços e níveis de estoque.
- Registrar compras, perdas e ajustes de estoque.
- Acompanhar vendedores, metas e comissões.
- Organizar despesas fixas.
- Consultar indicadores e uma DRE gerencial estimada.
- Exportar relatórios em PDF e CSV.
- Criar backups protegidos por senha para guardar e restaurar seus dados.

**Privacidade:** os registros são armazenados localmente no dispositivo. O aplicativo não oferece integração bancária nem sincronização em nuvem nesta versão. Os backups exportados são protegidos por senha; mantenha a senha em local seguro.

**Importante:** os relatórios são gerenciais e não substituem sistemas fiscais, escrituração contábil, apuração tributária ou orientação de profissional habilitado. O Prumo não é banco, instituição de pagamento ou consultor de investimentos.

## Declarações a confirmar na Play Console

### Segurança de dados

Rascunho baseado no manifesto e no código desta revisão:
- O aplicativo não declara permissão INTERNET.
- Os dados financeiros e operacionais são armazenados localmente em Room e Preferences DataStore.
- Não há integração com servidor próprio, analytics, anúncios ou sincronização em nuvem identificada no build Android.
- Exportações são iniciadas pelo usuário por meio dos mecanismos do Android.
- O bloqueio biométrico usa AndroidX BiometricPrompt; a biometria é processada pelo sistema Android, sem acesso do app aos dados biométricos.
- A senha do backup é usada localmente para derivação de chave e não é armazenada.

**Atenção:** a resposta final do formulário de Segurança de Dados deve ser conferida contra o AAB assinado, permissões mescladas, SDKs transitivos e comportamento efetivo. Não marque respostas definitivas apenas com base neste rascunho.

### Declaração de recursos financeiros

O Prumo oferece organização gerencial de caixa, vendas e estoque. Nesta versão, não:
- oferece conta bancária ou carteira;
- movimenta dinheiro ou processa pagamentos;
- concede empréstimos ou crédito;
- oferece investimentos, corretagem ou criptoativos;
- consulta saldos bancários;
- emite documentos fiscais.

A declaração de recursos financeiros da Play Console deve refletir o escopo real do aplicativo no momento do envio.

### Classificação indicativa e público-alvo

- Responder ao questionário de classificação indicativa na Play Console.
- Definir o público-alvo com base na finalidade empresarial do app.
- Não declarar o app como infantil.
- Confirmar todas as respostas sobre conteúdo, anúncios e coleta de dados.

## Materiais gráficos necessários

- Ícone de aplicativo em 512 × 512 px.
- Imagem de destaque (feature graphic) em 1024 × 500 px.
- Pelo menos duas capturas de tela reais de telefone, com dados fictícios e sem informações pessoais.
- Capturas adicionais para fluxos Caixa, Estoque, Vendas e DRE.
- Vídeo promocional é opcional; não produzir até que a interface final esteja congelada.

As capturas devem ser feitas na versão release candidata e não podem conter dados reais de clientes ou negócios.

## Requisitos técnicos de envio

- Gerar Android App Bundle (.aab), não distribuir APK debug.
- Assinar com chave de upload protegida e habilitar Play App Signing.
- Conferir versionCode e versionName.
- Testar instalação limpa e atualização sobre a versão anterior.
- Concluir teste interno/fechado conforme as exigências vigentes da Play Console.
- Publicar política de privacidade em URL HTTPS pública, acessível sem login, e manter link também dentro do app.
- Revisar as declarações legais e de segurança com o responsável pelo produto.

## Requisito de API

O projeto está configurado para target SDK 36. A documentação do Google Play informa que, a partir de 31 de agosto de 2026, novos aplicativos e atualizações devem segmentar Android 16 (API 36) ou superior para envio na categoria de apps móveis. Confirmar a exigência vigente na data do envio:

- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/9859455

## Bloqueios antes do envio

- [ ] Informar nome legal do responsável/controlador e canal de contato válido.
- [ ] Publicar a Política de Privacidade em URL HTTPS pública e confirmar que o link dentro do app funciona.
- [ ] Revisar os Termos de Uso.
- [ ] Confirmar as declarações de Segurança de Dados e recursos financeiros no AAB final.
- [ ] Criar ícone final, imagem de destaque e capturas reais.
- [ ] Configurar a chave de upload e as secrets de release.
- [ ] Validar o bloqueio biométrico/credencial em aparelhos reais.
- [ ] Concluir testes funcionais e de acessibilidade em dispositivos físicos.
