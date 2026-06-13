package com.carlosdaza.splitexpense.kafka.consumer;

import com.carlosdaza.splitexpense.domain.dto.SplitDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExpenseEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.expense-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(SplitDto.ExpenseEvent event) {
        log.info("Received expense event: type={} expenseId={} group={}",
                event.eventType(), event.expenseId(), event.groupName());

        switch (event.eventType()) {
            case "EXPENSE_CREATED" -> handleExpenseCreated(event);
            case "EXPENSE_SETTLED" -> handleExpenseSettled(event);
            default -> log.warn("Unknown event type: {}", event.eventType());
        }
    }

    private void handleExpenseCreated(SplitDto.ExpenseEvent event) {
        // In production: push notifications to group members,
        // update activity feed, send email summary, etc.
        log.info("EXPENSE_CREATED — group={} paidBy={} amount={} {}",
                event.groupName(), event.paidByName(), event.amount(), event.currency());
    }

    private void handleExpenseSettled(SplitDto.ExpenseEvent event) {
        log.info("EXPENSE_SETTLED — group={} amount={} {}",
                event.groupName(), event.amount(), event.currency());
    }
}
