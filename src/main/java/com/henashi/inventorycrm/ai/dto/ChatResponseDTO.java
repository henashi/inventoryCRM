package com.henashi.inventorycrm.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "聊天响应")
public record ChatResponseDTO(
        @Schema(description = "AI 回复内容")
        String reply,

        @Schema(description = "是否使用了降级模式（无 API Key 时）")
        boolean fallback
) {}
