-- Change analyzed_content from VARCHAR to TEXT to support longer post content
ALTER TABLE post_topic_analysis
    MODIFY COLUMN analyzed_content TEXT;
