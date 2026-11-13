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

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.TipoEquipamento;
import br.com.bugnacio.techos.repository.ClienteRepository;
import br.com.bugnacio.techos.repository.EquipamentoRepository;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/equipamentos")
public class EquipamentoController {

    private static final String MSG_SERIE_DUPLICADA =
            "Já existe um equipamento cadastrado com este número de série.";

    private final EquipamentoRepository repo;
    private final ClienteRepository clienteRepo;

    public EquipamentoController(EquipamentoRepository repo, ClienteRepository clienteRepo) {
        this.repo = repo;
        this.clienteRepo = clienteRepo;
    }

    // Disponibiliza as listas do formulario (select de cliente e de tipo)
    // em todas as telas deste controller.
    @ModelAttribute("clientes")
    public List<Cliente> clientes() {
        return clienteRepo.findAll(Sort.by("nome"));
    }

    @ModelAttribute("tipos")
    public TipoEquipamento[] tipos() {
        return TipoEquipamento.values();
    }

    // READ: lista (com busca opcional por marca ou modelo).
    @GetMapping
    public String listar(
            @RequestParam(value = "busca", required = false) String busca,
            Model model) {
        if (busca != null && !busca.isBlank()) {
            model.addAttribute("equipamentos",
                    repo.findByMarcaContainingIgnoreCaseOrModeloContainingIgnoreCase(busca, busca));
        } else {
            model.addAttribute("equipamentos", repo.findAll(Sort.by("marca")));
        }
        model.addAttribute("busca", busca);
        return "equipamentos/lista";
    }

    // CREATE (1/2): formulario vazio.
    @GetMapping("/novo")
    public String novo(Model model) {
        Equipamento equipamento = new Equipamento();
        equipamento.setCliente(new Cliente()); // evita erro de null no th:field="*{cliente.id}"
        model.addAttribute("equipamento", equipamento);
        return "equipamentos/form";
    }

    // CREATE (2/2) e UPDATE
    @PostMapping("/salvar")
    public String salvar(
            @Valid Equipamento equipamento,
            BindingResult result,
            RedirectAttributes redirect) {

        boolean novoCadastro = equipamento.getId() == null;

        // Remove espaços e verifica se o número de série já existe
        String serie = equipamento.getNumeroSerie();
        if (serie != null && !serie.isBlank()) {
            serie = serie.trim();
            equipamento.setNumeroSerie(serie);

            boolean duplicado = novoCadastro
                    ? repo.existsByNumeroSerieIgnoreCase(serie)
                    : repo.existsByNumeroSerieIgnoreCaseAndIdNot(serie, equipamento.getId());

            if (duplicado) {
                result.rejectValue("numeroSerie", "duplicado", MSG_SERIE_DUPLICADA);
            }
        }

        // Se nenhum cliente foi escolhido, o formulario envia um Cliente sem id.
        // Transforma em null para nao dar erro ao salvar.
        if (equipamento.getCliente() != null && equipamento.getCliente().getId() == null) {
            equipamento.setCliente(null);
        }

        if (result.hasErrors()) {
            return voltarAoFormulario(equipamento);
        }

        try {
            repo.save(equipamento);
        } catch (DataIntegrityViolationException ex) {
            // Proteção extra: o banco recusou o duplicado (só ocorre se unique = true na entidade)
            result.rejectValue("numeroSerie", "duplicado", MSG_SERIE_DUPLICADA);
            return voltarAoFormulario(equipamento);
        }

        redirect.addFlashAttribute("mensagem",
                novoCadastro ? "Equipamento cadastrado com sucesso!" : "Equipamento atualizado com sucesso!");
        return "redirect:/equipamentos";
    }

    // Garante que o cliente não seja null ao reabrir o formulário com erro.
    private String voltarAoFormulario(Equipamento equipamento) {
        if (equipamento.getCliente() == null) {
            equipamento.setCliente(new Cliente());
        }
        return "equipamentos/form";
    }

    // UPDATE (1/2): formulario preenchido.
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable("id") Long id,
            Model model, RedirectAttributes redirect) {
        Optional<Equipamento> equipamento = repo.findById(id);
        if (equipamento.isEmpty()) {
            redirect.addFlashAttribute("erro", "Equipamento não encontrado.");
            return "redirect:/equipamentos";
        }
        Equipamento e = equipamento.get();
        if (e.getCliente() == null) {
            e.setCliente(new Cliente());
        }
        model.addAttribute("equipamento", e);
        return "equipamentos/form";
    }

    // DELETE.
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable("id") Long id, RedirectAttributes redirect) {
        repo.deleteById(id);
        redirect.addFlashAttribute("mensagem", "Equipamento excluído.");
        return "redirect:/equipamentos";
    }
}