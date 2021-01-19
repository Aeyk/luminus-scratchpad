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
    permissions
  )
  VALUES (
    :status,
    :email,
    :username,
    :password,
    :user_data,
    :permissions
  );
