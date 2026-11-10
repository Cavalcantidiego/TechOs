package br.com.bugnacio.techos.controller;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.repository.ClienteRepository;
import jakarta.validation.Valid;

// @Controller: bean do Spring que recebe os pedidos (requisições) das telas.
@Controller
// @RequestMapping na classe: início do endereço de TODOS os métodos daqui.
// Ex.: http://localhost:8080/clientes
@RequestMapping("/clientes")
public class ClienteController {

    // O controller precisa do repository para acessar o banco de dados.
    private final ClienteRepository repo;

    // Injeção de dependência: o Spring cria o ClienteRepository
    // e entrega pronto para o controller pelo construtor.
    public ClienteController(ClienteRepository repo) {
        this.repo = repo;
    }

    // GET /clientes: mostra a lista de clientes.
    // O parâmetro "busca" é opcional (ex.: /clientes?busca=maria).
    @GetMapping
    public String listar(
            @RequestParam(value = "busca", required = false) String busca,
            Model model) {

        if (busca != null && !busca.isBlank()) {
            // Se digitou algo na busca, filtra pelo nome
            // (contém o texto, sem diferenciar maiúscula de minúscula).
            model.addAttribute("clientes", repo.findByNomeContainingIgnoreCase(busca));
        } else {
            // Sem busca: traz todos os clientes ordenados por nome.
            model.addAttribute("clientes", repo.findAll(Sort.by("nome")));
        }

        // Devolve o texto da busca para a tela (para manter no campo de pesquisa).
        model.addAttribute("busca", busca);

        // Abre: src/main/resources/templates/clientes/lista.html
        return "clientes/lista";
    }

    // GET /clientes/novo: mostra o formulário vazio para cadastrar um cliente.
    @GetMapping("/novo")
    public String novo(Model model) {

        // Cliente vazio para o formulário preencher.
        model.addAttribute("cliente", new Cliente());

        // Abre: src/main/resources/templates/clientes/form.html
        return "clientes/form";
    }

    // POST /clientes/salvar: recebe o formulário e salva no banco.
    // @Valid: confere as regras do Cliente (ex.: nome obrigatório).
    // BindingResult: guarda os erros de validação (precisa vir logo após o @Valid).
    // RedirectAttributes: permite mandar mensagem para a próxima tela.
    @PostMapping("/salvar")
    public String salvar(@Valid Cliente cliente,
            BindingResult result,
            RedirectAttributes redirect) {

        boolean jaExiste;

        if (cliente.getId() == null) {
            // Cliente novo: verifica se já existe alguém com esse telefone.
            jaExiste = repo.existsByTelefone(cliente.getTelefone());
        } else {
            // Edição: verifica se OUTRO cliente (id diferente) tem esse telefone.
            jaExiste = repo.existsByTelefoneAndIdNot(
                    cliente.getTelefone(),
                    cliente.getId());
        }

        if (jaExiste) {
            // Adiciona um erro no campo "telefone" para aparecer no formulário.
            result.rejectValue(
                    "telefone",
                    "duplicado",
                    "Já existe um cliente com esse telefone.");
        }

        if (result.hasErrors()) {
            // Se houver erro, volta para o formulário mostrando as mensagens.
            return "clientes/form";
        }

        // Sem erros: grava no banco (insere se novo, atualiza se já existe).
        repo.save(cliente);

        // Mensagem exibida uma única vez na próxima tela.
        redirect.addFlashAttribute(
                "mensagem",
                "Cliente salvo com sucesso!");

        // Redireciona para a lista de clientes.
        return "redirect:/clientes";
    }

    // POST /clientes/{id}/excluir: apaga o cliente pelo id.
    // @PathVariable: pega o {id} que vem na URL.
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id,
            RedirectAttributes attributes) {

        // Remove o cliente do banco.
        repo.deleteById(id);

        attributes.addFlashAttribute(
                "mensagem",
                "Cliente excluído com sucesso!");

        return "redirect:/clientes";
    }

    // GET /clientes/{id}/editar: abre o formulário preenchido com os dados do
    // cliente.
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {

        // Busca o cliente no banco; se não achar, lança erro.
        Cliente cliente = repo.findById(id).orElseThrow();

        // Envia o cliente para a tela preencher os campos.
        model.addAttribute("cliente", cliente);

        return "clientes/form";
    }
}