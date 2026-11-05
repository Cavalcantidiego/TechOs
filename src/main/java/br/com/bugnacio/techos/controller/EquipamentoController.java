package br.com.bugnacio.techos.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/equipamentos")
public class EquipamentoController {

    // Endereco: http://localhost:8080/equipamentos
    @GetMapping
    public String listar(Model model) {
        // A tela equipamentos/lista espera uma lista chamada "equipamentos".
        model.addAttribute("equipamentos", List.of());

        // Abre: templates/equipamentos/lista.html
        return "equipamentos/lista";
    }
}