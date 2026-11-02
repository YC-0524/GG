package com.group17.game.Resolvers;

import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    public CurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter p) {
        return p.hasParameterAnnotation(CurrentUser.class)
            && User.class.isAssignableFrom(p.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter p, ModelAndViewContainer mav,
                                  NativeWebRequest req, WebDataBinderFactory f) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Not authenticated");
        }

        Object principal = auth.getPrincipal();
        long id;
        if (principal instanceof Long l) {
            id = l;
        } else {
            try {
                id = Long.parseLong(auth.getName());
            } catch (NumberFormatException e) {
                throw new AccessDeniedException("Invalid principal");
            }
        }

        return userRepository.findById(id)
            .orElseThrow(() -> new AccessDeniedException("User not found"));
    }
}