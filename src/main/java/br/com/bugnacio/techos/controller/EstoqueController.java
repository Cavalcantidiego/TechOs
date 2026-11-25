package br.com.bugnacio.techos.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.bugnacio.techos.model.Estoque;
import br.com.bugnacio.techos.model.ItemEstoque;
import br.com.bugnacio.techos.model.MovimentoEstoque;
import br.com.bugnacio.techos.repository.ItemEstoqueRepository;
import br.com.bugnacio.techos.repository.MovimentoEstoqueRepository;
import br.com.bugnacio.techos.repository.ReservaEstoqueRepository;
import br.com.bugnacio.techos.service.EstoqueService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private static final String MSG_CODIGO_DUPLICADO = "Já existe um item cadastrado com este código.";

    private final ItemEstoqueRepository repo;
    private final MovimentoEstoqueRepository movRepo;
    private final ReservaEstoqueRepository reservaRepo;
    private final EstoqueService estoqueService;

    public EstoqueController(ItemEstoqueRepository repo, MovimentoEstoqueRepository movRepo,
            ReservaEstoqueRepository reservaRepo, EstoqueService estoqueService) {
        this.repo = repo;
        this.movRepo = movRepo;
        this.reservaRepo = reservaRepo;
        this.estoqueService = estoqueService;
    }

    @ModelAttribute("categorias")
    public Estoque[] categorias() {
        return Estoque.values();
    }

    // ---------- LISTA ----------
    @GetMapping
    public String listar(@RequestParam(value = "busca", required = false) String busca, Model model) {
        List<ItemEstoque> itens;
        if (busca != null && !busca.isBlank()) {
            String b = busca.trim();
            itens = repo.findByNomeContainingIgnoreCaseOrCodigoContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(b, b, b);
        } else {
            itens = repo.findAll(Sort.by("nome"));
        }

        // Preenche o "reservado" de cada item (peças separadas por OS abertas)
        Map<Long, Long> reservas = new HashMap<>();
        for (Object[] linha : reservaRepo.totalReservadoPorItem()) {
            reservas.put((Long) linha[0], (Long) linha[1]);
        }
        itens.forEach(i -> i.setReservado(reservas.getOrDefault(i.getId(), 0L).intValue()));

        List<ItemEstoque> todos = repo.findAll();
        long baixos = todos.stream().filter(i -> i.isBaixo() || i.isZerado()).count();
        BigDecimal valorTotal = todos.stream()
                .map(ItemEstoque::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("itens", itens);
        model.addAttribute("busca", busca);
        model.addAttribute("totalItens", todos.size());
        model.addAttribute("totalBaixos", baixos);
        model.addAttribute("valorTotal", valorTotal);
        return "estoque/lista";
    }

    // ---------- CADASTRO / EDIÇÃO ----------
    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("item", new ItemEstoque());
        return "estoque/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("item") ItemEstoque item,
            BindingResult result, RedirectAttributes redirect) {

        boolean novo = item.getId() == null;

        String codigo = item.getCodigo();
        if (codigo != null && !codigo.isBlank()) {
            codigo = codigo.trim();
            item.setCodigo(codigo);
            boolean duplicado = novo
                    ? repo.existsByCodigoIgnoreCase(codigo)
                    : repo.existsByCodigoIgnoreCaseAndIdNot(codigo, item.getId());
            if (duplicado) {
                result.rejectValue("codigo", "duplicado", MSG_CODIGO_DUPLICADO);
            }
        }

        if (result.hasErrors()) {
            return "estoque/form";
        }

        try {
            if (novo) {
                // A quantidade digitada vira uma ENTRADA no histórico ("Estoque inicial")
                int inicial = item.getQuantidade() == null ? 0 : item.getQuantidade();
                item.setQuantidade(0);
                ItemEstoque salvo = repo.save(item);
                if (inicial > 0) {
                    estoqueService.entrada(salvo.getId(), inicial, "Estoque inicial");
                }
            } else {
                // Na edição a quantidade NÃO muda por aqui: só por entrada ou pela OS.
                Optional<ItemEstoque> atual = repo.findById(item.getId());
                if (atual.isEmpty()) {
                    redirect.addFlashAttribute("erro", "Item não encontrado.");
                    return "redirect:/estoque";
                }
                item.setQuantidade(atual.get().getQuantidade());
                repo.save(item);
            }
        } catch (DataIntegrityViolationException ex) {
            result.rejectValue("codigo", "duplicado", MSG_CODIGO_DUPLICADO);
            return "estoque/form";
        }

        redirect.addFlashAttribute("mensagem",
                novo ? "Item cadastrado com sucesso!" : "Item atualizado com sucesso!");
        return "redirect:/estoque";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
        Optional<ItemEstoque> item = repo.findById(id);
        if (item.isEmpty()) {
            redirect.addFlashAttribute("erro", "Item não encontrado.");
            return "redirect:/estoque";
        }
        model.addAttribute("item", item.get());
        return "estoque/form";
    }

    // ---------- ENTRADA (reposição). Não existe saída manual. ----------
    @PostMapping("/{id}/entrada")
    public String entrada(@PathVariable("id") Long id,
            @RequestParam("quantidade") Integer quantidade,
            @RequestParam(value = "motivo", required = false) String motivo,
            RedirectAttributes redirect) {
        try {
            if (quantidade == null || quantidade < 1) {
                throw new IllegalArgumentException("Informe uma quantidade maior que zero.");
            }
            String m = (motivo == null || motivo.isBlank()) ? "Reposição" : motivo.trim();
            estoqueService.entrada(id, quantidade, m);
            redirect.addFlashAttribute("mensagem", "Entrada registrada.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/estoque";
    }

    // ---------- LEITURA POR PISTOLA (só entrada) ----------
    @GetMapping("/bipar")
    public String bipar() {
        return "estoque/bipar";
    }

    @PostMapping("/bipar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> biparItem(
            @RequestParam("codigo") String codigo,
            @RequestParam(value = "quantidade", defaultValue = "1") Integer quantidade) {

        if (quantidade == null || quantidade < 1) {
            return resposta(HttpStatus.BAD_REQUEST, false, "Quantidade inválida.", null);
        }
        Optional<ItemEstoque> opt = repo.buscarPorLeitura(codigo.trim());
        if (opt.isEmpty()) {
            return resposta(HttpStatus.NOT_FOUND, false, "Código não cadastrado: " + codigo.trim(), null);
        }
        try {
            ItemEstoque item = estoqueService.entrada(opt.get().getId(), quantidade, "Recebimento (leitura)");
            return resposta(HttpStatus.OK, true, "Entrada registrada", item);
        } catch (IllegalArgumentException ex) {
            return resposta(HttpStatus.BAD_REQUEST, false, ex.getMessage(), opt.get());
        }
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, boolean ok, String mensagem, ItemEstoque item) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("ok", ok);
        corpo.put("mensagem", mensagem);
        if (item != null) {
            corpo.put("nome", item.getNome());
            corpo.put("codigo", item.getCodigo());
            corpo.put("quantidade", item.getQuantidade());
            corpo.put("baixo", item.isBaixo());
            corpo.put("zerado", item.isZerado());
        }
        return ResponseEntity.status(status).body(corpo);
    }

    // ---------- HISTÓRICO (todos, ou de um item: ?item=ID) ----------
    @GetMapping("/historico")
    public String historico(@RequestParam(value = "item", required = false) Long itemId, Model model) {
        List<MovimentoEstoque> movimentos = (itemId != null)
                ? movRepo.findByItemIdOrderByDataHoraDesc(itemId)
                : movRepo.findTop100ByOrderByDataHoraDesc();
        model.addAttribute("movimentos", movimentos);
        model.addAttribute("itemFiltro", itemId == null ? null : repo.findById(itemId).orElse(null));
        return "estoque/historico";
    }

        // ---------- EXCLUIR (apaga o item e o histórico dele) ----------
    // Se o item já foi usado em uma OS, o banco recusa e nada é apagado.
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            estoqueService.excluirDefinitivo(id);
            redirect.addFlashAttribute("mensagem", "Item excluído.");
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("erro",
                    "Este item foi usado em uma OS e não pode ser excluído. Use Inativar.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/estoque";
    }
}