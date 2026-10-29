package br.com.bugnacio.techos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ClienteController {
    @GetMapping("/clientes")
    @ResponseBody
    public String listarClientes() {
        return "Clientes no ar";
    }
}
