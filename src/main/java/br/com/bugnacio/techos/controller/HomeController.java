package br.com.bugnacio.techos.controller;

// Importa a anotação que transforma a classe em um Controller do Spring
import org.springframework.stereotype.Controller;
// Importa o Model, usado para enviar dados para o HTML
import org.springframework.ui.Model;
// Importa a anotação para criar rotas GET
import org.springframework.web.bind.annotation.GetMapping;

// Importa o repositório de clientes
import br.com.bugnacio.techos.repository.ClienteRepository;

// Indica que esta classe é um Controller
@Controller
public class HomeController {

    // Variável que permitirá acessar os clientes do banco de dados
    private final ClienteRepository clienteRepository;

    // Construtor da classe
    // O Spring injeta automaticamente o ClienteRepository aqui
    public HomeController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // Quando alguém acessar a rota "/"
    @GetMapping("/")
    public String inicio(Model model) {

        // Conta quantos clientes existem cadastrados no banco
        long totalClientes = clienteRepository.count();

        // Envia o total de clientes para a página dashboard
        model.addAttribute("totalClientes", totalClientes);

        // Envia o total de equipamentos para a página dashboard
        model.addAttribute("totalEquipamentos", 10);

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