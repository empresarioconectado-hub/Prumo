# Checklist de publicação do Prumo

Este documento é o gate de release. Marque cada item somente após existir evidência verificável. Um item não testado não pode ser considerado concluído.

## 1. Código e compilação

- [ ] Branch de release atualizada a partir da branch principal.
- [ ] Revisão de código concluída.
- [ ] Build debug concluído.
- [ ] Build release concluído com assinatura de produção.
- [ ] R8/ProGuard avaliado e regras testadas.
- [ ] Nenhuma credencial, keystore ou dado real no repositório ou artefatos públicos.
- [ ] Dependências diretas e transitivas revisadas.
- [ ] Licenças das dependências verificadas.
- [ ] Relatório de lint sem erros bloqueadores.

## 2. Integridade financeira

- [ ] Entradas e saídas testadas com valores mínimos, máximos e negativos inválidos.
- [ ] Arredondamento de impostos e comissões validado.
- [ ] Saldo inicial, saldo realizado e saldo projetado conferidos manualmente.
- [ ] Venda à vista e a prazo testadas.
- [ ] Edição e exclusão de venda testadas com estoque vinculado.
- [ ] Estoque insuficiente e overflow numérico testados.
- [ ] Movimentações de compra, venda, perda e ajuste testadas.
- [ ] Relatórios comparados com cálculos independentes.
- [ ] Cenários de concorrência e repetição de ação crítica avaliados.

## 3. Persistência e recuperação

- [ ] Migração da versão anterior testada com banco real de teste.
- [ ] Backup criptografado exportado e restaurado em instalação limpa.
- [ ] Senha incorreta rejeitada e perda de senha comunicada ao usuário.
- [ ] Backup legado sem criptografia importado e reexportado no novo formato.
- [ ] Backup inválido, truncado, grande e com referências inconsistentes rejeitado.
- [ ] Teste instrumentado de recuperação a partir de registro persistente passa.
- [ ] Encerramento forçado durante restauração e limpeza é testado em dispositivo real.
- [ ] Registro de recuperação corrompido mantém a interface financeira bloqueada e permite nova tentativa.
- [ ] Exportação e restauração preservam todos os campos.
- [ ] Política de retenção e armazenamento dos backups definida.

## 4. Segurança e privacidade

- [ ] Permissões do manifesto justificadas.
- [ ] Bloqueio de acesso e exposição em multitarefa avaliados.
- [ ] Política para logs e dados pessoais revisada.
- [ ] Backup criptografado ou risco formalmente aceito antes da distribuição.
- [ ] Bloqueio biométrico/credencial testado em aparelho com biometria e em aparelho apenas com PIN/padrão/senha.
- [ ] Bloqueio automático ao ir para segundo plano e FLAG_SECURE verificados em dispositivos reais.
- [ ] Política de Privacidade revisada por responsável competente.
- [ ] Termos de Uso revisados.
- [ ] Canal de contato válido publicado.
- [ ] Formulário de Segurança de Dados do Google Play preenchido com base no comportamento real.
- [ ] Exclusão de dados e atendimento a solicitações definidos, se aplicável.

## 5. Experiência e acessibilidade

- [ ] Fluxos principais testados em telefone compacto e grande.
- [ ] Tema claro e escuro revisados.
- [ ] Contraste e fonte ampliada verificados.
- [ ] TalkBack e ordem de foco testados.
- [ ] Estados vazios, carregamento, erro e sucesso revisados.
- [ ] Ações destrutivas exigem confirmação clara.
- [ ] Gráficos têm alternativa textual.
- [ ] Capturas de tela representam a versão final.

## 6. Testes em dispositivos

- [ ] Instalação limpa testada.
- [ ] Atualização sobre versão anterior testada.
- [ ] Retorno do app após encerramento forçado testado.
- [ ] Comportamento sem rede testado.
- [ ] Rotação e mudança de tamanho de fonte avaliadas.
- [ ] Consumo de memória e desempenho avaliados.
- [ ] Teste exploratório dos módulos Caixa, Estoque, Vendas e DRE concluído.

## 7. Google Play

- [ ] Conta de desenvolvedor e identidade do publicador confirmadas.
- [ ] Application ID definitivo confirmado.
- [ ] Nome, ícone, descrição curta e descrição completa revisados.
- [ ] AAB assinado e validado.
- [ ] Chave de upload protegida e com backup seguro.
- [ ] Classificação indicativa concluída.
- [ ] Público-alvo e declarações de conteúdo preenchidos.
- [ ] URL HTTPS pública da Política de Privacidade validada.
- [ ] Links da Política de Privacidade e dos Termos de Uso testados dentro do app.
- [ ] Declarações de anúncios, coleta, compartilhamento e segurança de dados correspondem ao app.
- [ ] Teste interno/fechado concluído conforme exigências vigentes da Play Console.
- [ ] Notas da versão e plano de rollback preparados.

## Bloqueios conhecidos a resolver antes de publicar

- Room e DataStore continuam sendo persistências separadas; o registro persistente de recuperação foi implementado para restauração e limpeza completa, com teste instrumentado. Ainda falta validar encerramento forçado, armazenamento cheio e falha de leitura do registro em dispositivos reais.
- A senha do backup e a experiência de recuperação ainda precisam ser validadas em dispositivos físicos.
- Não há comprovação de testes em dispositivos físicos nesta revisão.
- Não há evidência de AAB release assinado por chave de produção.
- Política de Privacidade e Termos exigem validação do responsável e canal de contato público real.
- Bloqueio biométrico forte com CryptoObject, fallback separado para credencial do Android e proteção de tela estão implementados no código, mas ainda precisam de validação em dispositivos reais.
- O namespace Kotlin/Android ainda usa com.example; confirmar o Application ID definitivo e avaliar a padronização do namespace antes da primeira publicação.

**Status:** não declarar publicação liberada enquanto qualquer bloqueio obrigatório estiver pendente.
