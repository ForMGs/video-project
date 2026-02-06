from typing import List, Optional
from pydantic import BaseModel , Field 

class Chapter(BaseModel):
    start: int = Field(ge=0, description="start time in integer seconds")
    title: str = Field(min_length=1, max_length=80)
    description: Optional[str] = Field(default=None, max_length=200)    

class ChaptersOut(BaseModel):
    chapters: List[Chapter] = Field(min_length=1, max_length=20)    