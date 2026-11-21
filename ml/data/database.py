import os
from urllib.parse import quote_plus

from dotenv import load_dotenv
from sqlalchemy import create_engine, text

load_dotenv()

host = os.getenv("MYSQL_HOST")
port = os.getenv("MYSQL_PORT")
database = os.getenv("MYSQL_DATABASE")
user = os.getenv("MYSQL_USER")
password = os.getenv("MYSQL_PASSWORD")

database_url = (
    f"mysql+pymysql://{quote_plus(user)}:{quote_plus(password)}"
    f"@{host}:{port}/{database}"
)

engine = create_engine(
    database_url,
    pool_pre_ping=True,
)