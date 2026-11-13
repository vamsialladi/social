package com.truehunt.social.service;

import com.truehunt.social.dto.ChatMessageDto;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ConversationEntity;
import com.truehunt.social.model.MessageEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ConversationRepository;
import com.truehunt.social.repository.MessageRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProfileRepository profileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          UserAccountRepository userAccountRepository,
                          ProfileRepository profileRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userAccountRepository = userAccountRepository;
        this.profileRepository = profileRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChatMessageDto processAndSendMessage(ChatMessageDto req, Principal principal) {
        if (req == null || req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty.");
        }

        String senderRawId = resolveIdentity(principal);
        Optional<UserAccountEntity> senderOpt = resolveUserAccount(senderRawId);
        String senderEmail = senderOpt.map(UserAccountEntity::getEmail).orElse(senderRawId);
        String senderPublicUserId = senderOpt.map(UserAccountEntity::getPublicUserId).orElse(senderRawId);
        String senderName = resolveParticipantDisplayName(senderOpt, senderEmail, senderPublicUserId, senderRawId, req.getSenderName());

        String receiverRawId = req.getReceiverId() != null ? req.getReceiverId().trim() : null;
        Optional<UserAccountEntity> receiverOpt = resolveUserAccount(receiverRawId);
        if (receiverOpt.isEmpty() && req.getReceiverName() != null) {
            receiverOpt = resolveUserAccount(req.getReceiverName());
        }
        String receiverEmail = receiverOpt.map(UserAccountEntity::getEmail).orElse(null);
        String receiverPublicUserId = receiverOpt.map(UserAccountEntity::getPublicUserId).orElse(null);
        String receiverName = resolveParticipantDisplayName(receiverOpt, receiverEmail, receiverPublicUserId, receiverRawId, null);

        if (receiverEmail == null || receiverPublicUserId == null || receiverName == null || receiverName.isBlank()) {
            Optional<ProfileEntity> prof = profileRepository.findByUserId(receiverRawId != null ? receiverRawId : "");
            if (prof.isEmpty() && req.getReceiverName() != null && !req.getReceiverName().isBlank()) {
                final String targetName = req.getReceiverName().trim();
                prof = profileRepository.findByName(targetName).or(() -> profileRepository.findFirstByNameIgnoreCase(targetName));
            }
            if (prof.isPresent()) {
                if (receiverEmail == null) receiverEmail = prof.get().getEmail();
                if (receiverPublicUserId == null) receiverPublicUserId = prof.get().getUserId();
                if (prof.get().getName() != null && !prof.get().getName().isBlank()) {
                    receiverName = prof.get().getName().trim();
                }
            }
        }

        if (receiverEmail == null) receiverEmail = receiverRawId;
        if (receiverPublicUserId == null) receiverPublicUserId = receiverRawId;
        if (receiverName == null || receiverName.isBlank()) {
            receiverName = (req.getReceiverName() != null && !req.getReceiverName().isBlank())
                    ? req.getReceiverName().trim()
                    : resolveUserName(receiverRawId);
        }

        ConversationEntity conversation = null;
        if (req.getConversationId() != null) {
            conversation = conversationRepository.findById(req.getConversationId()).orElse(null);
        }

        if (conversation == null) {
            conversation = findOrCreateConversation(senderEmail, senderPublicUserId, senderName,
                    receiverEmail, receiverPublicUserId, receiverName);
        } else {
            Set<String> senderIdentifiers = new HashSet<>();
            addIdentityKeys(senderIdentifiers, senderEmail);
            addIdentityKeys(senderIdentifiers, senderPublicUserId);
            addIdentityKeys(senderIdentifiers, senderRawId);
            if (senderOpt.isPresent() && senderOpt.get().getId() != null) {
                addIdentityKeys(senderIdentifiers, String.valueOf(senderOpt.get().getId()));
            }

            String user1Id = conversation.getUser1Id();
            String user2Id = conversation.getUser2Id();
            boolean senderIsUser1 = user1Id != null && senderIdentifiers.stream()
                    .anyMatch(identity -> identity.equalsIgnoreCase(user1Id));
            boolean senderIsUser2 = user2Id != null && senderIdentifiers.stream()
                    .anyMatch(identity -> identity.equalsIgnoreCase(user2Id));
            if (!senderIsUser1 && !senderIsUser2) {
                throw new UnauthorizedException("You cannot send messages in this conversation.");
            }

            String partnerId = senderIsUser1 ? conversation.getUser2Id() : conversation.getUser1Id();
            Optional<UserAccountEntity> partnerOpt = resolveUserAccount(partnerId);
            receiverEmail = partnerOpt.map(UserAccountEntity::getEmail).orElse(partnerId);
            receiverPublicUserId = partnerOpt.map(UserAccountEntity::getPublicUserId).orElse(partnerId);
            receiverName = resolveParticipantDisplayName(
                    partnerOpt,
                    receiverEmail,
                    receiverPublicUserId,
                    partnerId,
                    senderIsUser1 ? conversation.getUser2Name() : conversation.getUser1Name()
            );
        }

        Long conversationId = conversation.getId();

        if (receiverEmail == null || receiverEmail.isBlank() || receiverEmail.equalsIgnoreCase(senderEmail)) {
            String partner = (conversation.getUser1Id() != null && !conversation.getUser1Id().equalsIgnoreCase(senderEmail))
                    ? conversation.getUser1Id()
                    : conversation.getUser2Id();
            if (partner != null && !partner.isBlank() && !partner.equalsIgnoreCase(senderEmail)) {
                Optional<UserAccountEntity> partnerOpt = resolveUserAccount(partner);
                receiverEmail = partnerOpt.map(UserAccountEntity::getEmail).orElse(partner);
                receiverPublicUserId = partnerOpt.map(UserAccountEntity::getPublicUserId).orElse(partner);
                receiverName = resolveParticipantDisplayName(partnerOpt, receiverEmail, receiverPublicUserId, partner, null);
            }
        }

        MessageEntity messageEntity = new MessageEntity(
                conversationId,
                senderEmail,
                senderName,
                receiverEmail != null ? receiverEmail : "unknown",
                receiverName,
                req.getContent().trim(),
                req.getAttachment()
        );
        messageEntity = messageRepository.save(messageEntity);

        String outId = (req.getId() != null && !req.getId().isBlank())
                ? req.getId()
                : "msg-" + messageEntity.getId();

        conversation.addMessage(outId, senderEmail, senderName, receiverEmail, req.getContent().trim(), "outgoing", req.getAttachment());
        List<Map<String, Object>> conversationThread = conversation.getThread();
        if (!conversationThread.isEmpty()) {
            conversationThread.get(conversationThread.size() - 1).put("messageId", messageEntity.getId());
            conversation.setThread(conversationThread);
        }
        syncConversationParticipantNames(
                conversation,
                senderEmail,
                senderPublicUserId,
                senderName,
                receiverEmail,
                receiverPublicUserId,
                receiverName
        );
        conversation.setPreview(req.getContent().trim());
        conversation.setDate("Just now");
        conversation.setUnread(true);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        ChatMessageDto outDto = new ChatMessageDto(
                outId,
                conversationId,
                senderEmail,
                senderName,
                receiverEmail,
                receiverName,
                messageEntity.getContent(),
                "outgoing",
                messageEntity.getAttachment(),
                "SENT"
        );
        outDto.setSentAt(LocalDateTime.now().toString());
        outDto.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        outDto.setType(req.getType() != null ? req.getType() : "CHAT");

        deliverRealtime(outDto, senderEmail, receiverEmail, conversationId);

        return outDto;
    }

    private void deliverRealtime(ChatMessageDto outDto, String senderEmail, String receiverEmail, Long conversationId) {
        try {
            Set<String> participants = new LinkedHashSet<>();
            if (senderEmail != null && !senderEmail.isBlank()) participants.add(senderEmail.trim());
            if (receiverEmail != null && !receiverEmail.isBlank()) participants.add(receiverEmail.trim());
            for (String participant : participants) {
                messagingTemplate.convertAndSendToUser(participant, "/queue/messages", outDto);
            }

            log.info("[WebSocket] Real-time message #{} delivered to conversation {} (sender: {}, receiver: {})",
                    outDto.getId(), conversationId, senderEmail, receiverEmail);
        } catch (Exception ex) {
            log.warn("[WebSocket] Could not deliver real-time message over STOMP: {}", ex.getMessage());
        }
    }

    private void addIdentityKeys(Set<String> set, String raw) {
        if (raw == null || raw.isBlank()) return;
        String val = raw.trim();
        set.add(val);
        set.add(val.toLowerCase(Locale.ROOT));
        String noSpace = val.replaceAll("\\s+", "");
        if (!noSpace.equalsIgnoreCase(val)) {
            set.add(noSpace);
            set.add(noSpace.toLowerCase(Locale.ROOT));
        }
    }

    public List<Map<String, Object>> getMessagesForConversation(Long conversationId, String currentUserId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        Set<String> callerIdentifiers = new HashSet<>();
        addIdentityKeys(callerIdentifiers, currentUserId);
        resolveUserAccount(currentUserId).ifPresent(user -> {
            addIdentityKeys(callerIdentifiers, user.getEmail());
            addIdentityKeys(callerIdentifiers, user.getPublicUserId());
            if (user.getId() != null) addIdentityKeys(callerIdentifiers, String.valueOf(user.getId()));
        });
        boolean isParticipant = callerIdentifiers.stream().anyMatch(identity ->
                identity.equalsIgnoreCase(conversation.getUser1Id())
                        || identity.equalsIgnoreCase(conversation.getUser2Id()));
        if (!isParticipant) {
            throw new UnauthorizedException("You cannot view messages in this conversation.");
        }

        String resolvedId = resolveUserAccount(currentUserId)
                .map(UserAccountEntity::getEmail)
                .orElse(currentUserId);
        List<MessageEntity> messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId);
        if (!messages.isEmpty()) {
            return messages.stream().map(m -> {
                Map<String, Object> dto = m.toDto(resolvedId);
                String liveSenderName = resolveLatestUserName(m.getSenderId());
                if (liveSenderName != null) dto.put("senderName", liveSenderName);
                String liveReceiverName = resolveLatestUserName(m.getReceiverId());
                if (liveReceiverName != null) dto.put("receiverName", liveReceiverName);
                return dto;
            }).collect(Collectors.toList());
        }
        ConversationEntity conv = conversationRepository.findById(conversationId).orElse(null);
        return conv != null ? conv.toDto(resolvedId).get("thread") instanceof List ?
                (List<Map<String, Object>>) conv.toDto(resolvedId).get("thread") : List.of() : List.of();
    }

    public void broadcastConversationChange(Long conversationId, String type, String messageId, String content) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("conversationId", conversationId);
        if (messageId != null) event.put("id", messageId);
        if (content != null) event.put("content", content);
        if (messageId != null) event.put("edited", true);
        Set<String> participantEmails = new LinkedHashSet<>();
        for (String participantId : List.of(
                conversation.getUser1Id() == null ? "" : conversation.getUser1Id(),
                conversation.getUser2Id() == null ? "" : conversation.getUser2Id()
        )) {
            String email = resolveUserAccount(participantId)
                    .map(UserAccountEntity::getEmail)
                    .orElse(participantId.contains("@") ? participantId.trim() : "");
            if (!email.isBlank()) participantEmails.add(email);
        }
        for (String participantEmail : participantEmails) {
            messagingTemplate.convertAndSendToUser(participantEmail, "/queue/messages", event);
        }
    }

    private ConversationEntity findOrCreateConversation(String senderEmail, String senderPublicId, String senderName,
                                                        String receiverEmail, String receiverPublicId, String receiverName) {
        List<String> userAKeys = Arrays.asList(senderEmail, senderPublicId);
        List<String> userBKeys = Arrays.asList(receiverEmail, receiverPublicId, receiverName);

        for (String keyA : userAKeys) {
            if (keyA == null || keyA.isBlank()) continue;
            for (String keyB : userBKeys) {
                if (keyB == null || keyB.isBlank()) continue;
                Optional<ConversationEntity> existing = conversationRepository.findByUser1IdAndUser2Id(keyA, keyB);
                if (existing.isPresent()) return existing.get();

                existing = conversationRepository.findByUser1IdAndUser2Id(keyB, keyA);
                if (existing.isPresent()) return existing.get();
            }
        }

        if (receiverName != null && !receiverName.isBlank()) {
            Optional<ConversationEntity> byName = conversationRepository.findFirstByNameIgnoreCase(receiverName.trim());
            if (byName.isPresent()) {
                ConversationEntity conv = byName.get();
                boolean isUnassigned = conv.getUser1Id() == null || conv.getUser2Id() == null;
                boolean matchesSender = (senderEmail != null && (senderEmail.equalsIgnoreCase(conv.getUser1Id()) || senderEmail.equalsIgnoreCase(conv.getUser2Id())))
                        || (senderPublicId != null && (senderPublicId.equalsIgnoreCase(conv.getUser1Id()) || senderPublicId.equalsIgnoreCase(conv.getUser2Id())));
                if (isUnassigned || matchesSender) {
                    if (conv.getUser1Id() == null) conv.setUser1Id(senderEmail);
                    if (conv.getUser2Id() == null) conv.setUser2Id(receiverEmail);
                    return conversationRepository.save(conv);
                }
            }
        }

        ConversationEntity conv = new ConversationEntity();
        conv.setUser1Id(senderEmail != null ? senderEmail : senderPublicId);
        conv.setUser1Name(senderName);
        conv.setUser2Id(receiverEmail != null ? receiverEmail : receiverPublicId);
        conv.setUser2Name(receiverName != null ? receiverName : "Cinema Professional");
        conv.setName(receiverName != null ? receiverName : "Cinema Professional");
        conv.setCategory("Direct");
        conv.setMailbox("Focused");
        conv.setPreview("Start of conversation");
        conv.setDate("Just now");
        return conversationRepository.save(conv);
    }

    public Optional<UserAccountEntity> resolveUserAccount(String identifier) {
        if (identifier == null || identifier.isBlank()) return Optional.empty();
        String clean = identifier.trim();
        String stripped = clean.startsWith("profile-") ? clean.substring("profile-".length()).trim() : clean;

        Optional<UserAccountEntity> byEmail = userAccountRepository.findByEmail(clean.toLowerCase(Locale.ROOT));
        if (byEmail.isPresent()) return byEmail;
        if (!stripped.equalsIgnoreCase(clean)) {
            byEmail = userAccountRepository.findByEmail(stripped.toLowerCase(Locale.ROOT));
            if (byEmail.isPresent()) return byEmail;
        }

        Optional<UserAccountEntity> byPublicId = userAccountRepository.findByPublicUserId(clean)
                .or(() -> userAccountRepository.findByPublicUserId(stripped));
        if (byPublicId.isPresent()) return byPublicId;

        Optional<ProfileEntity> profile = profileRepository.findByUserId(clean)
                .or(() -> profileRepository.findByUserId(stripped));
        if (profile.isEmpty()) {
            try {
                Long pid = Long.parseLong(stripped);
                profile = profileRepository.findById(pid);
            } catch (NumberFormatException ignored) {}
        }
        if (profile.isPresent()) {
            if (profile.get().getEmail() != null && !profile.get().getEmail().isBlank()) {
                Optional<UserAccountEntity> byProfEmail = userAccountRepository.findByEmail(profile.get().getEmail().toLowerCase(Locale.ROOT));
                if (byProfEmail.isPresent()) return byProfEmail;
            }
            if (profile.get().getUserId() != null && !profile.get().getUserId().isBlank()) {
                Optional<UserAccountEntity> byProfUserId = userAccountRepository.findByPublicUserId(profile.get().getUserId());
                if (byProfUserId.isPresent()) return byProfUserId;
            }
        }

        Optional<UserAccountEntity> byName = userAccountRepository.findByName(clean)
                .or(() -> userAccountRepository.findByName(stripped))
                .or(() -> userAccountRepository.findFirstByNameIgnoreCase(clean))
                .or(() -> userAccountRepository.findFirstByNameIgnoreCase(stripped));
        if (byName.isPresent()) return byName;

        Optional<ProfileEntity> profByName = profileRepository.findByName(clean)
                .or(() -> profileRepository.findByName(stripped))
                .or(() -> profileRepository.findFirstByNameIgnoreCase(clean))
                .or(() -> profileRepository.findFirstByNameIgnoreCase(stripped));
        if (profByName.isPresent()) {
            if (profByName.get().getEmail() != null && !profByName.get().getEmail().isBlank()) {
                Optional<UserAccountEntity> byProfName = userAccountRepository.findByEmail(profByName.get().getEmail().toLowerCase(Locale.ROOT));
                if (byProfName.isPresent()) return byProfName;
            }
            if (profByName.get().getUserId() != null && !profByName.get().getUserId().isBlank()) {
                Optional<UserAccountEntity> byProfUserId = userAccountRepository.findByPublicUserId(profByName.get().getUserId());
                if (byProfUserId.isPresent()) return byProfUserId;
            }
        }

        Optional<UserAccountEntity> byAnyName = userAccountRepository.findAll().stream()
                .filter(u -> u.getName() != null && (u.getName().equalsIgnoreCase(clean)
                        || clean.toLowerCase().contains(u.getName().toLowerCase())
                        || u.getName().toLowerCase().contains(clean.toLowerCase())))
                .findFirst();
        if (byAnyName.isPresent()) return byAnyName;

        if ("me".equalsIgnoreCase(clean)) {
            return userAccountRepository.findByEmail("pro@truehunt.in")
                    .or(() -> userAccountRepository.findAll().stream().findFirst());
        }

        return Optional.empty();
    }

    private String resolveIdentity(Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName().trim();
        }
        throw new UnauthorizedException("A valid sign-in is required to send messages.");
    }

    /**
     * Resolves the latest non-blank display name from ProfileEntity or UserAccountEntity.
     */
    public String resolveLatestUserName(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        String clean = identifier.trim();
        String stripped = clean.startsWith("profile-") ? clean.substring("profile-".length()).trim() : clean;

        Optional<ProfileEntity> directProfile = profileRepository.findByEmail(clean.toLowerCase(Locale.ROOT))
                .or(() -> profileRepository.findByEmail(stripped.toLowerCase(Locale.ROOT)))
                .or(() -> profileRepository.findByUserId(clean))
                .or(() -> profileRepository.findByUserId(stripped));
        if (directProfile.isPresent() && directProfile.get().getName() != null && !directProfile.get().getName().isBlank()) {
            return directProfile.get().getName().trim();
        }

        Optional<UserAccountEntity> account = resolveUserAccount(clean);
        if (account.isPresent()) {
            UserAccountEntity acc = account.get();
            Optional<ProfileEntity> linkedProfile = Optional.empty();
            if (acc.getEmail() != null && !acc.getEmail().isBlank()) {
                linkedProfile = profileRepository.findByEmail(acc.getEmail().trim().toLowerCase(Locale.ROOT));
            }
            if (linkedProfile.isEmpty() && acc.getPublicUserId() != null && !acc.getPublicUserId().isBlank()) {
                linkedProfile = profileRepository.findByUserId(acc.getPublicUserId().trim());
            }
            if (linkedProfile.isPresent() && linkedProfile.get().getName() != null && !linkedProfile.get().getName().isBlank()) {
                return linkedProfile.get().getName().trim();
            }
            if (acc.getName() != null && !acc.getName().isBlank()) {
                return acc.getName().trim();
            }
        }
        return null;
    }

    private String resolveParticipantDisplayName(Optional<UserAccountEntity> accountOpt,
                                                 String email,
                                                 String publicUserId,
                                                 String rawId,
                                                 String requestFallbackName) {
        for (String candidate : new String[]{email, publicUserId, rawId}) {
            String live = resolveLatestUserName(candidate);
            if (live != null && !live.isBlank()) {
                return live;
            }
        }
        if (accountOpt != null && accountOpt.isPresent()) {
            String accountName = accountOpt.get().getName();
            if (accountName != null && !accountName.isBlank()) {
                return accountName.trim();
            }
        }
        if (requestFallbackName != null && !requestFallbackName.isBlank()) {
            return requestFallbackName.trim();
        }
        return resolveUserName(rawId);
    }

    private void syncConversationParticipantNames(ConversationEntity conversation,
                                                  String senderEmail, String senderPublicId, String senderName,
                                                  String receiverEmail, String receiverPublicId, String receiverName) {
        if (conversation == null) return;

        String u1 = conversation.getUser1Id();
        String u2 = conversation.getUser2Id();

        String updatedU1Name = resolveSyncedParticipantName(u1, senderEmail, senderPublicId, senderName,
                receiverEmail, receiverPublicId, receiverName);
        if (updatedU1Name != null && !updatedU1Name.isBlank()) {
            conversation.setUser1Name(updatedU1Name);
        }

        String updatedU2Name = resolveSyncedParticipantName(u2, senderEmail, senderPublicId, senderName,
                receiverEmail, receiverPublicId, receiverName);
        if (updatedU2Name != null && !updatedU2Name.isBlank()) {
            conversation.setUser2Name(updatedU2Name);
            conversation.setName(updatedU2Name);
            conversation.setInitials(computeInitials(updatedU2Name));
        }
    }

    private String resolveSyncedParticipantName(String participantId,
                                                String senderEmail, String senderPublicId, String senderName,
                                                String receiverEmail, String receiverPublicId, String receiverName) {
        if (participantId == null || participantId.isBlank()) return null;
        String live = resolveLatestUserName(participantId);
        if (live != null && !live.isBlank()) return live;

        String clean = participantId.trim();
        if ((senderEmail != null && clean.equalsIgnoreCase(senderEmail))
                || (senderPublicId != null && clean.equalsIgnoreCase(senderPublicId))) {
            return senderName;
        }
        if ((receiverEmail != null && clean.equalsIgnoreCase(receiverEmail))
                || (receiverPublicId != null && clean.equalsIgnoreCase(receiverPublicId))) {
            return receiverName;
        }
        return null;
    }

    private String computeInitials(String title) {
        if (title == null || title.isBlank()) return "TH";
        String[] parts = title.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String resolveUserName(String identifier) {
        if (identifier == null || identifier.isBlank()) return "Cinema Professional";
        String live = resolveLatestUserName(identifier);
        if (live != null && !live.isBlank()) return live;

        return identifier;
    }
}
