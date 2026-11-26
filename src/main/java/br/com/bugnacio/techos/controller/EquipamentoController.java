package br.com.bugnacio.techos.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import br.com.bugnacio.techos.repository.EquipamentoRepository;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.TipoEquipamento;
import br.com.bugnacio.techos.repository.ClienteRepository;
import jakarta.validation.Valid;

/*
 * Controller responsável pelo cadastro e gerenciamento
 * dos equipamentos dos clientes.
 * 
 * Aqui ficam as operações de:
 * - Listar
 * - Cadastrar
 * - Editar
 * - Excluir equipamentos
 */
@Controller
@RequestMapping("/equipamentos")
public class EquipamentoController {

    // Mensagem reutilizada quando existir número de série duplicado.
    private static final String MSG_SERIE_DUPLICADA = "Já existe um equipamento cadastrado com este número de série.";

    private final EquipamentoRepository repo;
    private final ClienteRepository clienteRepo;

    public EquipamentoController(
            EquipamentoRepository repo,
            ClienteRepository clienteRepo) {

        this.repo = repo;
        this.clienteRepo = clienteRepo;
    }

    /*
     * Disponibiliza automaticamente a lista de clientes
     * para qualquer tela deste controller.
     *
     * Muito usado para preencher o <select> de clientes.
     */
    @ModelAttribute("clientes")
    public List<Cliente> clientes() {
        return clienteRepo.findAll(Sort.by("nome"));
    }

    /*
     * Disponibiliza todos os tipos de equipamentos
     * definidos no enum TipoEquipamento.
     */
    @ModelAttribute("tipos")
    public TipoEquipamento[] tipos() {
        return TipoEquipamento.values();
    }

    /*
     * Lista todos os equipamentos.
     *
     * Também permite pesquisar por marca ou modelo.
     */
    @GetMapping
    public String listar(
            @RequestParam(value = "busca", required = false) String busca,
            Model model) {

        if (busca != null && !busca.isBlank()) {

            // Pesquisa por marca OU modelo.
            model.addAttribute(
                    "equipamentos",
                    repo.findByMarcaContainingIgnoreCaseOrModeloContainingIgnoreCase(
                            busca, busca));

        } else {

            // Sem filtro: lista todos ordenados pela marca.
            model.addAttribute(
                    "equipamentos",
                    repo.findAll(Sort.by("marca")));
        }

        model.addAttribute("busca", busca);

        return "equipamentos/lista";
    }

    /*
     * Abre formulário para cadastro de equipamento.
     */
    @GetMapping("/novo")
    public String novo(Model model) {

        Equipamento equipamento = new Equipamento();

        /*
         * Evita erro no Thymeleaf ao acessar cliente.id.
         *
         * Sem isso:
         * equipamento.getCliente() seria null.
         */
        equipamento.setCliente(new Cliente());

        model.addAttribute("equipamento", equipamento);

        return "equipamentos/form";
    }

    /*
     * Salva um equipamento novo ou atualiza um existente.
     */
    @PostMapping("/salvar")
    public String salvar(
            @Valid Equipamento equipamento,
            BindingResult result,
            RedirectAttributes redirect) {

        boolean novoCadastro = equipamento.getId() == null;

        /*
         * Verifica duplicidade do número de série.
         *
         * Como o número de série deve ser único,
         * removemos espaços extras antes da consulta.
         */
        String serie = equipamento.getNumeroSerie();

        if (serie != null && !serie.isBlank()) {

            serie = serie.trim();
            equipamento.setNumeroSerie(serie);

            boolean duplicado = novoCadastro
                    ? repo.existsByNumeroSerieIgnoreCase(serie)
                    : repo.existsByNumeroSerieIgnoreCaseAndIdNot(
                            serie,
                            equipamento.getId());

            if (duplicado) {
                result.rejectValue(
                        "numeroSerie",
                        "duplicado",
                        MSG_SERIE_DUPLICADA);
            }
        }

        /*
         * Quando o usuário não seleciona cliente,
         * o formulário ainda envia um objeto Cliente vazio.
         *
         * Transformamos em null para evitar erros no banco.
         */
        if (equipamento.getCliente() != null
                && equipamento.getCliente().getId() == null) {

            equipamento.setCliente(null);
        }

        // Caso exista qualquer erro de validação.
        if (result.hasErrors()) {
            return voltarAoFormulario(equipamento);
        }

        try {

            repo.save(equipamento);

        } catch (DataIntegrityViolationException ex) {

            /*
             * Segurança extra.
             *
             * Mesmo que a validação do sistema falhe,
             * o banco pode impedir números de série duplicados.
             */
            result.rejectValue(
                    "numeroSerie",
                    "duplicado",
                    MSG_SERIE_DUPLICADA);

            return voltarAoFormulario(equipamento);
        }

        redirect.addFlashAttribute(
                "mensagem",
                novoCadastro
                        ? "Equipamento cadastrado com sucesso!"
                        : "Equipamento atualizado com sucesso!");

        return "redirect:/equipamentos";
    }

    /*
     * Garante que o campo cliente nunca seja null
     * ao reabrir o formulário após um erro.
     */
    private String voltarAoFormulario(Equipamento equipamento) {

        if (equipamento.getCliente() == null) {
            equipamento.setCliente(new Cliente());
        }

        return "equipamentos/form";
    }

    /*
     * Abre o formulário já preenchido para edição.
     */
    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirect) {

        Optional<Equipamento> equipamento = repo.findById(id);

        if (equipamento.isEmpty()) {

            redirect.addFlashAttribute(
                    "erro",
                    "Equipamento não encontrado.");

            return "redirect:/equipamentos";
        }

        Equipamento e = equipamento.get();

        /*
         * Proteção contra NullPointerException
         * na tela de edição.
         */
        if (e.getCliente() == null) {
            e.setCliente(new Cliente());
        }

        model.addAttribute("equipamento", e);

        return "equipamentos/form";
    }

    /*
     * Exclui um equipamento.
     *
     * O banco impede a exclusão caso ele esteja
     * vinculado a Ordens de Serviço.
     */
    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable("id") Long id,
            RedirectAttributes redirect) {

        try {

            repo.deleteById(id);

            /*
             * Força a execução imediata no banco
             * para capturar possíveis erros aqui.
             */
            repo.flush();

            redirect.addFlashAttribute(
                    "mensagem",
                    "Equipamento excluído.");

        } catch (DataIntegrityViolationException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    "Este equipamento possui ordens de serviço e não pode ser excluído.");
        }

        return "redirect:/equipamentos";
    }
}