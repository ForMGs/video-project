import os
from minio import Minio

def get_minio_client():
    return Minio(
        os.getenv("MINIO_ENDPOINT","minio:9000"),
        access_key = os.getenv("MINIO_ACCESS_KEY"),
        secret_key = os.getenv("MINIO_SECRET_KEY"),
        secure=os.getenv("MINO_SECURE","flase").lower() == "true",
    )

def download_from_minio(bucket: str, object_key: str, dst_path:str):
    client = get_minio_client()
    client.fget_object(bucket, object_key, dst_path)
