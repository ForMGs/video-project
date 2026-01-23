import React, { useEffect, useRef, useState } from "react";
import { VideoSummary } from "../types/video";
import { http } from "../api/http";

export type RecoItem = {
  id: string;
  title: string;
  channel: string;
  views: string;
  time: string;
  duration: string;
  thumb: string;
  badge?: string;
  profileImg: string; // ✅ 추가
};
export default function RecommendedRow({
  title = "실시간 급상승 영상"
}: {
  title?: string;
}) {
  const scrollerRef = useRef<HTMLDivElement | null>(null);
  const [atStart, setAtStart] = useState(true);
  const [atEnd, setAtEnd] = useState(false);
  const [items, setItems] = useState<VideoSummary[]>([]);
  const [loading , setLoading] = useState(false);


  const updateEnds = () => {
    const el = scrollerRef.current;
    if (!el) return;
    const { scrollLeft, scrollWidth, clientWidth } = el;
    setAtStart(scrollLeft <= 2);
    setAtEnd(scrollLeft + clientWidth >= scrollWidth - 2);
  };

  useEffect(() => {
    const el = scrollerRef.current;
    if (!el) return;
    updateEnds();
    const onScroll = () => updateEnds();
    el.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", updateEnds);
    return () => {
      el.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", updateEnds);
    };
  }, []);

  useEffect(()=>{
    let alive = true;
    (async() =>{
      if(loading) return;
      setLoading(true);
      try {
        const v = await http.get<VideoSummary[]>("/videos/ranking");
        setItems((prev)=>{
          const m = new Map<number,VideoSummary>();
          for(const x of prev) m.set(x.videoID, x);
          for(const x of v.data) m.set(x.videoID,x);
          return [...m.values()];
        });
      } catch (error) {
        console.log(error);
      }finally{
        if(alive) setLoading(false);
      }

    })();
    return () =>{
      alive = false;
    };
    
  },[]);

  const scrollByPage = (dir: 1 | -1) => {
    const el = scrollerRef.current;
    if (!el) return;
    const delta = Math.round(el.clientWidth * 0.9) * dir;
    el.scrollBy({ left: delta, behavior: "smooth" });
  };

  return (
    <section className="reco">
      <header className="reco-head">
        <h2>{title}</h2>
        <div className="reco-actions">
          <button
            className="reco-nav prev"
            onClick={() => scrollByPage(-1)}
            disabled={atStart}
            aria-label="이전"
          >
            ‹
          </button>
          <button
            className="reco-nav next"
            onClick={() => scrollByPage(1)}
            disabled={atEnd}
            aria-label="다음"
          >
            ›
          </button>
        </div>
      </header>

      <div className="reco-track" ref={scrollerRef}>
        {items.map((v) => (
        <article className="reco-card" key={v.videoID}>
        <a className="thumb" href={`/watch/${v.videoID}`} title={v.title}>
            <div className="thumb-ratio">
            <img src={v.thumbNailUrl} alt={v.title} loading="lazy" />
            </div>
            <span className="duration">{v.duration}</span>
            {/* {v.badge && <span className="badge">{v.badge}</span>} */}
        </a>

        <div className="meta">
            <div className="meta-head">
            <img src={v.profileUrl} alt={v.userName} className="profile-img" />
            <h3 className="title clamp-2" title={v.title}>
                {v.title}
            </h3>
            </div>
            <div className="sub">
            <span className="channel">{v.userName}</span>
            <span className="dot">조회수</span>
            <span className="views">{v.view}</span>
            <span className="dot">회</span>
            {/* <span className="time">{v.time}</span> */}
            </div>
        </div>
        </article>

        ))}
      </div>
    </section>
  );
}
