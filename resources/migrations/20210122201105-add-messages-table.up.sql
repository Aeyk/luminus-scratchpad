CREATE TABLE messages
  (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    from_user_id uuid REFERENCES users(id),
    content text NOT NULL

  );
