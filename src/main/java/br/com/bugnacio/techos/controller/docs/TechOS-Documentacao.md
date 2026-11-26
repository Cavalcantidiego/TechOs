# TechOS: Documentação (Versão 1)

> **Sobre esta versão:** o TechOS é um projeto de **portfólio e aprendizado**, feito por **um estudante**.
> Esta é a **versão 1**. Ela funciona para demonstração, mas tem limitações e pode ter falhas.
> Não foi feita para uso comercial: não tem login de usuários e não passou por revisão de segurança profissional.

---

## 1. O que é o TechOS

O TechOS é um sistema web para uma **assistência técnica** de computadores e eletrônicos. Ele ajuda a:

- cadastrar **clientes** e os **equipamentos** deles;
- controlar o **estoque de peças** (as peças são usadas nos reparos, não vendidas);
- abrir e acompanhar **Ordens de Serviço (OS)**;
- avisar o cliente quando o equipamento está pronto e informar a **garantia**;
- ver um **painel** com o resumo do dia.

Regra de negócio central: **a peça só sai do estoque por meio de uma OS** (ou por uma baixa avulsa justificada, como quebra ou perda). Toda movimentação fica registrada, para dar rastreabilidade.

---

## 2. Tecnologias usadas e para que servem

| Tecnologia | Para que serve no projeto |
|---|---|
| **Java 21** | Linguagem em que todo o sistema foi escrito. |
| **Spring Boot 4.1.2** | Framework que monta e inicia o sistema (servidor, configurações, ligações entre as partes). |
| **Spring MVC** | Recebe os acessos do navegador (as páginas) e decide o que responder. |
| **Spring Data JPA + Hibernate** | Conversa com o banco de dados sem escrever SQL na mão. Transforma classes Java em tabelas. |
| **Thymeleaf** | Cria as páginas HTML com dados reais (listas, nomes, valores). |
| **Bootstrap 5** | Deixa as telas bonitas e adaptáveis ao celular (carregado pela internet, via CDN). |
| **MySQL (via XAMPP)** | Banco de dados onde tudo fica gravado. O XAMPP traz o MySQL/MariaDB pronto para uso. |
| **Bean Validation** | Regras de preenchimento dos formulários (campo obrigatório, tamanho máximo, valor mínimo). |
| **Maven (mvnw)** | Baixa as bibliotecas e compila/roda o projeto. |

---

## 3. Como o sistema é organizado

O código fica em `src/main/java/br/com/bugnacio/techos/` e é dividido em camadas. Cada camada tem uma função:

```
Navegador  ->  Controller  ->  Service  ->  Repository  ->  Banco (MySQL)
   ^                |
   +---- Página HTML (Thymeleaf) <----+
```

| Pasta | Função | Exemplos |
|---|---|---|
| `model` | Os "moldes" dos dados. Cada classe vira uma tabela. | `Cliente`, `Equipamento`, `ItemEstoque`, `OrdemServico`, `PecaUsada`, `MovimentoEstoque`, `ReservaEstoque`, `AnexoOs` |
| `repository` | Busca, grava e apaga dados no banco. | `ClienteRepository`, `ItemEstoqueRepository` |
| `service` | Regras de negócio mais importantes. | `EstoqueService`, `OrdemServicoService`, `AnexoOsService` |
| `controller` | Recebe os cliques e acessos do usuário e escolhe a página. | `EstoqueController`, `OrdemServicoController`, `HomeController` |
| `resources/templates` | As páginas HTML. | `estoque/lista.html`, `os/detalhe.html`, `dashboard.html` |
| `resources/static` | Arquivos fixos (CSS, JavaScript). | `css/techos.css` |

### Por que separar em camadas?

Para que cada parte faça uma coisa só. A tela não conhece o banco, e a regra do estoque fica em **um único lugar** (`EstoqueService`). Assim, quando a regra muda, muda em um só arquivo.

---

## 4. Recursos de Java e do Spring usados (e como)

