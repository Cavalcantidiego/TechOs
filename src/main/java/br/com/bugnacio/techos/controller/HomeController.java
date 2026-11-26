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
import br.com.bugnacio.techos.repository.ItemEstoqueRepository;
import br.com.bugnacio.techos.repository.MovimentoEstoqueRepository;
import br.com.bugnacio.techos.repository.OrdemServicoRepository;
import br.com.bugnacio.techos.repository.ReservaEstoqueRepository;
import br.com.bugnacio.techos.repository.EquipamentoRepository;

/*
 * Controller da página inicial (Dashboard).
 *
 * Sua função é reunir informações de vários módulos
 * e enviá-las para a tela principal do sistema.
 */
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
            OrdemServicoRepository osRepository) {

        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.itemEstoqueRepository = itemEstoqueRepository;
        this.movimentoRepository = movimentoRepository;
        this.reservaRepository = reservaRepository;
        this.osRepository = osRepository;
    }

    /*
     * Carrega a página inicial.
     */
    @GetMapping("/")
    public String inicio(Model model) {

        // =====================================================
        // INDICADORES DE ORDENS DE SERVIÇO
        // =====================================================

        List<OrdemServico> todas = osRepository.findAll();

        /*
         * Status considerados "em andamento".
         */
        List<StatusOS> emAndamento = List.of(
                StatusOS.RECEBIDO,
                StatusOS.EM_ANALISE,
                StatusOS.AGUARDANDO_PECA,
                StatusOS.EM_REPARO);

        // Quantidade de OS abertas
        long abertas = todas.stream()
                .filter(o -> emAndamento.contains(o.getStatus()))
                .count();

        // Quantidade aguardando peça
        long aguardandoPeca = todas.stream()
                .filter(o -> o.getStatus() == StatusOS.AGUARDANDO_PECA)
                .count();

        // Quantidade concluída
        long prontas = todas.stream()
                .filter(o -> o.getStatus() == StatusOS.CONCLUIDA)
                .count();

        // =====================================================
        // FATURAMENTO
        // =====================================================

        /*
         * Considera apenas OS finalizadas
         * (concluídas ou entregues).
         */
        List<OrdemServico> finalizadas = todas.stream()
                .filter(o -> o.getStatus().isFinalizada())
                .toList();

        /*
         * Soma todo o valor das OS finalizadas.
         */
        BigDecimal acumulado = finalizadas.stream()
                .map(OrdemServico::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        /*
         * Soma apenas as OS concluídas no mês atual.
         */
        YearMonth mesAtual = YearMonth.now();

        BigDecimal doMes = finalizadas.stream()
                .filter(o ->
                        o.getDataConclusao() != null
                        && YearMonth.from(o.getDataConclusao())
                                .equals(mesAtual))
                .map(OrdemServico::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // =====================================================
        // ALERTAS DE ESTOQUE
        // =====================================================

        /*
         * Busca itens com estoque baixo ou zerado.
         */
        List<ItemEstoque> alertasEstoque =
                itemEstoqueRepository.findAll()
                        .stream()
                        .filter(i ->
                                !i.isInativo()
                                && (i.isZerado() || i.isBaixo()))
                        .sorted(
                                Comparator.comparing(
                                        ItemEstoque::getQuantidade))
                        .toList();

        /*
         * Últimas 5 movimentações do estoque.
         */
        List<MovimentoEstoque> ultimasMov =
                movimentoRepository
                        .findTop5ByOrderByDataHoraDesc();

        // =====================================================
        // DADOS ENVIADOS PARA A TELA
        // =====================================================

        // Financeiro
        model.addAttribute("faturamentoMes", doMes);
        model.addAttribute("faturamento", acumulado);

        // Ordens de serviço
        model.addAttribute("osAbertas", abertas);
        model.addAttribute("osAguardandoPeca", aguardandoPeca);
        model.addAttribute("osProntas", prontas);

        // Cadastros
        model.addAttribute(
                "totalClientes",
                clienteRepository.count());

        model.addAttribute(
                "totalEquipamentos",
                equipamentoRepository.count());

        /*
         * Últimas 5 OS cadastradas.
         */
        model.addAttribute(
                "ultimasOs",
                osRepository.findTop5ByOrderByDataEntradaDesc());

        // Estoque
        model.addAttribute(
                "totalAlertasEstoque",
                alertasEstoque.size());

        model.addAttribute(
                "alertasEstoque",
                alertasEstoque.stream().limit(5).toList());

        model.addAttribute(
                "pecasReservadas",
                reservaRepository.totalGeralReservado());

        model.addAttribute(
                "ultimasMovimentacoes",
                ultimasMov);

        // Abre dashboard.html
        return "dashboard";
    }
}