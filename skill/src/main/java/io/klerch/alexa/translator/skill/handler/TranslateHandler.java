package io.klerch.alexa.translator.skill.handler;

import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.SimpleCard;
import com.amazonaws.util.StringUtils;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.tellask.model.AlexaInput;
import io.klerch.alexa.tellask.model.AlexaOutput;
import io.klerch.alexa.tellask.schema.annotation.AlexaIntentListener;
import io.klerch.alexa.tellask.util.AlexaRequestHandlerException;
import io.klerch.alexa.translator.skill.model.TextToSpeech;
import io.klerch.alexa.translator.skill.util.GoogleTranslation;
import io.klerch.alexa.translator.skill.util.TTSPolly;
import org.apache.log4j.Logger;

import java.util.Optional;

@AlexaIntentListener(customIntents = "Translate")
public class TranslateHandler extends AbstractIntentHandler {
    private static final Logger log = Logger.getLogger(TranslateHandler.class);

    @Override
    public AlexaOutput handleRequest(final AlexaInput input) throws AlexaRequestHandlerException, AlexaStateException {
        final String lang = StringUtils.lowerCase(input.getSlotValue("language"));
        final String term = input.getSlotValue("term");

        log.info("Translating '" + term + "' into '" + lang + "'");

        final TTSPolly ttsPolly = new TTSPolly(input.getLocale(), lang);

        // translate term
        final Optional<String> translated = new GoogleTranslation(input.getLocale()).translate(term, lang);

        if (translated.isPresent()) {
            // translated term to speech
            final Optional<TextToSpeech> tts = ttsPolly.textToSpeech(term, translated.get());

            if (tts.isPresent()) {
                final Card card = new SimpleCard();
                card.setTitle(term + " -> " + translated.get());

                return AlexaOutput.tell("SayTranslate")
                        .withCard(card)
                        .putState(tts.get().withLanguage(lang))
                        .build();
            } else {
                log.warn("Did not get result of text-to-speech.");
            }
        } else {
            log.warn("Did not get result of translation.");
        }
        return AlexaOutput.tell("SayNoTranslation")
                .putSlot("text", term)
                .putSlot("language", lang)
                .build();
    }
}
