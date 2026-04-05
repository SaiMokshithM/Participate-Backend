package com.participatehere.service;

import com.participatehere.entity.Activity;
import com.participatehere.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ActivityRepository activityRepository;
    private final RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String chat(String userMessage) {
        // 1. Fetch active activities from the database
        List<Activity> activeActivities = activityRepository.findByStatus(
                Activity.Status.ACTIVE,
                org.springframework.data.domain.PageRequest.of(0, 20)
        ).getContent();

        // 2. Build event list string
        String eventList;
        if (activeActivities.isEmpty()) {
            eventList = "No active events currently. Check back soon!";
        } else {
            eventList = activeActivities.stream()
                    .map(a -> String.format(
                            "Event: %s\n   Category   : %s\n   Description: %s\n   Organizer  : %s\n   Location   : %s\n   Schedule   : %s\n   Spots Left : %d out of %d",
                            a.getName(),
                            a.getCategory(),
                            a.getDescription(),
                            a.getOrganizer(),
                            a.getLocation(),
                            a.getSchedule(),
                            a.getCapacity() - a.getEnrolled(),
                            a.getCapacity()))
                    .collect(Collectors.joining("\n\n"));
        }

        // 3. Compose system prompt
        String systemPrompt = """
                Role: You are "Participate+ AI", an enthusiastic and motivational campus activity advisor
                living inside the student dashboard. Your goal is to EXCITE students and drive them to JOIN events.
                
                Current Active Events (live from database):
                %s
                
                Rules you MUST follow:
                1. ALWAYS name EVERY event by its exact name — cover ALL events in the list, not just one
                2. For EACH event, pull its Description, Location, Organizer, and Schedule — be SPECIFIC
                3. Tell the student what SKILLS or EXPERIENCE they will gain from EACH event
                4. Give genuine MOTIVATION after listing events — why joining any of these matters for their growth
                5. Mention the organizer and location for each event to make it feel real and credible
                6. If an event's spots are running low (< 30%% left), highlight URGENCY for that event
                7. Use 2-3 emojis naturally — keep energy HIGH but not spammy
                8. Structure your response: cover each event briefly (1-2 sentences each), then close with 1 motivational line
                9. NEVER skip or ignore any event from the list above — every event deserves a mention
                10. If no events are available, encourage the student to stay sharp and check back soon
                11. NEVER invent events, locations, or organizers outside the list above
                """.formatted(eventList);

        // 4. Call OpenAI Chat Completions API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "max_tokens", 500,
                "temperature", 0.8,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(GROQ_URL, entity, Map.class);

            if (response == null) {
                return fallbackMessage(activeActivities);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return fallbackMessage(activeActivities);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String reply = (String) message.get("content");
            return reply != null ? reply.trim() : fallbackMessage(activeActivities);

        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage());
            return fallbackMessage(activeActivities);
        }
    }

    private String fallbackMessage(List<Activity> events) {
        if (events.isEmpty()) {
            return "No active events right now, but that's your cue to stay sharp! 💪 "
                    + "Great campus opportunities are on the way — check back soon and be the first to grab a spot!";
        }
        Activity top = events.get(0);
        int spotsLeft = top.getCapacity() - top.getEnrolled();
        String urgency = spotsLeft < (top.getCapacity() * 0.3)
                ? " ⚡ Only " + spotsLeft + " spots left — act fast!"
                : " Spots are still available, but they fill up quick!";
        String others = events.size() > 1
                ? " Plus " + (events.size() - 1) + " more event(s) waiting for you!"
                : "";
        return "🎉 **" + top.getName() + "** is live now — organized by " + top.getOrganizer()
                + " at " + top.getLocation() + ". " + top.getDescription()
                + urgency + others
                + " Join and level up your campus experience! 🚀";
    }
}
