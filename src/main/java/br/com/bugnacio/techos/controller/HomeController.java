package br.com.bugnacio.techos.controller;

import java.math.BigDecimal;
import java.time.YearMonth;
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

        // ----- OS -----
        List<OrdemServico> todas = osRepository.findAll();
        List<StatusOS> emAndamento = List.of(
                StatusOS.RECEBIDO, StatusOS.EM_ANALISE, StatusOS.AGUARDANDO_PECA, StatusOS.EM_REPARO);

        long abertas = todas.stream().filter(o -> emAndamento.contains(o.getStatus())).count();
        long aguardandoPeca = todas.stream().filter(o -> o.getStatus() == StatusOS.AGUARDANDO_PECA).count();
        long prontas = todas.stream().filter(o -> o.getStatus() == StatusOS.CONCLUIDA).count();

        // Faturamento: soma do valor das OS Concluídas e Entregues
        List<OrdemServico> finalizadas = todas.stream().filter(o -> o.getStatus().isFinalizada()).toList();
        BigDecimal acumulado = finalizadas.stream()
                .map(OrdemServico::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        YearMonth mesAtual = YearMonth.now();
        BigDecimal doMes = finalizadas.stream()
                .filter(o -> o.getDataConclusao() != null && YearMonth.from(o.getDataConclusao()).equals(mesAtual))
                .map(OrdemServico::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ----- Estoque -----
        List<ItemEstoque> alertasEstoque = itemEstoqueRepository.findAll().stream()
                .filter(i -> i.isZerado() || i.isBaixo())
                .sorted(Comparator.comparing(ItemEstoque::getQuantidade))
                .toList();

        List<MovimentoEstoque> ultimasMov = movimentoRepository.findTop5ByOrderByDataHoraDesc();

        // ----- Envia para a tela -----
        model.addAttribute("faturamentoMes", doMes);
        model.addAttribute("faturamento", acumulado);
        model.addAttribute("osAbertas", abertas);
        model.addAttribute("osAguardandoPeca", aguardandoPeca);
        model.addAttribute("osProntas", prontas);
        model.addAttribute("totalClientes", clienteRepository.count());
        model.addAttribute("totalEquipamentos", equipamentoRepository.count());
        model.addAttribute("ultimasOs", osRepository.findTop5ByOrderByDataEntradaDesc());

        model.addAttribute("totalAlertasEstoque", alertasEstoque.size());
        model.addAttribute("alertasEstoque", alertasEstoque.stream().limit(5).toList());
        model.addAttribute("pecasReservadas", reservaRepository.totalGeralReservado());
        model.addAttribute("ultimasMovimentacoes", ultimasMov);

        return "dashboard";
    }
}