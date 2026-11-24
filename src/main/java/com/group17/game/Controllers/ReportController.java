package com.group17.game.Controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.ProfileTag;
import com.group17.game.Models.ProfileTagRepository;
import com.group17.game.Models.ProfileTagType;
import com.group17.game.Models.User;
import com.group17.game.Models.UserReportRepository;
import com.group17.game.Models.UserRepository;

import tools.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/report")
public class ReportController {
    private final UserRepository userRepository;
    private final UserReportRepository reportRepository;

    public ReportController(UserRepository userRepository, UserReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
    }

    @PostMapping("/user")
    public String viewTagsPage(
        Model model, 
        @RequestParam String reportedUserID, 
        @RequestParam String description, 
        @RequestParam String reason
    ) {
        

        return "/dashboard";
    }
}
