package com.henashi.inventorycrm.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "聊天请求")
public record ChatRequestDTO(
        @Schema(description = "用户消息")
        String message,

        @Schema(description = "历史消息（可选）")
        List<ChatMessage> history
) {
    public record ChatMessage(
            String role,
            String content
    ) {}
}
