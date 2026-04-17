package com.once.globalnews.chat.infrastructure.bedrock;

import com.once.globalnews.chat.domain.ChatAttachment;
import com.once.globalnews.chat.domain.ChatAttachmentKind;
import com.once.globalnews.storage.infrastructure.S3ObjectMetadataReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockContentBuilder {

    private final S3ObjectMetadataReader s3ObjectMetadataReader;

    public List<Map<String, Object>> buildUserContent(String userText, List<ChatAttachment> attachments) {
        List<Map<String, Object>> blocks = new ArrayList<>();

        if (attachments != null) {
            for (ChatAttachment a : attachments) {
                if (a.getKind() != ChatAttachmentKind.IMAGE) {
                    continue;
                }
                byte[] bytes = s3ObjectMetadataReader.getObjectBytes(a.getS3Key());
                String b64 = Base64.getEncoder().encodeToString(bytes);
                blocks.add(Map.of(
                        "type", "image",
                        "source", Map.of(
                                "type", "base64",
                                "media_type", a.getContentType(),
                                "data", b64)));
                log.debug("Attachment block appended: attachmentId={}, contentType={}, byteSize={}",
                        a.getId(), a.getContentType(), a.getByteSize());
            }
        }

        blocks.add(Map.of("type", "text", "text", userText));
        return blocks;
    }
}
