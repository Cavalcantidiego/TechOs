package br.com.bugnacio.techos.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.ItemEstoque;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOS;
import br.com.bugnacio.techos.repository.EquipamentoRepository;
import br.com.bugnacio.techos.repository.ItemEstoqueRepository;
import br.com.bugnacio.techos.repository.OrdemServicoRepository;
import br.com.bugnacio.techos.repository.PecaUsadaRepository;
import br.com.bugnacio.techos.repository.ReservaEstoqueRepository;
import br.com.bugnacio.techos.service.OrdemServicoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/os")
public class OrdemServicoController {

    private final OrdemServicoRepository osRepo;
    private final PecaUsadaRepository pecaRepo;
    private final EquipamentoRepository equipamentoRepo;
    private final ItemEstoqueRepository itemRepo;
    private final ReservaEstoqueRepository reservaRepo;
    private final OrdemServicoService osService;

    public OrdemServicoController(OrdemServicoRepository osRepo, PecaUsadaRepository pecaRepo,
            EquipamentoRepository equipamentoRepo, ItemEstoqueRepository itemRepo,
            ReservaEstoqueRepository reservaRepo, OrdemServicoService osService) {
        this.osRepo = osRepo;
        this.pecaRepo = pecaRepo;
        this.equipamentoRepo = equipamentoRepo;
        this.itemRepo = itemRepo;
        this.reservaRepo = reservaRepo;
        this.osService = osService;
    }

