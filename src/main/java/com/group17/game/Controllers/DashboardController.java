package com.group17.game.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@Controller 
@RequestMapping("/dashboard")
public class DashboardController {
    @GetMapping("")
    public String showDashboardPage() {
        return "dashboard";
    }

    @GetMapping("/moderator")
    public String showModeratorTools() {
        return "moderator";
    }

    @GetMapping("/admin")
    public String showAdminTools() {
        return "admin";
    }
}

