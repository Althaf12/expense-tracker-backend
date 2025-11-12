-- SQL script to create the user_expense_category table
-- Run this script manually in your MySQL database

CREATE TABLE IF NOT EXISTS user_expense_category (
  user_expense_category_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL,
  user_expense_category_name VARCHAR(255) NOT NULL,
  last_update_tmstp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  status VARCHAR(1) NOT NULL DEFAULT 'A',
  CONSTRAINT uq_user_category UNIQUE (username, user_expense_category_name),
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Example queries to verify the table:
-- SELECT * FROM user_expense_category WHERE username = 'testuser';
-- SELECT COUNT(*) FROM user_expense_category WHERE username = 'testuser';
