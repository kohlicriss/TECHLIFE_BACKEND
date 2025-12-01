package com.example.OfferLetter.Service;

import com.example.OfferLetter.Dto.OfferDTO;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class OfferLetterService {

    private final TemplateEngine templateEngine;

    public OfferLetterService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String generateOfferLetterHtml(OfferDTO offerDTO) throws Exception {
        // Create Thymeleaf context and set variables
        Context context = new Context();
        context.setVariable("offer", offerDTO);
        context.setVariable("currentDate", java.time.LocalDate.now());

        // Determine template based on job type
        String templateName;
        if ("Intern".equalsIgnoreCase(offerDTO.getJobType())) {
            templateName = "offer-letter-template";
        } else {
            templateName = "offer-letter-fulltime-template";
        }


        return templateEngine.process(templateName, context);
    }
}