### Classes e entidades (`@Entity`)
Uma classe como `ItemEstoque` descreve uma peça (nome, código, quantidade, preço). Com `@Entity`, o Hibernate cria a tabela `item_estoque` automaticamente.

### Enums
Listas fixas de valores, para ninguém digitar errado. Exemplos: `StatusOS` (Recebido, Em análise, Concluída...), `TipoMovimento` (Entrada, Saída para OS, Devolução...) e as categorias de peça.

### Interfaces de repositório
`ItemEstoqueRepository extends JpaRepository` já traz salvar, buscar e apagar prontos. Também dá para criar buscas só pelo nome do método:
`findByNomeContainingIgnoreCaseOrCodigoContainingIgnoreCase...` significa "buscar por nome ou código, contendo o texto, sem diferenciar maiúscula de minúscula".

### Anotações (`@`)
Marcações que dão instruções ao Spring: `@Controller` (esta classe responde páginas), `@Service` (regra de negócio), `@GetMapping` e `@PostMapping` (qual endereço aciona qual método), `@Valid` (validar o formulário).

### Injeção de dependência
Em vez de criar objetos na mão, o controller recebe o que precisa **pelo construtor**, e o Spring entrega pronto.

### Validação (`@NotBlank`, `@Size`, `@Min`, `@Pattern`...)
Impede salvar dados inválidos, e a mensagem aparece em vermelho no campo. Ex.: o código da peça não aceita espaços; os preços precisam ser maiores que zero.

### `BigDecimal`
Tipo correto para dinheiro. O tipo `double` perde precisão nos centavos.

### `Optional`
Representa "pode ter ou não ter resultado" ao buscar algo no banco, evitando erros com valor nulo.

### Streams
Filtrar e somar listas de forma curta. Ex.: somar o valor das OS concluídas para o faturamento do painel.

### `@Transactional`
Garante que várias gravações aconteçam **juntas ou nenhuma**. Se faltar uma peça ao concluir uma OS, nada é baixado.

### Exceções
`IllegalArgumentException` leva uma mensagem pronta ao usuário (ex.: "Estoque insuficiente").

### Records
Formato curto de classe para carregar dados (ex.: o resultado do envio de fotos).

### Upload de arquivos (`MultipartFile`)
Recebe as fotos da OS. Elas são salvas na pasta `uploads/os` e o banco guarda só o nome gerado (UUID). Só são aceitas imagens (JPG, PNG, WEBP e GIF).

---

## 5. Banco de dados (tabelas principais)

| Tabela | O que guarda |
|---|---|
| `cliente` | Nome, telefone e e-mail. |
| `equipamento` | Marca, modelo, número de série, tipo e o cliente dono. |
| `item_estoque` | Peças: nome, código, código de barras, categoria, quantidade, mínimo, preços e se está inativa. |
| `movimento_estoque` | **Histórico**: cada entrada, reserva, saída, devolução e baixa avulsa (peça, quantidade, saldo, data/hora, OS, técnico, motivo). |
| `reserva_estoque` | Peças separadas por uma OS que ainda não foram usadas. |
| `ordem_servico` | A OS: equipamento, defeito, laudo, técnico, status, datas, valor e garantia. |
| `peca_usada` | Quais peças (e quantas) cada OS usa. |
| `anexo_os` | Fotos de cada OS. |

As tabelas são criadas automaticamente pelo Hibernate quando o sistema inicia (`ddl-auto=update`).

---

## 6. Regras de negócio

### Estoque
- **Entrada:** chegou peça (reposição). Fica no histórico com motivo.
- **Reserva:** ao adicionar a peça em uma OS, ela fica **reservada**. O estoque físico não muda, mas o "disponível" diminui. Duas OS não "prometem" a mesma peça.
- **Saída para OS:** quando a OS é concluída, as peças saem do estoque.
- **Devolução:** se a OS for reaberta ou cancelada, as peças voltam.
- **Baixa avulsa:** para perda, quebra ou correção de contagem. Exige **motivo** e **senha de administrador**.
- **Inativar:** esconde a peça das listas, mas guarda o histórico.
- **Alertas:** a peça aparece como **Baixo** (quantidade no mínimo ou abaixo) ou **Zerado**.
- O estoque **nunca fica negativo**.

