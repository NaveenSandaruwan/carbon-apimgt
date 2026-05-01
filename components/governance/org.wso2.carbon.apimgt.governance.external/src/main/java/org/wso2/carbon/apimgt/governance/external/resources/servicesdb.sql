CREATE TABLE EXTERNAL_SERVICES (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    prompt TEXT,
    timeout_ms INTEGER DEFAULT 5000,
    retry_count INTEGER DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    CREATE TYPE header_category AS ENUM ('Standard', 'Security');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE EXTERNAL_SERVICE_HEADERS (
    id SERIAL PRIMARY KEY,
    service_id UUID REFERENCES EXTERNAL_SERVICES(id) ON DELETE CASCADE,
    header_key VARCHAR(100) NOT NULL,
    header_value TEXT NOT NULL,
    category header_category DEFAULT 'Standard',

    CONSTRAINT fk_service
        FOREIGN KEY(service_id) 
        REFERENCES EXTERNAL_SERVICES(id) 
        ON DELETE CASCADE   
);