package io.github.sihyuuun.youthmoa.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class HomeController {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentPage", "home");
        return "index";
    }

    @PostMapping("/api/ping")
    public String ping(Model model) {
        model.addAttribute("timestamp", LocalDateTime.now().format(FORMATTER));
        return "fragments/ping :: result";
    }
}
