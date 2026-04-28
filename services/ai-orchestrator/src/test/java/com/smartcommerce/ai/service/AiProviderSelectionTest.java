package com.smartcommerce.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that exactly one AiProvider adapter is registered based on the
 * {@code ai.provider} property — Anthropic by default, OpenAI or Gemini when
 * the env var picks a different provider. This is the contract Atakan asked
 * for: env switch, no code change.
 */
class AiProviderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
        .withBean(com.smartcommerce.ai.repository.AiUsageLogRepository.class,
            () -> mock(com.smartcommerce.ai.repository.AiUsageLogRepository.class))
        .withBean(AiBudgetGuard.class,
            () -> mock(AiBudgetGuard.class))
        .withUserConfiguration(AnthropicAdapter.class, OpenAiAdapter.class, GeminiAdapter.class);

    @Test
    @DisplayName("default (no ai.provider set) loads AnthropicAdapter")
    void defaultLoadsAnthropic() {
        runner.run(ctx -> {
            assertThat(ctx.getBeansOfType(AiProvider.class)).hasSize(1);
            assertThat(ctx.getBean(AiProvider.class)).isInstanceOf(AnthropicAdapter.class);
            assertThat(ctx.getBean(AiProvider.class).getName()).isEqualTo("anthropic");
        });
    }

    @Test
    @DisplayName("ai.provider=openai loads only OpenAiAdapter")
    void openaiLoadsOpenAi() {
        runner.withPropertyValues("ai.provider=openai").run(ctx -> {
            assertThat(ctx.getBeansOfType(AiProvider.class)).hasSize(1);
            assertThat(ctx.getBean(AiProvider.class)).isInstanceOf(OpenAiAdapter.class);
            assertThat(ctx.getBean(AiProvider.class).getName()).isEqualTo("openai");
        });
    }

    @Test
    @DisplayName("ai.provider=gemini loads only GeminiAdapter")
    void geminiLoadsGemini() {
        runner.withPropertyValues("ai.provider=gemini").run(ctx -> {
            assertThat(ctx.getBeansOfType(AiProvider.class)).hasSize(1);
            assertThat(ctx.getBean(AiProvider.class)).isInstanceOf(GeminiAdapter.class);
            assertThat(ctx.getBean(AiProvider.class).getName()).isEqualTo("gemini");
        });
    }

    @Test
    @DisplayName("ai.provider=anthropic explicitly still loads AnthropicAdapter")
    void anthropicExplicit() {
        runner.withPropertyValues("ai.provider=anthropic").run(ctx -> {
            assertThat(ctx.getBeansOfType(AiProvider.class)).hasSize(1);
            assertThat(ctx.getBean(AiProvider.class)).isInstanceOf(AnthropicAdapter.class);
        });
    }
}
