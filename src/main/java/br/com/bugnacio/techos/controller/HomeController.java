package br.com.bugnacio.techos.controller;

import java.util.Comparator;
import java.util.List;

// Importa a anotação que transforma a classe em um Controller do Spring
import org.springframework.stereotype.Controller;

// Importa o Model, usado para enviar dados para o HTML
import org.springframework.ui.Model;

// Importa a anotação para criar rotas GET
import org.springframework.web.bind.annotation.GetMapping;

import br.com.bugnacio.techos.model.ItemEstoque;
import br.com.bugnacio.techos.model.MovimentoEstoque;
import br.com.bugnacio.techos.repository.ClienteRepository;
import br.com.bugnacio.techos.repository.EquipamentoRepository;
import br.com.bugnacio.techos.repository.ItemEstoqueRepository;
import br.com.bugnacio.techos.repository.MovimentoEstoqueRepository;
import br.com.bugnacio.techos.repository.ReservaEstoqueRepository;

// Indica que esta classe é um Controller
@Controller
public class HomeController {

    // Variáveis que permitem acessar os dados do banco
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ItemEstoqueRepository itemEstoqueRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final ReservaEstoqueRepository reservaRepository;

    // Construtor da classe
    // O Spring injeta automaticamente os repositórios aqui
    public HomeController(
        ClienteRepository clienteRepository,
        EquipamentoRepository equipamentoRepository,
        ItemEstoqueRepository itemEstoqueRepository,
        MovimentoEstoqueRepository movimentoRepository,
        ReservaEstoqueRepository reservaRepository
    ) {
        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.itemEstoqueRepository = itemEstoqueRepository;
        this.movimentoRepository = movimentoRepository;
        this.reservaRepository = reservaRepository;
    }

    // Quando alguém acessar a rota "/"
    @GetMapping("/")
    public String inicio(Model model) {

        // Conta os clientes cadastrados
        long totalClientes = clienteRepository.count();

        // Conta os equipamentos cadastrados
        long totalEquipamentos = equipamentoRepository.count();

        // Itens do estoque zerados ou abaixo do mínimo (os mais críticos primeiro)
        List<ItemEstoque> alertasEstoque = itemEstoqueRepository.findAll().stream()
                .filter(i -> i.isZerado() || i.isBaixo())
                .sorted(Comparator.comparing(ItemEstoque::getQuantidade))
                .toList();

        // Envia o total de clientes para a página dashboard
        model.addAttribute("totalClientes", totalClientes);

        // Envia o total de equipamentos para a página dashboard
        model.addAttribute("totalEquipamentos", totalEquipamentos);

        // Envia a quantidade de itens do estoque em alerta (card do painel)
        model.addAttribute("totalAlertasEstoque", alertasEstoque.size());

        // Envia só os 5 mais críticos para a lista "Precisam de reposição"
        model.addAttribute("alertasEstoque", alertasEstoque.stream().limit(5).toList());

        // NOVO: peças separadas por OS abertas (será maior que 0 quando a OS existir)
        model.addAttribute("pecasReservadas", reservaRepository.totalGeralReservado());

        // NOVO: últimas 5 movimentações do estoque (lista do painel)
        List<MovimentoEstoque> ultimasMov = movimentoRepository.findTop5ByOrderByDataHoraDesc();
        model.addAttribute("ultimasMovimentacoes", ultimasMov);

        // Envia a quantidade de ordens de serviço abertas
        model.addAttribute("osAbertas", 3);

        // Envia a quantidade de ordens de serviço concluídas
        model.addAttribute("osConcluidas", 25);

        // Envia o valor do faturamento
        model.addAttribute("faturamento", 4850);

        // Retorna a página dashboard.html
        return "dashboard";
    }
}