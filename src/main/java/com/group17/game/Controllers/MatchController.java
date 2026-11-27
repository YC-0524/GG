package com.group17.game.Controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import com.group17.game.DTOs.MatchViews.ChatMessageView;
import com.group17.game.DTOs.MatchViews.ChatView;
import com.group17.game.DTOs.MatchViews.MatchSummary;
import com.group17.game.Models.ChatMessage;
import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.User;
import com.group17.game.Utilities.MatchService;

// Matches list (US 47), chat (US 48) and unmatching (US 50)
@Controller
@RequestMapping("/matches")
public class MatchController {
    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("")
    public String matches(Model model, @CurrentUser User user) {
        // build the list before marking new matches as seen, so they still show a "New" label
        List<MatchSummary> rows = matchService.activeMatches(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("matches", rows);
        model.addAttribute("newMatches", matchService.takeNewMatches(user.getId()));
        return "matches";
    }

    @GetMapping("/{matchId}")
    public String chat(@PathVariable long matchId, Model model, @CurrentUser User user) {
        ChatView chat;
        try {
            chat = matchService.openChat(matchId, user.getId());
        } catch (ResponseStatusException e) {
            return matchesError(MatchService.CHAT_UNAVAILABLE);
        }
        model.addAttribute("user", user);
        model.addAttribute("chat", chat);
        model.addAttribute("maxLength", ChatMessage.MAX_LENGTH);
        return "chat";
    }

    @PostMapping("/{matchId}/messages")
    public String send(@PathVariable long matchId, @RequestParam(name = "message", required = false) String message,
            @CurrentUser User user) {
        try {
            matchService.sendMessage(matchId, user.getId(), message);
        } catch (ResponseStatusException e) {
            return matchesError(MatchService.CHAT_UNAVAILABLE);
        } catch (IllegalArgumentException e) {
            return "redirect:/matches/" + matchId + "?error="
                    + UriUtils.encodeQueryParam(e.getMessage(), StandardCharsets.UTF_8);
        }
        return "redirect:/matches/" + matchId;
    }

    // The chat page asks for new messages every few seconds.
    // 404 if it isn't this player's match, 410 once the chat is closed.
    @GetMapping("/{matchId}/messages")
    @ResponseBody
    public List<ChatMessageView> newMessages(@PathVariable long matchId,
            @RequestParam(name = "after", defaultValue = "0") long after, @CurrentUser User user) {
        return matchService.newMessages(matchId, user.getId(), after);
    }

    @PostMapping("/{matchId}/unmatch")
    public String unmatch(@PathVariable long matchId, @CurrentUser User user) {
        String otherUsername;
        try {
            otherUsername = matchService.unmatch(matchId, user.getId());
        } catch (ResponseStatusException e) {
            return matchesError(MatchService.CHAT_UNAVAILABLE);
        }
        return "redirect:/matches?unmatched=" + UriUtils.encodeQueryParam(otherUsername, StandardCharsets.UTF_8);
    }

    private String matchesError(String message) {
        return "redirect:/matches?error=" + UriUtils.encodeQueryParam(message, StandardCharsets.UTF_8);
    }
}
