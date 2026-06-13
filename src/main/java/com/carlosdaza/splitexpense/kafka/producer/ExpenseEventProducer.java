package com.carlosdaza.splitexpense.kafka.producer;

import com.carlosdaza.splitexpense.domain.dto.SplitDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventProducer {

    private final KafkaTemplate<String, SplitDto.ExpenseEvent> kafkaTemplate;

    @Value("${kafka.topics.expense-events}")
    private String expenseEventsTopic;

    public void publishExpenseEvent(SplitDto.ExpenseEvent event) {
        log.info("Publishing expense event: type={} expenseId={}", event.eventType(), event.expenseId());
        kafkaTemplate.send(expenseEventsTopic, event.expenseId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish expense event: {}", ex.getMessage());
                    } else {
                        log.info("Expense event published: offset={}", result.getRecordMetadata().offset());
                    }
                });
    }
}
