package com.group17.game.Controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.ProfileTag;
import com.group17.game.Models.ProfileTagRepository;
import com.group17.game.Models.ProfileTagType;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

@Controller 
@RequestMapping("/tags")
public class ProfileTagController {
    private final UserRepository userRepository;
    private final ProfileTagRepository profileTagRepository;

    public ProfileTagController(UserRepository userRepository, ProfileTagRepository tagRepository) {
        this.userRepository = userRepository;
        this.profileTagRepository = tagRepository;
    }

    @GetMapping("")
    public String viewTagsPage(Model model, @CurrentUser User user) {
        List<ProfileTag> personalTags = profileTagRepository.findAllByUserId(user.getId());

        model.addAttribute("personalTags", personalTags);
        model.addAttribute("tagTypes", ProfileTagType.values());
        model.addAttribute("catalog", ProfileTag.ALLOWED_TAGS.toArray());
        model.addAttribute("user", user);
        return "tags";
    }

    @PostMapping("")
    public String addTagToUserProfile(@RequestParam String tag, @RequestParam String tagType, @CurrentUser User user) { 
        if (!ProfileTag.ALLOWED_TAGS.contains(tag)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        ProfileTagType type;

        try {
            type = ProfileTagType.valueOf(tagType);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
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