    // ---------- LISTA (busca por nº, cliente, marca, modelo ou nº de série + filtro de status) ----------
    @GetMapping
    public String listar(@RequestParam(value = "busca", required = false) String busca,
            @RequestParam(value = "status", required = false) StatusOS status, Model model) {

        List<OrdemServico> lista = osRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));

        if (status != null) {
            lista = lista.stream().filter(o -> o.getStatus() == status).toList();
        }
        if (busca != null && !busca.isBlank()) {
            String b = busca.trim().toLowerCase().replace("#", "");
            lista = lista.stream().filter(o -> corresponde(o, b)).toList();
        }

        model.addAttribute("ordens", lista);
        model.addAttribute("busca", busca);
        model.addAttribute("statusSel", status);
        model.addAttribute("statusLista", StatusOS.values());
        return "os/lista";
    }

    private boolean corresponde(OrdemServico o, String b) {
        Equipamento e = o.getEquipamento();
        return String.valueOf(o.getId()).equals(b)
                || contem(e.getMarca(), b)
                || contem(e.getModelo(), b)
                || contem(e.getNumeroSerie(), b)
                || (e.getCliente() != null && contem(e.getCliente().getNome(), b));
    }

    private boolean contem(String texto, String b) {
        return texto != null && texto.toLowerCase().contains(b);
    }

    // ---------- NOVA ----------
    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("os", new OrdemServico());
        carregarEquipamentos(model);
        return "os/form";
    }

    // ---------- SALVAR (nova ou edição) ----------
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("os") OrdemServico form, BindingResult result,
            Model model, RedirectAttributes redirect) {

        Long equipamentoId = form.getEquipamento() == null ? null : form.getEquipamento().getId();
        Equipamento equipamento = null;
        if (equipamentoId == null) {
            result.rejectValue("equipamento", "obrigatorio", "Selecione o equipamento.");
        } else {
            equipamento = equipamentoRepo.findById(equipamentoId).orElse(null);
            if (equipamento == null) {
                result.rejectValue("equipamento", "invalido", "Equipamento não encontrado.");
            }
        }

        if (result.hasErrors()) {
            carregarEquipamentos(model);
            return "os/form";
        }

        boolean nova = form.getId() == null;
        OrdemServico os;
        if (nova) {
            os = new OrdemServico(); // status RECEBIDO e data de entrada vêm do padrão da entidade
        } else {
            Optional<OrdemServico> existente = osRepo.findById(form.getId());
            if (existente.isEmpty()) {
                redirect.addFlashAttribute("erro", "OS não encontrada.");
                return "redirect:/os";
            }
            os = existente.get();
        }

        // Copia só os campos editáveis (status, peças e baixa NÃO vêm do formulário)
        os.setEquipamento(equipamento);
        os.setDefeito(form.getDefeito().trim());
        os.setLaudo(form.getLaudo() == null || form.getLaudo().isBlank() ? null : form.getLaudo().trim());
        os.setTecnicoNome(form.getTecnicoNome() == null || form.getTecnicoNome().isBlank()
                ? null : form.getTecnicoNome().trim());
        os.setValor(form.getValor());

        OrdemServico salva = osRepo.save(os);
        redirect.addFlashAttribute("mensagem",
                nova ? "OS #" + salva.getId() + " aberta. Adicione as peças, se precisar."
                        : "OS #" + salva.getId() + " atualizada.");
        return "redirect:/os/" + salva.getId();
    }

    // ---------- EDITAR ----------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
        Optional<OrdemServico> os = osRepo.findById(id);
        if (os.isEmpty()) {
            redirect.addFlashAttribute("erro", "OS não encontrada.");
            return "redirect:/os";
        }
        model.addAttribute("os", os.get());
        carregarEquipamentos(model);
        return "os/form";
    }

    // ---------- DETALHE (dados, status e peças) ----------
    @GetMapping("/{id}")
    public String detalhe(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
        Optional<OrdemServico> os = osRepo.findById(id);
        if (os.isEmpty()) {
            redirect.addFlashAttribute("erro", "OS não encontrada.");
            return "redirect:/os";
        }

        // Peças do estoque com o "disponível" (físico menos reservado) para o campo de busca
        Map<Long, Long> reservas = new HashMap<>();
        for (Object[] linha : reservaRepo.totalReservadoPorItem()) {
            reservas.put((Long) linha[0], (Long) linha[1]);
        }
        List<ItemEstoque> itens = itemRepo.findAll(Sort.by("nome"));
        itens.forEach(i -> i.setReservado(reservas.getOrDefault(i.getId(), 0L).intValue()));

        model.addAttribute("os", os.get());
        model.addAttribute("pecas", pecaRepo.findByOsIdOrderByIdAsc(id));
        model.addAttribute("itensEstoque", itens.stream().filter(i -> i.getDisponivel() > 0).toList());
        model.addAttribute("statusLista", StatusOS.values());
        return "os/detalhe";
    }

    // ---------- MUDAR STATUS ----------
    @PostMapping("/{id}/status")
    public String mudarStatus(@PathVariable("id") Long id, @RequestParam("status") StatusOS status,
            RedirectAttributes redirect) {
        try {
            osService.mudarStatus(id, status);
            redirect.addFlashAttribute("mensagem", "Status atualizado para " + status.getDescricao() + ".");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/os/" + id;
    }

    // ---------- PEÇAS ----------
    @PostMapping("/{id}/pecas")
    public String adicionarPeca(@PathVariable("id") Long id,
            @RequestParam("codigo") String codigo,
            @RequestParam(value = "quantidade", defaultValue = "1") Integer quantidade,
            RedirectAttributes redirect) {
        try {
            osService.adicionarPeca(id, codigo, quantidade == null ? 1 : quantidade);
            redirect.addFlashAttribute("mensagem", "Peça adicionada e reservada.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/os/" + id;
    }

    @PostMapping("/{id}/pecas/{pecaId}/remover")
    public String removerPeca(@PathVariable("id") Long id, @PathVariable("pecaId") Long pecaId,
            RedirectAttributes redirect) {
        try {
            osService.removerPeca(id, pecaId);
            redirect.addFlashAttribute("mensagem", "Peça removida da OS.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/os/" + id;
    }

    // Só equipamentos que têm cliente podem receber uma OS
    private void carregarEquipamentos(Model model) {
        List<Equipamento> equipamentos = equipamentoRepo.findAll(Sort.by("marca")).stream()
                .filter(e -> e.getCliente() != null)
                .toList();
        model.addAttribute("equipamentos", equipamentos);
    }
}