CREATE TABLE notifica_log (
  id UUID PRIMARY KEY,
  ordine_id UUID NOT NULL,
  tipo VARCHAR(50) NOT NULL,
  messaggio TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE processed_events (
  event_id UUID PRIMARY KEY,
  processed_at TIMESTAMP NOT NULL DEFAULT now()
);
