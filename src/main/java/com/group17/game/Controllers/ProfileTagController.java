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

import tools.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/tags")
public class ProfileTagController {
    private final ProfileTagRepository profileTagRepository;

    public ProfileTagController(ProfileTagRepository tagRepository) {
        this.profileTagRepository = tagRepository;
    }

    @GetMapping("")
    public String viewTagsPage(Model model, @RequestParam(name = "error", required = false) String error,
            @CurrentUser User user) {
        List<ProfileTag> personalTags = profileTagRepository.findAllByUserId(user.getId());

        model.addAttribute("personalTags", personalTags);
        model.addAttribute("catalog", ProfileTag.ALLOWED_TAGS);
        model.addAttribute("catalogJson", new ObjectMapper().writeValueAsString(ProfileTag.ALLOWED_TAGS));
        model.addAttribute("user", user);

        if (error != null) {
            model.addAttribute("error", error);
        }

        return "tags";
    }

    @PostMapping("")
    public String addTagToUserProfile(@RequestParam String tag, @RequestParam String tagType, @CurrentUser User user) {
        ProfileTagType type;

        try {
            type = ProfileTagType.valueOf(tagType);
        } catch (IllegalArgumentException ex) {
            return "redirect:/tags?error=Invalid tag type";
        }

        if (!ProfileTag.isValidTag(tag, type)) {
            return "redirect:/tags?error=Invalid tag combination";
        }

        Optional<ProfileTag> existingTag = profileTagRepository.findByUserIdAndTag(user.getId(), tag);
        if (existingTag.isPresent()) {
            // user already has this tag
            return "redirect:/tags";
        }

        ProfileTag newTag = new ProfileTag(user.getId(), tag, type);
        profileTagRepository.save(newTag);

        return "redirect:/tags";
    }

    @PostMapping("/{tagID}/delete")
    public String removeTagFromProfile(@PathVariable("tagID") Long tagID, @CurrentUser User user) {
        Optional<ProfileTag> tag = profileTagRepository.findById(tagID);
        if (tag.isEmpty()) {
            return "redirect:/tags?error=Tag doesn't exist.";
        } else if (tag.get().getUserID() != user.getId()) {
            return "redirect:/tags?error=Tag doesn't belong to you.";
        }

        profileTagRepository.deleteById(tagID);

        return "redirect:/tags";
    }
}
