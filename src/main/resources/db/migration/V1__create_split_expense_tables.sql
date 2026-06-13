-- Users
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- Groups
CREATE TABLE groups (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(255),
    created_by   UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- Group members
CREATE TABLE group_members (
    group_id     UUID NOT NULL REFERENCES groups(id),
    user_id      UUID NOT NULL REFERENCES users(id),
    joined_at    TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);

-- Expenses
CREATE TABLE expenses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id     UUID NOT NULL REFERENCES groups(id),
    paid_by      UUID NOT NULL REFERENCES users(id),
    description  VARCHAR(255) NOT NULL,
    amount       NUMERIC(15, 2) NOT NULL,
    currency     VARCHAR(3) NOT NULL DEFAULT 'USD',
    expense_date DATE NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- Expense splits (who owes what per expense)
CREATE TABLE expense_splits (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id   UUID NOT NULL REFERENCES expenses(id),
    user_id      UUID NOT NULL REFERENCES users(id),
    amount_owed  NUMERIC(15, 2) NOT NULL,
    settled      BOOLEAN NOT NULL DEFAULT false,
    settled_at   TIMESTAMP
);

-- Settlements (payments between members)
CREATE TABLE settlements (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id     UUID NOT NULL REFERENCES groups(id),
    payer_id     UUID NOT NULL REFERENCES users(id),
    payee_id     UUID NOT NULL REFERENCES users(id),
    amount       NUMERIC(15, 2) NOT NULL,
    currency     VARCHAR(3) NOT NULL DEFAULT 'USD',
    settled_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_expenses_group_id     ON expenses(group_id);
CREATE INDEX idx_expenses_paid_by      ON expenses(paid_by);
CREATE INDEX idx_splits_expense_id     ON expense_splits(expense_id);
CREATE INDEX idx_splits_user_id        ON expense_splits(user_id);
CREATE INDEX idx_settlements_group_id  ON settlements(group_id);
