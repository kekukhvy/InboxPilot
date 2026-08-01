package dev.inboxpilot.domain.rules;

import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;

/** Runs YAML examples against a rule's compiled condition without a mailbox. */
public final class RuleTestRunner {

    private static final MessageId TEST_MESSAGE_ID = new MessageId("rule-test");
    private static final Instant TEST_RECEIVED_AT = Instant.EPOCH;
    private final ConditionCompiler compiler = new ConditionCompiler();

    public RuleTestReport run(RuleDefinition rule) {
        MessageCondition condition = compiler.compile(rule.condition());
        List<RuleTestResult> results = rule.tests().stream()
                .map(test -> result(test, condition))
                .toList();
        return new RuleTestReport(rule.id(), results);
    }

    private static RuleTestResult result(RuleTestCase test, MessageCondition condition) {
        MailMessage message = new MailMessage(
                TEST_MESSAGE_ID, test.sender(), test.subject(), TEST_RECEIVED_AT, List.of());
        return new RuleTestResult(test.name(), test.expectedMatch(), condition.matches(message));
    }
}