### Ordem de Serviço
Status: **Recebido, Em análise, Aguardando peça, Em reparo, Concluída, Entregue, Cancelada.**
- Para **concluir**, o **laudo técnico** e o **valor do serviço** são obrigatórios.
- Depois de concluída, as peças da OS ficam **travadas** (para o estoque bater com o que foi usado).
- **Reativar** uma OS cancelada e **excluir** uma OS pedem a **senha de administrador**.
- A **garantia** (em dias) é contada a partir da data de conclusão.
- As peças **não são cobradas à parte**: o valor da OS é o do serviço.

### Senha de administrador
Fica no arquivo de configuração (`techos.senha-admin`). **Não escreva a senha real em arquivos públicos.**

---

## 7. Como rodar o projeto na sua máquina (com XAMPP)

### O que instalar (uma vez só)
1. **JDK 21 ou mais novo** (precisa ser JDK, não só JRE). Sugestão: Eclipse Temurin, em adoptium.net. Na instalação, marque **"Set JAVA_HOME variable"** e **"Add to PATH"**.
2. **XAMPP**, em apachefriends.org. Ele fornece o banco de dados (MySQL/MariaDB). O sistema roda pelo Java, não pelo Apache.
3. **Git** (opcional, só para baixar pelo comando). Dá para baixar o projeto em ZIP pelo botão verde **Code** do GitHub.

### Passo a passo
1. **Baixe o projeto:** `git clone https://github.com/Cavalcantidiego/TechOs.git` ou baixe o ZIP e extraia.
2. **Abra o XAMPP Control Panel** e clique em **Start** só no **MySQL**. O Apache não é necessário.
3. **Crie o banco:** abra `http://localhost/phpmyadmin` (se o Apache estiver desligado, clique em **Admin** na linha do MySQL no XAMPP). Clique em **Novo**, digite o nome `techos`, escolha a codificação `utf8mb4_general_ci` e clique em **Criar**.
4. **Abra o terminal (PowerShell) dentro da pasta do projeto** (a pasta que tem o arquivo `pom.xml`) e rode:
   ```
   .\mvnw.cmd clean spring-boot:run
   ```
   No Linux ou Mac: `./mvnw clean spring-boot:run`
   Na primeira vez demora, porque baixa as bibliotecas.
5. Quando aparecer `Started TechosApplication`, abra **http://localhost:8080** no navegador.
6. Para desligar, volte ao terminal e aperte `Ctrl + C`.

As tabelas são criadas sozinhas na primeira execução. O sistema começa vazio: cadastre um cliente, um equipamento e algumas peças para testar.

### Configuração padrão do projeto
O projeto já vem configurado para o XAMPP: banco `techos`, usuário `root` e **senha vazia** (padrão do XAMPP). Se o seu MySQL tiver senha, ou se você quiser outra senha de administrador, crie na **raiz do projeto** (ao lado do `pom.xml`) o arquivo `application-local.properties`:

```
spring.datasource.password=SUA_SENHA_DO_MYSQL
techos.senha-admin=SUA_SENHA_DE_ADMINISTRADOR
techos.ddd-padrao=11
```

Esse arquivo é lido por cima da configuração padrão e **não vai para o GitHub**. O `techos.ddd-padrao` é o DDD usado quando o telefone do cliente foi cadastrado sem DDD.

### Senha de administrador
A senha de demonstração é `admin123`. Ela serve só para testar. Para usar a sua, defina `techos.senha-admin` no `application-local.properties`.

### Se algo der errado

