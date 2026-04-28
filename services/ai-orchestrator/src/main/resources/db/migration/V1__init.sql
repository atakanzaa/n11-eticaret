CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversations_user ON ai_conversations(user_id, updated_at DESC);

CREATE TABLE ai_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    tool_name VARCHAR(100),
    tool_input JSONB,
    tool_result JSONB,
    token_count INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation ON ai_messages(conversation_id, created_at);

CREATE TABLE ai_usage_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    purpose VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    input_tokens INT,
    output_tokens INT,
    cost_usd DECIMAL(10,6),
    duration_ms INT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usage_user_date ON ai_usage_log(user_id, created_at DESC);
CREATE INDEX idx_usage_purpose ON ai_usage_log(purpose, created_at DESC);
