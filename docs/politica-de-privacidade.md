# Política de Privacidade — Prumo Gestão

**Última atualização: 27 de novembro de 2026**

Esta política descreve o tratamento de informações na versão Android do Prumo Gestão, aplicativo de organização financeira e operacional para pequenos negócios.

O responsável pelo aplicativo deverá revisar este documento, identificar formalmente o controlador dos dados e disponibilizar um canal de contato válido antes de distribuir o produto publicamente.

## 1. Informações tratadas

O aplicativo permite que o usuário cadastre informações relacionadas à operação do negócio, incluindo:

- lançamentos de caixa, valores, categorias, contas, datas e formas de pagamento;
- vendas, nomes de clientes e vendedores;
- produtos, quantidades, custos, preços e movimentações de estoque;
- despesas fixas;
- configurações do negócio, como nome, saldo inicial e parâmetros gerenciais;
- arquivos de backup exportados ou importados pelo próprio usuário.

Essas informações são fornecidas pelo usuário durante o uso do aplicativo. A versão descrita não solicita credenciais bancárias nem oferece conexão com contas de instituições financeiras.

## 2. Armazenamento e processamento

Os registros operacionais são armazenados localmente no dispositivo Android, por meio do banco de dados Room. As preferências do negócio são mantidas localmente pelo Preferences DataStore.

Na versão documentada, os registros financeiros e operacionais não são sincronizados com um servidor do Prumo. O manifesto Android desativa o backup automático do aplicativo e não declara permissão de notificações, pois notificações programadas não fazem parte desta versão. A cópia de segurança disponível é iniciada manualmente pelo usuário, por meio da exportação de um arquivo JSON criptografado com senha. A senha não é armazenada pelo aplicativo. Backups legados sem criptografia podem ser importados para migração.

O acesso ao dispositivo desbloqueado pode permitir a visualização das informações. O bloqueio opcional do Prumo utiliza biometria ou credencial de bloqueio do Android e impede capturas de tela/prévias recentes enquanto ativo. Backups novos são protegidos por AES-256-GCM; arquivos legados sem criptografia continuam legíveis. O usuário deve proteger o aparelho, a senha e os arquivos gerados.

## 3. Compartilhamento e serviços de terceiros

O aplicativo permite salvar arquivos de relatório e backup em destinos escolhidos pelo usuário por meio dos mecanismos do Android. Caso o usuário copie ou compartilhe esses arquivos por outros aplicativos, o tratamento posterior será regido pelas políticas e condições do serviço de destino.

O Prumo não controla a disponibilidade, a segurança ou as práticas de privacidade de serviços de terceiros escolhidos pelo usuário.

## 4. Permissões

A versão atual não declara permissões de notificações, câmera, localização ou acesso a contatos. A funcionalidade de notificações programadas não integra o escopo funcional disponível.

As permissões efetivamente solicitadas podem variar conforme a versão e os recursos implementados. O usuário pode consultar e gerenciar as permissões concedidas nas configurações do Android.

## 5. Retenção e exclusão

Os registros permanecem no dispositivo até que o usuário:
- os exclua pelas funções disponíveis no aplicativo;
- utilize a opção de apagar os dados;
- limpe os dados do aplicativo nas configurações do Android; ou
- desinstale o aplicativo.

A opção de apagar todos os dados remove permanentemente os registros locais abrangidos pela função e não pode ser desfeita. Recomenda-se exportar um backup antes de confirmar a operação.

Arquivos exportados não são apagados automaticamente quando os dados do aplicativo são removidos. O usuário deve localizar e excluir separadamente as cópias armazenadas ou compartilhadas.

## 6. Segurança

O aplicativo utiliza os mecanismos de armazenamento e proteção disponibilizados pelo Android e mantém os registros operacionais localmente. O modo de privacidade visual apenas oculta valores nas telas compatíveis; não equivale a criptografia. O bloqueio opcional depende da credencial configurada no Android. A criptografia do backup protege somente o arquivo exportado, não substitui a proteção do dispositivo nem cifra o banco Room local.

Nenhum mecanismo elimina todos os riscos. Recomenda-se utilizar bloqueio de tela, manter o sistema atualizado e não compartilhar backups com pessoas não autorizadas.

## 7. Direitos dos titulares

A Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018) pode ser aplicável ao tratamento de dados pessoais realizado no contexto de uso do aplicativo.

Como os registros operacionais desta versão permanecem no dispositivo do usuário e não são enviados ao servidor do Prumo, o próprio usuário dispõe das funções locais de consulta, exportação e exclusão. Para solicitações que dependam do responsável pelo produto, deverá ser disponibilizado um canal de contato válido e monitorado.

Esta descrição não substitui a avaliação jurídica do fluxo de dados, das responsabilidades do controlador e dos operadores ou das obrigações legais aplicáveis à distribuição do produto.

## 8. Público e uso adequado

O aplicativo é uma ferramenta de apoio à gestão de pequenos negócios. Não foi documentado como serviço bancário, instituição de pagamento ou ferramenta de consulta a dados financeiros de instituições.

O usuário deve evitar inserir dados pessoais excessivos ou informações que não sejam necessárias à gestão do negócio.

## 9. Alterações desta política

Esta política poderá ser atualizada quando houver mudanças no aplicativo, nos dados tratados, nas permissões, nos serviços integrados ou na legislação aplicável. A versão vigente deverá permanecer acessível aos usuários.

## 10. Identificação e contato do responsável

**Responsável pelo aplicativo:** informação a ser preenchida pelo mantenedor do Prumo.

**Canal de contato para privacidade:** deve ser definido e monitorado pelo responsável antes da distribuição pública.

Não publique o aplicativo em uma loja enquanto os dados de identificação e o canal de contato exigidos para a operação não estiverem preenchidos e revisados.