| Mensagem | O que fazer |
|---|---|
| `No compiler is provided in this environment` | Você está com JRE. Instale um JDK e abra o terminal de novo. |
| `The JAVA_HOME environment variable is not defined correctly` | Aponte o `JAVA_HOME` para a pasta do JDK (sem o `\bin`). |
| `Communications link failure` ou `Connection refused` | O MySQL do XAMPP não está ligado. Clique em **Start** nele. |
| `Unknown database 'techos'` | Falta criar o banco `techos` no phpMyAdmin (passo 3). |
| `Access denied for user 'root'` | O seu MySQL tem senha. Crie o `application-local.properties` com `spring.datasource.password=SUA_SENHA`. |
| `Port 8080 was already in use` | Já há outro programa na porta 8080 (outra execução do sistema ou o Tomcat do XAMPP). Feche-o, ou acrescente `server.port=8081` no `application-local.properties` e abra `http://localhost:8081`. |
| Aviso do Hibernate sobre a versão `5.5.5` do MySQL | Normal no XAMPP, que usa MariaDB. Pode ignorar. |

---

## 8. Guia do usuário (para quem não entende de computador)

### Painel (tela inicial)
Mostra o resumo: faturamento do mês, quantas OS estão abertas, aguardando peça e prontas para retirada, e quantas peças estão acabando. Clique em um número para ver a lista.
As seções **Últimas ordens**, **Precisam de reposição** e **Últimas movimentações** podem ser abertas ou fechadas pelos botões **Expandir / Recolher**.

### Cadastrar um cliente
1. Clique em **Clientes** no menu e em **Novo cliente**.
2. Preencha nome, telefone (com DDD) e e-mail.
3. Clique em **Salvar**.

Para procurar, digite o nome no campo de busca. Para voltar à lista completa, apague o texto.

> Não é possível excluir um cliente que já tem equipamento cadastrado. O sistema avisa.

### Cadastrar um equipamento
1. Clique em **Equipamentos** e em **Novo equipamento**.
2. Escolha o cliente, o tipo, e preencha marca, modelo e número de série.
3. Clique em **Salvar**.

> Não dá para cadastrar dois equipamentos com o mesmo número de série.

### Estoque de peças

**Cadastrar uma peça nova**
1. **Estoque** > **Novo item**.
2. Preencha nome, **código** (sem espaços, ex.: `SSD-001`), categoria, quantidade, estoque mínimo e preços.
3. **Salvar**.

**Chegou mais peça (entrada)**
Na linha da peça, digite a quantidade, escreva o motivo se quiser e clique em **Entrada**.

**Receber com leitor de código de barras**
1. Clique em **Receber (bipar)**.
2. Aponte o leitor para a etiqueta da peça. O sistema soma 1 no estoque e emite um som.
3. Para somar várias de uma vez: leia a etiqueta `#QTD5` e depois a da peça (soma 5). Depois disso, volta a somar 1.
4. Código não cadastrado: o sistema faz um som grave e mostra erro vermelho.

**Peça quebrou ou sumiu (baixa avulsa)**
Na linha da peça, clique em **Baixa**, informe a quantidade, o **motivo** e a **senha de administrador**.

**Ver o que aconteceu com uma peça**
Clique em **Histórico** na linha da peça. Aparecem todas as entradas e saídas, com data, hora, OS e técnico.

**Esconder uma peça (inativar)**
Clique em **Inativar**. Ela some da lista, mas o histórico é guardado. Para vê-la, clique em **Ver inativos** (e em **Reativar** para trazê-la de volta).

**Excluir uma peça**
Clique em **Excluir** e confirme. Apaga a peça e o histórico dela. Se a peça já foi usada em uma OS, o sistema não deixa excluir; nesse caso, use **Inativar**.

### Ordem de Serviço (OS)

**Abrir uma OS (quando o cliente chega)**
1. **Ordens de serviço** > **Nova OS**.
2. Escolha o equipamento, descreva o **defeito** e informe o **técnico** (nome completo).
3. Se quiser, anexe **fotos** do equipamento (por exemplo, riscos ou amassados de quando chegou).
4. Defina a **garantia** em dias e clique em **Salvar OS**.

