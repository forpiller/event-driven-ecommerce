CREATE TABLE prodotti (
  id VARCHAR(100) PRIMARY KEY,
  nome VARCHAR(255) NOT NULL,
  quantita INT NOT NULL DEFAULT 0
);

CREATE TABLE processed_events (
  event_id UUID PRIMARY KEY,
  processed_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO prodotti (id, nome, quantita) VALUES ('PROD-1', 'Prodotto Demo', 100);
INSERT INTO prodotti (id, nome, quantita) VALUES ('PROD-OUT', 'Prodotto Esaurito', 0);
