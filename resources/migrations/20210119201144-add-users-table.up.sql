CREATE TABLE users
  (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    email             text COLLATE pg_catalog."default" NOT NULL,
    username          text COLLATE pg_catalog."default" NOT NULL,
    password          text COLLATE pg_catalog."default" NOT NULL,
    
    permissions JSONB,
    last_login TIMESTAMP,
    is_active BOOLEAN,
    pass VARCHAR(300),

    CONSTRAINT account_pkey PRIMARY KEY (id),
    CONSTRAINT account_email_key UNIQUE (email),
    CONSTRAINT account_username_key UNIQUE (username)
  );
