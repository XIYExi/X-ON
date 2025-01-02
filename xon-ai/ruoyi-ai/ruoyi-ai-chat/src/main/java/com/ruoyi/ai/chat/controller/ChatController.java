package com.ruoyi.ai.chat.controller;

import com.ruoyi.common.annotation.Anonymous;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/ai/openai")
@AllArgsConstructor
public class ChatController {

    private final OpenAiChatModel openAiChatModel;

    @Anonymous
    @GetMapping("/v1/chat")
    public String chat(@RequestParam(value = "msg") String msg) {
        return openAiChatModel.call(msg);
    }


}
