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
import org.springframework.dao.DataIntegrityViolationException;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.repository.ClienteRepository;
import jakarta.validation.Valid;

/*
 * Controller responsável pelas operações de clientes.
 * 
 * Ele recebe as requisições da tela,
 * conversa com o banco através do Repository
 * e devolve as páginas para o usuário.
 */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    // Responsável por acessar a tabela de clientes no banco.
    private final ClienteRepository repo;

    // Injeção de dependência.
    // O Spring cria o Repository e entrega pronto para uso.
    public ClienteController(ClienteRepository repo) {
        this.repo = repo;
    }

    /*
     * Lista todos os clientes.
     * Também permite pesquisar pelo nome usando o parâmetro "busca".
     * 
     * Exemplos:
     * /clientes
     * /clientes?busca=maria
     */
    @GetMapping
    public String listar(
            @RequestParam(value = "busca", required = false) String busca,
            Model model) {

        if (busca != null && !busca.isBlank()) {

            // Busca clientes cujo nome contenha o texto informado.
            model.addAttribute(
                    "clientes",
                    repo.findByNomeContainingIgnoreCase(busca));

        } else {

            // Sem filtro: lista todos ordenados por nome.
            model.addAttribute(
                    "clientes",
                    repo.findAll(Sort.by("nome")));
        }

        // Mantém o texto da pesquisa preenchido na tela.
        model.addAttribute("busca", busca);

        return "clientes/lista";
    }

    /*
     * Abre o formulário de cadastro.
     */
    @GetMapping("/novo")
    public String novo(Model model) {

        // Cria um objeto vazio para o formulário preencher.
        model.addAttribute("cliente", new Cliente());

        return "clientes/form";
    }

    /*
     * Salva um cliente novo ou atualiza um já existente.
     * 
     * @Valid executa as validações definidas na entidade Cliente.
     * BindingResult recebe os possíveis erros encontrados.
     */
    @PostMapping("/salvar")
    public String salvar(
            @Valid Cliente cliente,
            BindingResult result,
            RedirectAttributes redirect) {

        boolean jaExiste;

        if (cliente.getId() == null) {

            // Cadastro novo:
            // verifica se o telefone já existe.
            jaExiste = repo.existsByTelefone(
                    cliente.getTelefone());

        } else {

            // Edição:
            // verifica se outro cliente já utiliza o telefone.
            jaExiste = repo.existsByTelefoneAndIdNot(
                    cliente.getTelefone(),
                    cliente.getId());
        }

        if (jaExiste) {

            // Adiciona erro diretamente no campo telefone.
            result.rejectValue(
                    "telefone",
                    "duplicado",
                    "Já existe um cliente com esse telefone.");
        }

        // Caso haja qualquer erro de validação,
        // retorna ao formulário exibindo as mensagens.
        if (result.hasErrors()) {
            return "clientes/form";
        }

        /*
         * save() funciona para inserir e atualizar.
         * 
         * Se o id for null:
         * INSERT
         * Se existir id:
         * UPDATE
         */
        repo.save(cliente);

        redirect.addFlashAttribute(
                "mensagem",
                "Cliente salvo com sucesso!");

        return "redirect:/clientes";
    }

    /*
     * Exclui um cliente pelo ID.
     */
    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable Long id,
            RedirectAttributes attributes) {

        try {

            // Solicita a exclusão do cliente.
            repo.deleteById(id);

            /*
             * Flush força o Hibernate a executar a operação
             * imediatamente no banco.
             * 
             * Isso permite capturar erros de integridade
             * dentro deste bloco try/catch.
             */
            repo.flush();

            attributes.addFlashAttribute(
                    "mensagem",
                    "Cliente excluído com sucesso!");

        } catch (DataIntegrityViolationException ex) {

            /*
             * O banco impediu a exclusão porque
             * existem registros relacionados.
             *
             * Exemplo:
             * Cliente possui equipamentos cadastrados.
             */
            attributes.addFlashAttribute(
                    "erro",
                    "Este cliente possui equipamentos cadastrados e não pode ser excluído. "
                            + "Remova ou transfira os equipamentos primeiro.");
        }

        return "redirect:/clientes";
    }

    /*
     * Carrega um cliente existente para edição.
     */
    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            Model model) {

        // Busca o cliente no banco.
        // Caso não encontre, lança exceção.
        Cliente cliente = repo.findById(id).orElseThrow();

        // Envia os dados para preencher o formulário.
        model.addAttribute("cliente", cliente);

        return "clientes/form";
    }
}