# Backup e restauração

## Finalidade

O Prumo oferece exportação e restauração manual de dados em JSON para que o usuário possa guardar uma cópia dos registros e transferi-los entre instalações.

O backup não é automático. A responsabilidade por exportar, armazenar e proteger os arquivos é do usuário.

## Conteúdo do arquivo

O conteúdo financeiro interno mantém o formato `prumo-backup`, versão de esquema `1`, com configurações, lançamentos, produtos, movimentações, vendas, vendedores e despesas fixas. Valores monetários são serializados em centavos inteiros.

Na exportação, esse conteúdo é protegido por um envelope `prumo-encrypted-backup`:
- derivação de chave com PBKDF2 e 250.000 iterações;
- PBKDF2-HMAC-SHA256 em Android 8.0 (API 26) ou superior;
- PBKDF2-HMAC-SHA1 em Android 7.0/7.1 (API 24/25), para compatibilidade com a versão mínima;
- criptografia autenticada AES-256-GCM;
- salt aleatório de 16 bytes e IV aleatório de 12 bytes;
- senha definida pelo usuário com pelo menos 12 caracteres.

A senha não é salva no dispositivo nem incluída no arquivo. Sem ela, o backup criptografado não pode ser restaurado. A preferência de bloqueio biométrico/credencial é local ao dispositivo e não é transferida no backup.

## Exportar

1. Abra as configurações do Prumo.
2. Selecione a opção de exportação de backup.
3. Defina e confirme uma senha com pelo menos 12 caracteres.
4. Escolha o destino e confirme o salvamento do arquivo JSON criptografado.
5. Guarde a senha separadamente do arquivo e verifique se o arquivo foi salvo.

O nome sugerido pelo aplicativo inclui a identificação Prumo e um timestamp.

## Restaurar

1. Localize um arquivo de backup JSON válido.
2. Abra as configurações do aplicativo.
3. Selecione a opção de importação/restauração.
4. Escolha o arquivo.
5. Confirme a operação somente após verificar que o arquivo corresponde ao negócio e à cópia desejados.

A restauração verifica o identificador do formato, a versão do esquema, o limite de tamanho (10 milhões de caracteres para o conteúdo interno e 15 milhões para o envelope criptografado), campos obrigatórios, faixas numéricas, percentuais, identificadores duplicados e referências de vendas e lançamentos antes de substituir as tabelas Room. O histórico de movimentações mantém referências históricas mesmo quando um produto não está mais cadastrado.

**Atenção:** a restauração substitui os registros operacionais existentes. Faça uma exportação do estado atual antes de importar outro backup.

## Compatibilidade

A implementação atual aceita o envelope criptografado `prumo-encrypted-backup` versão 1, que contém o formato interno `prumo-backup` com `schemaVersion = 1`. Backups legados em JSON sem criptografia, no formato interno versão 1, continuam aceitos para facilitar migração. Arquivos de outros formatos ou versões não são compatíveis.

Ao modificar o modelo de dados:
- avalie o impacto no formato JSON;
- mantenha compatibilidade retroativa sempre que possível;
- implemente migração do formato quando necessário;
- adicione testes para exportação, validação e restauração.

## Segurança dos arquivos

O backup contém dados financeiros e operacionais do negócio. Trate-o como informação confidencial:
- armazene-o em local protegido;
- evite links públicos ou pastas compartilhadas;
- não o envie em issues ou Pull Requests;
- compartilhe apenas com destinatários autorizados;
- remova cópias que não sejam mais necessárias.

O aplicativo não controla a segurança do serviço ou dispositivo em que o usuário decidir armazenar o arquivo.

## Limitações

O backup é manual e depende de ação do usuário. Não há garantia de recuperação se o usuário perder o arquivo, se o arquivo estiver corrompido ou se a versão do formato deixar de ser suportada.


## Integridade e limitações atuais

A exportação lê um snapshot transacional do Room. A substituição das tabelas Room ocorre em uma transação; as preferências do negócio são gravadas posteriormente no DataStore, que é um armazenamento independente. Se uma das etapas falhar, o aplicativo tenta restaurar o snapshot e as preferências anteriores. Ainda não existe atomicidade distribuída e um encerramento abrupto do processo entre as duas gravações não pode ser compensado. Esse cenário precisa de teste específico e permanece como bloqueio para declarar recuperação plenamente resiliente.

A criptografia autenticada protege confidencialidade e integridade do conteúdo cifrado. A validação estrutural continua sendo executada após a descriptografia. Não restaure arquivos recebidos de origem desconhecida. Backups legados sem criptografia permanecem legíveis por compatibilidade e devem ser reexportados no formato protegido.
