# Backup e restauração

## Finalidade

O Prumo oferece exportação e restauração manual de dados em JSON para que o usuário possa guardar uma cópia dos registros e transferi-los entre instalações.

O backup não é automático. A responsabilidade por exportar, armazenar e proteger os arquivos é do usuário.

## Conteúdo do arquivo

O arquivo JSON contém:
- identificador de formato: `prumo-backup`;
- versão do esquema: `1`;
- data/hora de exportação em milissegundos;
- configurações do negócio;
- lançamentos de caixa;
- produtos;
- movimentações de estoque;
- vendas;
- vendedores;
- despesas fixas.

Os valores monetários são serializados em centavos inteiros.

## Exportar

1. Abra as configurações do Prumo.
2. Selecione a opção de exportação de backup.
3. Escolha o destino e confirme o salvamento do arquivo JSON.
4. Verifique se o arquivo foi salvo no local escolhido.

O nome sugerido pelo aplicativo inclui a identificação Prumo e um timestamp.

## Restaurar

1. Localize um arquivo de backup JSON válido.
2. Abra as configurações do aplicativo.
3. Selecione a opção de importação/restauração.
4. Escolha o arquivo.
5. Confirme a operação somente após verificar que o arquivo corresponde ao negócio e à cópia desejados.

A restauração verifica o identificador do formato, a versão do esquema, o limite de tamanho de 10 milhões de caracteres, campos obrigatórios, faixas numéricas, percentuais, identificadores duplicados e referências de vendas e lançamentos antes de substituir as tabelas Room. O histórico de movimentações mantém referências históricas mesmo quando um produto não está mais cadastrado.

**Atenção:** a restauração substitui os registros operacionais existentes. Faça uma exportação do estado atual antes de importar outro backup.

## Compatibilidade

A implementação atual aceita o formato `prumo-backup` com `schemaVersion = 1`. Arquivos de outros formatos ou versões não são compatíveis com o restaurador atual.

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

A substituição das tabelas Room ocorre em uma transação. As preferências do negócio são gravadas posteriormente no DataStore, que é um armazenamento independente. Portanto, ainda não existe atomicidade distribuída entre o banco e as preferências. Antes de uso em produção, devem ser adicionados testes de falha nessa fronteira e uma estratégia de recuperação que restaure também as configurações anteriores.

O arquivo JSON não é criptografado nesta versão. A validação estrutural reduz a aceitação de arquivos inválidos, mas não autentica a origem nem protege a confidencialidade do conteúdo. Não restaure arquivos recebidos de origem desconhecida.
