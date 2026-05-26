-- categories: finance → investment (ContentCategory.INVESTMENT 리네이밍)
UPDATE content_categories SET category = 'investment' WHERE category = 'finance';

-- user_interests: finance → investment
UPDATE user_interests SET domain_id = 'investment' WHERE domain_id = 'finance';
