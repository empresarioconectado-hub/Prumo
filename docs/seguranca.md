# Segurança

## Escopo

Este documento descreve os controles observáveis na implementação atual e as práticas recomendadas para desenvolvimento e uso do Prumo. Ele não representa certificação, auditoria independente ou garantia de segurança absoluta.

## Dados no dispositivo

- Registros operacionais são armazenados em banco local Room.
- Preferências são armazenadas pelo Preferences DataStore.
- O manifesto Android define `android:allowBackup="false"`.
- O aplicativo disponibiliza exportação manual de backup JSON protegido por senha.
- A proteção usa PBKDF2-HMAC-SHA256 (250.000 iterações, senha em UTF-8) para derivação da chave e AES-256-GCM para confidencialidade e autenticação do conteúdo. O mesmo KDF é usado em todas as versões Android suportadas para permitir portabilidade do backup.
- O arquivo utiliza salt e IV aleatórios; a senha não é persistida pelo aplicativo.
- Backups antigos sem criptografia são aceitos para migração, mas devem ser reexportados no formato protegido.
- O manifesto desativa o tráfego HTTP em texto claro por meio de `usesCleartextTraffic=false`.
- A permissão de notificações foi removida porque o fluxo atual não utiliza notificações agendadas.
- O restaurador valida estrutura, faixas de valores, IDs duplicados e referências antes de substituir os dados Room.
- Movimentações manuais de estoque são persistidas junto ao saldo em uma transação Room.
- O modo de privacidade oculta valores em telas compatíveis, sem criptografar os dados.
- O bloqueio opcional utiliza AndroidX BiometricPrompt com biometria ou credencial de dispositivo.
- A ativação exige que o Android tenha PIN, padrão ou senha de bloqueio configurado.
- Com o bloqueio ativo, FLAG_SECURE impede capturas de tela e prévias do app na tela de recentes.
- O bloqueio é reaplicado ao retornar ao primeiro plano; valide biometria e fallback de credencial em aparelhos reais.

O acesso ao dispositivo desbloqueado ou a backups legados sem criptografia pode expor informações do negócio. Recomenda-se manter bloqueio de tela, atualizações de segurança e controle de acesso ao aparelho.

## Transporte e serviços remotos

A versão Android documentada utiliza persistência local para os registros operacionais. Não se deve presumir que exista sincronização remota ou integração bancária ativa.

Arquivos compartilhados pelo usuário passam a depender também dos controles de segurança do aplicativo e do serviço de destino.

## Segredos e credenciais

- Não armazene chaves privadas, senhas, tokens de produção ou arquivos de assinatura no repositório.
- Utilize variáveis de ambiente ou o mecanismo de secrets do ambiente de build.
- Mantenha `.env`, keystores e credenciais fora do controle de versão.
- Não inclua dados reais de clientes ou informações financeiras em testes, issues ou logs.
- Se um segredo for exposto, revogue-o e gere uma nova credencial imediatamente.

O arquivo `.env.example` contém apenas instruções e valores ilustrativos. Não deve receber credenciais reais.

## Integridade dos dados

Operações que alteram múltiplos registros relacionados devem utilizar transações Room quando apropriado. Alterações em vendas, caixa e estoque exigem atenção especial para evitar divergências.

Valores financeiros devem continuar representados em centavos inteiros. Alterações no banco precisam de migrações testadas e compatibilidade com o backup.

## Exclusão e backup

A limpeza de dados remove registros locais e não deve ser tratada como reversível. Exporte um backup antes de executar operações destrutivas.

A restauração substitui registros existentes e deve ser realizada somente com arquivo confiável. Não restaure arquivos recebidos de origem desconhecida.

## Relato responsável de vulnerabilidades

Consulte [SECURITY.md](../SECURITY.md) para o procedimento de comunicação de vulnerabilidades.

## Verificações recomendadas antes de uma versão pública

- revisar permissões declaradas no manifesto;
- revisar bibliotecas e versões utilizadas;
- verificar que nenhum segredo foi incluído no APK ou no Git;
- testar backup, restauração e exclusão;
- testar operações de venda e estoque com dados de teste;
- executar testes automatizados;
- realizar testes manuais em dispositivos Android compatíveis;
- revisar a Política de Privacidade e os Termos de Uso;
- definir um canal de contato válido para titulares e usuários.

## Limitações conhecidas

O bloqueio biométrico/credencial foi implementado, mas ainda exige validação em dispositivos reais. Room e DataStore não participam de uma transação comum; a restauração tenta compensar falhas entre as gravações, mas um encerramento abrupto do processo nesse intervalo ainda pode deixar preferências e dados em estados diferentes. O modo de privacidade é apenas visual. O uso de mecanismos de segurança do Android não elimina riscos de dispositivo comprometido, compartilhamento indevido ou engenharia social.
