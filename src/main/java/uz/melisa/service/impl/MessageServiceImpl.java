package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.melisa.domain.Message;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.chat.ChatUserMessageDTO;
import uz.melisa.dto.claude.AIResult;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.ChatStreamPlan;
import uz.melisa.dto.message.MessageResponseDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;
import uz.melisa.dto.message.ProductBasedMessage;
import uz.melisa.enums.MessageContentType;
import uz.melisa.exp.ItemNotFoundException;
import uz.melisa.repository.MessageRepository;
import uz.melisa.service.AiChatService;
import uz.melisa.service.GlobalMessageHandler;
import uz.melisa.service.MessageService;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static uz.melisa.util.SecurityUtil.getCurrentUserId;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageHelperService messageHelperService;
    private final MessageRepository messageRepository;
    private final GlobalMessageHandler globalMessageHandler;
    private final ChatPostProcessService chatPostProcessService;
    private final AiChatService aiChatService;

    @Override
    public CommonResponse<MessageResponseDTO> sendMessage(MessageSendRequestDTO req) {
        Long userId = getCurrentUserId();
        ChatUserMessageDTO chatUserMessage = messageHelperService.saveChatAndUserMessage(req, userId);
        Map<Boolean, ProductBasedMessage> modelResponse = globalMessageHandler.handleChatMessage(chatUserMessage, userId);
        Map.Entry<Boolean, ProductBasedMessage> entry = modelResponse.entrySet().iterator().next();
        Boolean hasSuggestions = entry.getKey();
        ProductBasedMessage productBasedMessage = entry.getValue();
        String modelText = productBasedMessage.getMessage();
        Message modelMessage = messageHelperService.saveModelMessage(chatUserMessage.getChatId(), userId, modelText, hasSuggestions);
        chatPostProcessService.processRecommendedMessage(modelMessage.getId(),
                productBasedMessage.getProductIds());
        return CommonResponse.success(
                new MessageResponseDTO(modelMessage.getText(), chatUserMessage.getChatId(),
                        chatUserMessage.getMessageId(), modelMessage.getId(), getContentType(hasSuggestions))
        );
    }

    @Override
    public Flux<ServerSentEvent<Object>> sendMessageStream(MessageSendRequestDTO request) {
        return Mono.fromCallable(() -> prepareStreamContext(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(this::streamAnswer);
    }

    private ChatStreamContext prepareStreamContext(MessageSendRequestDTO request) {
        Long userId = getCurrentUserId();
        ChatUserMessageDTO userMessage = messageHelperService.saveChatAndUserMessage(request, userId);
        ChatStreamPlan plan = globalMessageHandler.prepareChatStream(userMessage, userId);

        return new ChatStreamContext(userId, userMessage, plan);
    }

    private Flux<ServerSentEvent<Object>> streamAnswer(ChatStreamContext context) {
        StringBuilder answer = new StringBuilder();
        AtomicReference<ChatResponse> firstChunk = new AtomicReference<>();
        AtomicReference<ChatResponse> lastChunk = new AtomicReference<>();

        Flux<ServerSentEvent<Object>> deltaEvents = aiChatService
                .chatStream(
                        context.plan().route(),
                        context.plan().conversationKey(),
                        context.plan().modelInput()
                )
                .doOnNext(chunk -> {
                    firstChunk.compareAndSet(null, chunk);
                    lastChunk.set(chunk);
                })
                .map(aiChatService::extractContent)
                .filter(text -> !text.isBlank())
                .doOnNext(answer::append)
                .map(this::toDeltaEvent);

        Mono<ServerSentEvent<Object>> doneEvent = Mono
                .fromCallable(() -> completeStreamedMessage(
                        context.userMessage(),
                        context.userId(),
                        context.plan(),
                        answer.toString(),
                        firstChunk.get(),
                        lastChunk.get()
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::toDoneEvent);

        return deltaEvents
                .concatWith(doneEvent)
                .onErrorResume(error -> handleStreamError(context, answer, error));
    }

    private ServerSentEvent<Object> toDeltaEvent(String text) {
        return ServerSentEvent.builder((Object) text)
                .event("delta")
                .build();
    }

    private ServerSentEvent<Object> toDoneEvent(MessageResponseDTO response) {
        return ServerSentEvent.builder((Object) response)
                .event("done")
                .build();
    }

    private Mono<ServerSentEvent<Object>> handleStreamError(ChatStreamContext context,
                                                            StringBuilder answer,
                                                            Throwable error) {
        log.error(
                "Streaming chat failed, chatId={}, messageId={}",
                context.userMessage().getChatId(),
                context.userMessage().getMessageId(),
                error
        );

        return Mono.just(ServerSentEvent.builder((Object) new ResponseMessageDTO("Something went wrong"))
                .event("error")
                .build());
    }

    private record ChatStreamContext(
            Long userId,
            ChatUserMessageDTO userMessage,
            ChatStreamPlan plan
    ) {
    }

    private MessageResponseDTO completeStreamedMessage(ChatUserMessageDTO chatUserMessage,
                                                       Long userId,
                                                       ChatStreamPlan plan,
                                                       String answer,
                                                       ChatResponse firstChunk,
                                                       ChatResponse lastChunk) {
        Message modelMessage = messageHelperService.saveModelMessage(
                chatUserMessage.getChatId(), userId, answer, plan.productBased());
        chatPostProcessService.processRecommendedMessage(modelMessage.getId(), plan.productIds());

        AIResult result = aiChatService.toAiResult(firstChunk, lastChunk, answer);
        chatPostProcessService.processClaude(
                chatUserMessage.getChatId(), chatUserMessage.getMessageId(), plan.conversationKey(),
                chatUserMessage.getMessage(), answer, plan.productBased(),
                plan.modelInput(), null, null, result
        );

        return new MessageResponseDTO(
                answer, chatUserMessage.getChatId(), chatUserMessage.getMessageId(),
                modelMessage.getId(), getContentType(plan.productBased())
        );
    }

    @Transactional
    @Override
    public CommonResponse<ResponseMessageDTO> deleteMessage(long id) {
        Long userId = getCurrentUserId();
        Message message = messageRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ItemNotFoundException("Message not found"));

        messageRepository.deleteMessageById(message.getId());
        log.info("User {} message deleted {}", userId, id);
        return CommonResponse.success(new ResponseMessageDTO("Message deleted"));
    }

    private MessageContentType getContentType(Boolean isProductBased) {
        return Boolean.TRUE.equals(isProductBased) ? MessageContentType.PRODUCT : MessageContentType.GENERAL;
    }
}
