CREATE TABLE IF NOT EXISTS songs (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `title` TEXT NOT NULL,
    `album` TEXT NOT NULL,
    `artist` TEXT NOT NULL,
    `file_path` TEXT NOT NULL
);