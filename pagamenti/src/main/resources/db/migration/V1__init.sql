CREATE TABLE pagamenti (
  id UUID PRIMARY KEY,
  ordine_id UUID NOT NULL,
  importo NUMERIC(10,2) NOT NULL,
  stato VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_pagamenti_ordine_id ON pagamenti (ordine_id);

CREATE TABLE outbox_events (
  id UUID PRIMARY KEY,
  aggregate_type VARCHAR(100) NOT NULL,
  aggregate_id VARCHAR(100) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL,
  published BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_events_published ON outbox_events (published);

CREATE TABLE processed_events (
  event_id UUID PRIMARY KEY,
  processed_at TIMESTAMP NOT NULL DEFAULT now()
);