**Usar peças do estoque na OS**
1. Na tela da OS, no campo **Adicionar peça**, digite o nome ou o código (ou leia a etiqueta com o leitor).
2. Escolha a peça na lista que aparece, informe a quantidade e clique em **Adicionar**.
3. A peça fica **reservada**. Ela ainda não saiu do estoque.
4. Para tirar uma peça da OS, clique em **Remover**.

**Acompanhar o andamento**
No quadro **Andamento**, escolha o novo status e clique em **Atualizar status**.

**Terminar o serviço**
1. Em **Editar dados**, escreva o **laudo técnico** (o que foi feito) e o **valor do serviço**.
2. Mude o status para **Concluída**. As peças saem do estoque.
3. Clique em **Avisar cliente pelo WhatsApp**. Abre a conversa com a mensagem pronta (equipamento pronto, valor e garantia). Basta enviar.
4. O botão **Comprovante** abre uma página para imprimir ou salvar em PDF, com os dados do serviço e a garantia.

**Cancelar uma OS**
Mude o status para **Cancelada**. As peças voltam ao estoque.

**Reativar uma OS cancelada**
Escolha outro status e digite a **senha de administrador**. Se alguma peça tiver acabado nesse meio tempo, o sistema avisa e a OS continua cancelada.

**Excluir uma OS**
No fim da tela da OS, clique em **Excluir esta OS** e informe a senha. Use só em caso de erro. Para encerrar uma OS normalmente, use **Cancelada**.

### Dúvidas comuns

| Pergunta | Resposta |
|---|---|
| Por que não consigo concluir a OS? | Faltam o laudo ou o valor do serviço. Preencha em **Editar dados**. |
| Por que não consigo mexer nas peças da OS? | A OS já foi concluída. Volte o status para **Em reparo** para alterar. |
| Por que o botão de WhatsApp não aparece? | O telefone do cliente está vazio ou sem DDD. Corrija no cadastro do cliente. |
| Por que a peça não aparece na busca da OS? | Ela está sem estoque disponível ou está inativa. |
| Apareceu "Senha incorreta". | Digite a senha de administrador definida na configuração do sistema. |

---

## 9. Limitações da versão 1

- **Não há login nem usuários.** Qualquer pessoa que abrir o sistema vê tudo. A senha de administrador protege só algumas ações.
- O **técnico é um texto digitado**, não há cadastro de técnicos.
- O aviso por **WhatsApp é um clique** (abre a conversa pronta). O envio totalmente automático exigiria a API oficial do WhatsApp.
- O aviso por **e-mail não está incluído** nesta versão.
- O comprovante **não é nota fiscal**.
- Anexos aceitam **somente imagens** (não vídeos).
- Não há relatórios avançados nem exportação para planilha.
- Não há testes automatizados.
- Foi feito por um estudante, então é natural encontrar falhas. Elas serão corrigidas nas próximas versões.

---

## 10. Ideias para as próximas versões

- Login com perfis (atendente, técnico, administrador).
- Cadastro de técnicos.
- Envio automático de WhatsApp e e-mail.
- Fotos das peças no estoque.
- Relatórios (faturamento, peças mais usadas).
- Testes automatizados.

---

## 11. Segurança e cuidados ao publicar no GitHub

- **Nunca** envie para o GitHub: senha real do MySQL, senhas de e-mail ou a sua senha real de administrador.
- O arquivo `application.properties` do repositório usa só valores de demonstração (XAMPP sem senha e `admin123`).
- Os dados reais ficam no `application-local.properties`, que está no `.gitignore` e só existe na sua máquina.
- A pasta `uploads/` (fotos de clientes) também **não deve** ser enviada.

---

**TechOS, versão 1.** Projeto de estudante para portfólio.
