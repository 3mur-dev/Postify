package com.omar.postify.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPostException.class)
    public String handleInvalidPost(InvalidPostException ex,
                                    RedirectAttributes redirectAttributes,
                                    Principal principal) {

        redirectAttributes.addFlashAttribute("error", ex.getMessage());

        if (principal != null) {
            return "redirect:/profile/" + principal.getName();
        }

        return "redirect:/";
    }
}
