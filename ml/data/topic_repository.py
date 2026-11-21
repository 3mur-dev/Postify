from sqlalchemy import text

from data.database import engine

def get_topics():
    query = text("""
        SELECT
            id,
            title,
            slug,
            parent_id
        FROM topics
    """)

    with engine.connect() as conn:
        results = conn.execute(query)

    return results.mappings().all()