package io.klerch.alexa.translator.skill.handler;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.tellask.model.AlexaInput;
import io.klerch.alexa.tellask.model.AlexaOutput;
import io.klerch.alexa.tellask.schema.annotation.AlexaIntentListener;
import io.klerch.alexa.tellask.schema.type.AlexaOutputFormat;
import io.klerch.alexa.tellask.util.AlexaRequestHandlerException;
import io.klerch.alexa.translator.skill.SkillConfig;
import io.klerch.alexa.translator.skill.util.GoogleTranslation;
import io.klerch.alexa.translator.skill.util.TTSPolly;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

@AlexaIntentListener(customIntents = "Translate")
public class TranslateHandler extends AbstractIntentHandler {
    @Override
    public AlexaOutput handleRequest(final AlexaInput input) throws AlexaRequestHandlerException, AlexaStateException {
        final String lang = input.getSlotValue("language");
        final String term = input.getSlotValue("term");

        final String translated = new GoogleTranslation(input.getLocale()).translate(term, lang);
        final InputStream tts = new TTSPolly(input.getLocale()).textToSpeech(translated, lang);

        final AmazonS3Client s3Client = new AmazonS3Client();

        final String filePath = input.getLocale() + "_" + term + "_" + translated + ".mp3";
        try {
            final PutObjectRequest s3Put = new PutObjectRequest(SkillConfig.getS3BucketName(), filePath, IOUtils.toString(tts)).withCannedAcl(CannedAccessControlList.PublicRead);
            s3Client.putObject(s3Put);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return AlexaOutput.tell("Translate")
                .putSlot("mp3", SkillConfig.getS3BucketUrl() + filePath, AlexaOutputFormat.AUDIO)
                .putSlot("language", lang)
                .putSlot("term", term)
                .build();
    }
}
