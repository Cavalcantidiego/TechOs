package br.com.bugnacio.techos.controller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.com.bugnacio.techos.model.ItemEstoque;
import br.com.bugnacio.techos.model.MovimentoEstoque;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOS;
import br.com.bugnacio.techos.repository.ClienteRepository;
import br.com.bugnacio.techos.repository.EquipamentoRepository;
import br.com.bugnacio.techos.repository.ItemEstoqueRepository;
import br.com.bugnacio.techos.repository.MovimentoEstoqueRepository;
import br.com.bugnacio.techos.repository.OrdemServicoRepository;
import br.com.bugnacio.techos.repository.ReservaEstoqueRepository;

@Controller
public class HomeController {

    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ItemEstoqueRepository itemEstoqueRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final ReservaEstoqueRepository reservaRepository;
    private final OrdemServicoRepository osRepository;

    public HomeController(
        ClienteRepository clienteRepository,
        EquipamentoRepository equipamentoRepository,
        ItemEstoqueRepository itemEstoqueRepository,
        MovimentoEstoqueRepository movimentoRepository,
        ReservaEstoqueRepository reservaRepository,
        OrdemServicoRepository osRepository
    ) {
        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.itemEstoqueRepository = itemEstoqueRepository;
        this.movimentoRepository = movimentoRepository;
        this.reservaRepository = reservaRepository;
        this.osRepository = osRepository;
    }

    @GetMapping("/")
    public String inicio(Model model) {

        long totalClientes = clienteRepository.count();
        long totalEquipamentos = equipamentoRepository.count();

        // Estoque: itens zerados ou abaixo do mínimo (os mais críticos primeiro)
        List<ItemEstoque> alertasEstoque = itemEstoqueRepository.findAll().stream()
                .filter(i -> i.isZerado() || i.isBaixo())
                .sorted(Comparator.comparing(ItemEstoque::getQuantidade))
                .toList();

        // OS: abertas = ainda em andamento; concluídas = Concluída ou Entregue
        List<OrdemServico> abertas = osRepository.findByStatusIn(List.of(
                StatusOS.RECEBIDO, StatusOS.EM_ANALISE, StatusOS.AGUARDANDO_PECA, StatusOS.EM_REPARO));
        List<OrdemServico> concluidas = osRepository.findByStatusIn(List.of(
                StatusOS.CONCLUIDA, StatusOS.ENTREGUE));

        // Faturamento = soma do valor das OS concluídas e entregues
        BigDecimal faturamento = concluidas.stream()
                .map(OrdemServico::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalEquipamentos", totalEquipamentos);
        model.addAttribute("osAbertas", abertas.size());
        model.addAttribute("osConcluidas", concluidas.size());
        model.addAttribute("faturamento", faturamento);
        model.addAttribute("ultimasOs", osRepository.findTop5ByOrderByDataEntradaDesc());

        model.addAttribute("totalAlertasEstoque", alertasEstoque.size());
        model.addAttribute("alertasEstoque", alertasEstoque.stream().limit(5).toList());
        model.addAttribute("pecasReservadas", reservaRepository.totalGeralReservado());

        List<MovimentoEstoque> ultimasMov = movimentoRepository.findTop5ByOrderByDataHoraDesc();
        model.addAttribute("ultimasMovimentacoes", ultimasMov);

        return "dashboard";
    }
}