CREATE TABLE ordini (
  id UUID PRIMARY KEY,
  prodotto_id VARCHAR(100) NOT NULL,
  quantita INT NOT NULL,
  importo_totale NUMERIC(10,2) NOT NULL,
  stato VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

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
