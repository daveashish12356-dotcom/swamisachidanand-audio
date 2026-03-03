-- MySQL table for storing suvichar text sent from Telegram bot

CREATE TABLE IF NOT EXISTS suvichar (
  id INT AUTO_INCREMENT PRIMARY KEY,
  text TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

