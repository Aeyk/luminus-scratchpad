-- :name all-users
-- :command :query
-- :result :many
-- :doc Selects all users
SELECT
  id,
  status,
  email,
  username,
  password,
  user_data,
  history,
  permissions
  FROM
      users;

-- :name get-user-by-id
-- :command :query
-- :result :one
-- :doc Selects the user matching the id
SELECT
  id,
  status,
  email,
  username,
  password,
  user_data,
history,
  permissions
  FROM
      users
 WHERE
  id = :id ::uuid;

-- :name get-user-by-username
-- :command :query
-- :result :one
-- :doc Selects the user matching the username
SELECT
  id,
  status,
  email,
  username,
  password,
  user_data,
  history,
  permissions
  FROM
      users
 WHERE
  LOWER(username) = LOWER(:username);

-- :name get-user-by-email
-- :command :query
-- :result :one
-- :doc Selects the user matching the email
SELECT
  id,
  status,
  email,
  username,
  password,
  user_data,
  history,
  permissions
  FROM
      users
 WHERE
  LOWER(email) = LOWER(:email);

-- :name insert-user!
-- :command :insert
-- :result :raw
-- :doc Inserts a single user into users table
  INSERT INTO users (
    status,
    email,
    username,
    password,
    user_data,
    history,
    permissions
  )
  VALUES (
    :status,
    :email,
    :username,
    :password,
    :user_data,
    :history,
    :permissions
  );

-- :name update-user-history!
-- :command :execute
-- :result :affected
-- :doc Update user history
UPDATE users
   SET    history = :history
 WHERE  id = :id ::uuid;


-- :name insert-message!
-- :command :insert
-- :result :raw
-- :doc Send a new message from a user
INSERT INTO messages (
  from_user_id,
  content
) VALUES (
  (CAST :from_user_id AS uuid),
  :content
);


-- :name get-most-recent-messages
-- :command :query
-- :result :many
-- :doc Get nth most recent messages
SELECT *
  FROM messages
 ORDER BY created_at DESC
 LIMIT :count;





