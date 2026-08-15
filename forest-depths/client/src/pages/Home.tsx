/* Forest Depths / Biophilic Editorial: asymmetric environmental storytelling, layered real assets, quiet motion. */
import { lazy, Suspense, useEffect, useMemo, useRef, useState } from "react";
import { ArrowDown, ArrowUpRight, Menu, Minus, MoveDown, X } from "lucide-react";
import AudioDirector from "@/components/AudioDirector";

const IMAGE_EDGE = "/manus-storage/8PFWBcc6WCvj_a335e405.webp";
const IMAGE_FERN = "/manus-storage/jIj7rtVw2ZY5_f8a05998.webp";
const IMAGE_CREEK = "/manus-storage/tZRokU1Ujc6o_82a9e6a4.webp";
const IMAGE_FOG = "/manus-storage/GDwyrw78SVr6_199cad02.webp";
const IMAGE_CEDAR = "/manus-storage/48R5xmBdrhpZ_c0803b1e.webp";
const IMAGE_MARSH = "/manus-storage/1aLT4ss2eJvp_4d5ddf1f.webp";
const LOGO = "/manus-storage/forest-depths-mark_1883c897.png";
const LazyThreeForestScene = lazy(() => import("@/components/ThreeForestScene"));

const hiddenCreatures = [
  { name: "Red fox", scientific: "Vulpes vulpes", cue: "A rust-colored shape pauses beyond the fern line.", kind: "fox" },
  { name: "Great horned owl", scientific: "Bubo virginianus", cue: "The canopy answers with one almost-silent turn.", kind: "owl" },
  { name: "Muntjac deer", scientific: "Muntiacus muntjak", cue: "Two eyes catch the last light between the trunks.", kind: "deer" },
  { name: "Luna moth", scientific: "Actias luna", cue: "A pale wing settles where the fog is warmest.", kind: "moth" },
  { name: "Red fox", scientific: "Vulpes vulpes", cue: "The path continues after the footsteps stop.", kind: "fox" },
  { name: "Great horned owl", scientific: "Bubo virginianus", cue: "Something watches from the cathedral of branches.", kind: "owl" },
  { name: "Luna moth", scientific: "Actias luna", cue: "A brief amber signal moves through the marsh grass.", kind: "moth" },
  { name: "Muntjac deer", scientific: "Muntiacus muntjak", cue: "The boundary stone is not as empty as it seems.", kind: "deer" },
  { name: "Red fox", scientific: "Vulpes vulpes", cue: "At the heartwood, the forest keeps one final witness.", kind: "fox" },
];

const stages = [
  { id: "edge", index: "01", depth: "12 m", meters: 12, eyebrow: "THE FOREST EDGE", title: "The canopy closes before the path does.", body: "Morning light enters at an angle, catching wet leaves and the first signs of a world that does not need an audience.", image: IMAGE_EDGE, tone: "edge", note: "edge habitat / 12 m", transitionCue: "light narrows", cameraCharacter: "slow lateral drift" },
  { id: "fern-passage", index: "02", depth: "34 m", meters: 34, eyebrow: "THE FERN PASSAGE", title: "Small life gathers closest to the ground.", body: "Ferns fold over the trail like a low tide. Every step reveals another scale of green beneath the obvious one.", image: IMAGE_FERN, tone: "fern", note: "understory / 34 m", curiosity: "Look below the first layer.", transitionCue: "foreground density rises", cameraCharacter: "low push through leaves" },
  { id: "moss-creek", index: "03", depth: "61 m", meters: 61, eyebrow: "THE MOSS CREEK", title: "Water writes the oldest line through the forest.", body: "A cold creek moves over stone and root. Its sound travels farther than the path, carrying the shape of the land with it.", image: IMAGE_CREEK, tone: "creek", note: "riparian floor / 61 m", transitionCue: "sound becomes movement", cameraCharacter: "diagonal creek follow" },
  { id: "fog-basin", index: "04", depth: "88 m", meters: 88, eyebrow: "THE FOG BASIN", title: "Distance softens, but nothing disappears.", body: "Mist settles in a shallow basin where trunks become columns and the air turns blue around their edges.", image: IMAGE_FOG, tone: "fog", note: "mist basin / 88 m", curiosity: "Wait for the next outline.", transitionCue: "contrast falls", cameraCharacter: "vertical lift through fog" },
  { id: "cathedral-grove", index: "05", depth: "117 m", meters: 117, eyebrow: "THE CATHEDRAL GROVE", title: "Age becomes architecture.", body: "Cedar trunks rise into a ceiling that holds its own weather. The forest feels less like a place than a room with no walls.", image: IMAGE_CEDAR, tone: "grove", note: "old growth / 117 m", transitionCue: "scale expands", cameraCharacter: "slow upward reveal" },
  { id: "thorn-hollow", index: "06", depth: "143 m", meters: 143, eyebrow: "THE THORN HOLLOW", title: "The path becomes a question of touch.", body: "Branches knot across the hollow, asking the body to slow down. Here, the shortest route is rarely the safest one.", image: IMAGE_FERN, tone: "hollow", note: "thorn understory / 143 m", curiosity: "Do not force the opening.", transitionCue: "path constricts", cameraCharacter: "careful serpentine pass" },
  { id: "night-marsh", index: "07", depth: "169 m", meters: 169, eyebrow: "THE NIGHT MARSH", title: "The dark begins to make its own light.", body: "Water holds the last blue. Fireflies and wet reeds trade small signals across a surface that refuses to stay still.", image: IMAGE_MARSH, tone: "marsh", note: "wetland edge / 169 m", curiosity: "One light answers another.", transitionCue: "amber points emerge", cameraCharacter: "floating marsh glide" },
  { id: "ancient-boundary", index: "08", depth: "196 m", meters: 196, eyebrow: "THE ANCIENT BOUNDARY", title: "A stone can remember a forest.", body: "Roots gather around a weathered marker at the edge of the oldest growth. The boundary is visible only because something kept growing past it.", image: IMAGE_CEDAR, tone: "boundary", note: "old boundary / 196 m", transitionCue: "landmark anchors frame", cameraCharacter: "orbit around stone" },
  { id: "heartwood", index: "09", depth: "224 m", meters: 224, eyebrow: "THE HEARTWOOD", title: "The map ends inside the tree.", body: "At the deepest quiet, bark, root, and shadow become one continuous chamber. The forest does not reveal an answer. It keeps the question alive.", image: IMAGE_FOG, tone: "heartwood", note: "ancient core / 224 m", curiosity: "Stay until the dark feels familiar.", transitionCue: "world folds inward", cameraCharacter: "slow inward descent" },
];

const chapterInteractions = [
  { asset: "/manus-storage/tree_detailed_68f84c2c.png", label: "Bark trace", title: "Read the rings without cutting the tree.", detail: "The outer edge is a threshold: enough light for leaves, enough shadow for the first slow movement beneath them.", x: "11%", y: "18%", kind: "tree" },
  { asset: "/manus-storage/plant_bushDetailed_3394e416.png", label: "Fern understorey", title: "The ground keeps a second canopy.", detail: "Look through the fronds rather than over them. Moisture collects in the lowest architecture, where small lives leave the clearest evidence.", x: "72%", y: "54%", kind: "fern" },
  { asset: "/manus-storage/stone_largeA_9bb59ccc.png", label: "Creek stone", title: "Water remembers every soft edge.", detail: "The creek does not travel in a straight line. It edits the stone slowly, leaving a record that can be felt before it can be named.", x: "18%", y: "61%", kind: "stone" },
  { asset: "/manus-storage/plant_bushDetailed_3394e416.png", label: "Fog fern", title: "The outline is part of the observation.", detail: "In the basin, distance is not empty space. It is a veil that lets one shape arrive before the rest of the forest does.", x: "74%", y: "22%", kind: "fog" },
  { asset: "/manus-storage/tree_oak_d6877cf9.png", label: "Cedar memory", title: "Scale changes the meaning of quiet.", detail: "A trunk this old turns weather into architecture. Stand beneath it long enough and the ceiling begins to feel alive.", x: "10%", y: "32%", kind: "trunk" },
  { asset: "/manus-storage/log_large_bb437177.png", label: "Thorn crossing", title: "Every opening has a cost.", detail: "The fallen log is not a barrier to solve. It is a pause the path has placed in front of the body.", x: "74%", y: "60%", kind: "log" },
  { asset: "/manus-storage/mushroom_redGroup_cbc8ffd2.png", label: "Marsh signal", title: "One amber point answers another.", detail: "Fireflies are not decoration here. They are intervals: brief, low to the ground, and easy to miss when the eye expects a landmark.", x: "17%", y: "23%", kind: "mushroom" },
  { asset: "/manus-storage/stone_tallA_de43a8e6.png", label: "Boundary stone", title: "A marker can outlast its meaning.", detail: "Roots gather around the stone because the forest has no obligation to preserve the line humans once drew through it.", x: "73%", y: "42%", kind: "stone" },
  { asset: "/manus-storage/tree_detailed_68f84c2c.png", label: "Heartwood grain", title: "The deepest room is made of time.", detail: "Inside the heartwood, the map becomes material. Touch is imagined as memory: rings, pressure, dark, and the patient work of remaining.", x: "13%", y: "58%", kind: "heartwood" },
];

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

export default function Home() {
  const [progress, setProgress] = useState(0);
  const [active, setActive] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(false);
  const [threeReady, setThreeReady] = useState(false);
  const [idleVisible, setIdleVisible] = useState(false);
  const [openInteraction, setOpenInteraction] = useState<number | null>(null);
  const hasScrolledRef = useRef(false);

  useEffect(() => {
    const preference = window.matchMedia("(prefers-reduced-motion: reduce)");
    const scheduleThree = () => {
      if (!preference.matches) setThreeReady(true);
    };
    const idleWindow = window as Window & {
      requestIdleCallback?: (callback: IdleRequestCallback, options?: IdleRequestOptions) => number;
      cancelIdleCallback?: (handle: number) => void;
    };
    const usesIdleCallback = Boolean(idleWindow.requestIdleCallback);
    const idleHandle = usesIdleCallback && idleWindow.requestIdleCallback
      ? idleWindow.requestIdleCallback(scheduleThree, { timeout: 1800 })
      : window.setTimeout(scheduleThree, 1200);
    const fallbackTimer = window.setTimeout(scheduleThree, 2200);
    setReducedMotion(preference.matches);
    const onPreference = () => setReducedMotion(preference.matches);
    preference.addEventListener("change", onPreference);

    let raf = 0;
    const update = () => {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      const next = max > 0 ? clamp(window.scrollY / max, 0, 1) : 0;
      setProgress(next);
      setActive(Math.min(stages.length - 1, Math.floor(next * stages.length)));
      raf = 0;
    };
    const onScroll = () => {
      if (reducedMotion) update();
      else if (!raf) raf = requestAnimationFrame(update);
    };
    update();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);
    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
      preference.removeEventListener("change", onPreference);
      if (raf) cancelAnimationFrame(raf);
      if (usesIdleCallback && idleWindow.cancelIdleCallback && typeof idleHandle === "number") idleWindow.cancelIdleCallback(idleHandle);
      else if (typeof idleHandle === "number") window.clearTimeout(idleHandle);
      window.clearTimeout(fallbackTimer);
    };
  }, [reducedMotion]);

  useEffect(() => {
    setOpenInteraction(null);
  }, [active]);

  useEffect(() => {
    let idleTimer: number | undefined;
    const armReveal = () => {
      if (idleTimer) window.clearTimeout(idleTimer);
      setIdleVisible(false);
      if (!hasScrolledRef.current) return;
      idleTimer = window.setTimeout(() => setIdleVisible(true), 2800);
    };
    const onScrollStop = () => {
      hasScrolledRef.current = true;
      armReveal();
    };
    window.addEventListener("scroll", onScrollStop, { passive: true });
    return () => {
      window.removeEventListener("scroll", onScrollStop);
      if (idleTimer) window.clearTimeout(idleTimer);
    };
  }, []);

  const current = stages[active];
  const scrubPosition = progress * (stages.length - 1);
  const hiddenCreature = hiddenCreatures[Math.min(active, hiddenCreatures.length - 1)];
  const activeDepth = useMemo(() => {
    const meters = stages.map((stage) => stage.meters);
    const local = progress * stages.length - active;
    const next = meters[Math.min(active + 1, meters.length - 1)];
    const value = Math.round(meters[active] + (next - meters[active]) * local);
    return Math.max(meters[active], value).toLocaleString("en-US");
  }, [active, progress]);

  const jumpTo = (index: number) => {
    const section = document.getElementById(stages[index].id);
    section?.scrollIntoView({ behavior: reducedMotion ? "auto" : "smooth" });
    setMenuOpen(false);
  };

  return (
    <div className={`forest-app tone-${current.tone} ${reducedMotion ? "reduced-motion" : ""} ${threeReady && !reducedMotion ? "has-three" : ""}`} style={{ "--depth-progress": progress } as React.CSSProperties}>
      {threeReady && !reducedMotion ? (
        <Suspense fallback={null}>
          <LazyThreeForestScene progress={progress} stageCount={stages.length} reducedMotion={reducedMotion} creatureVisible={idleVisible} creatureKind={hiddenCreature.kind} />
        </Suspense>
      ) : null}
      <AudioDirector activeStage={active} progress={progress} reducedMotion={reducedMotion} />
      <div className="environment" aria-hidden="true">
        <div className="scene-stack">
          {stages.map((stage, index) => (
            <div
              key={stage.id}
              className={`scene scene-${index} ${index === active ? "is-active" : ""}`}
              style={{
                backgroundImage: `url(${stage.image})`,
                opacity: reducedMotion ? (index === active ? 1 : 0) : Math.max(0, 1 - Math.abs(scrubPosition - index)),
                transform: `scale(${1.085 - Math.max(0, 1 - Math.abs(scrubPosition - index)) * 0.085})`,
              }}
            />
          ))}
        </div>
        <div className="water-gradient" />
        <div className="pressure-vignette" />
        <div className="particle-field particle-field-a" />
        <div className="particle-field particle-field-b" />
        <div className="ripple-glow" />
        <div className="grain" />
      </div>

      <header className="site-header">
        <button className="brand" onClick={() => jumpTo(0)} aria-label="Return to the forest edge">
          <img src={LOGO} alt="" />
          <span><strong>FOREST</strong><em>DEPTHS</em></span>
        </button>
        <div className="header-meta"><span>FIELD NOTE 01</span><Minus size={12} /><span>AN IMMERSIVE WALK</span></div>
        <button className="menu-toggle" aria-label={menuOpen ? "Close navigation" : "Open navigation"} onClick={() => setMenuOpen((open) => !open)}>
          {menuOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </header>

      <nav className={`chapter-menu ${menuOpen ? "is-open" : ""}`} aria-label="Forest chapters">
        <div className="menu-kicker">CHAPTERS / WALK</div>
        {stages.map((stage, index) => (
          <button key={stage.id} className={index === active ? "is-current" : ""} onClick={() => jumpTo(index)}>
            <span>{stage.index}</span><span>{stage.eyebrow}</span><strong>{stage.depth}</strong>
          </button>
        ))}
      </nav>

      <aside className="depth-rail" aria-label={`Current forest depth ${activeDepth} meters`}>
        <span className="rail-label">DEPTH / M</span>
        <div className="rail-track"><div className="rail-fill" /><div className="rail-dot" /></div>
        <div className="rail-numbers"><span>0</span><span>60</span><span>120</span><span>180</span><span>224</span></div>
      </aside>

      <main>
        <section className="hero-panel panel" id="surface">
          <div className="hero-copy reveal">
            <span className="eyebrow">FOREST DEPTHS / AN IMMERSIVE WALK</span>
            <h1>Walk slowly.<br /><i>Notice what gathers.</i></h1>
            <p className="hero-dek">A scroll-driven journey from the forest edge into the last quiet chamber of the heartwood.</p>
            <button className="begin-button" onClick={() => jumpTo(1)}><span>Enter the forest</span><ArrowDown size={16} /></button>
          </div>
          <div className="hero-index"><span>01</span><span>09</span></div>
          <div className="surface-note"><span>LAT 46° 12' N</span><span>·</span><span>LONG 121° 44' W</span></div>
        </section>

        {stages.slice(1).map((stage, index) => {
          const interactionIndex = index + 1;
          const interaction = chapterInteractions[interactionIndex];
          const isOpen = openInteraction === interactionIndex;
          const isActive = active === interactionIndex;
          return (
            <section className={`story-panel panel align-${index % 2 === 0 ? "right" : "left"} ${isActive ? "is-active" : ""}`} id={stage.id} key={stage.id}>
              <div className="story-copy reveal">
                <div className="story-topline"><span>{stage.index}</span><span>{stage.note}</span></div>
                <span className="eyebrow">{stage.eyebrow}</span>
                <h2 className={isActive ? "narrative-active" : ""}>{stage.title}</h2>
                <p className={isActive ? "narrative-active" : ""}>{stage.body}</p>
                {stage.curiosity ? <div className="curiosity-line"><span>FIELD QUESTION</span><strong>{stage.curiosity}</strong></div> : null}
                <div className="story-rule"><span /><small>WALK TO CONTINUE</small><ArrowDown size={14} /></div>
              </div>
              <div className={`chapter-object chapter-object-${interaction.kind} ${isOpen ? "is-open" : ""}`} style={{ "--object-x": interaction.x, "--object-y": interaction.y } as React.CSSProperties}>
                <button className="object-hotspot" type="button" onClick={() => setOpenInteraction(isOpen ? null : interactionIndex)} aria-expanded={isOpen} aria-controls={`observation-${stage.id}`}>
                  <img src={interaction.asset} alt="" />
                  <span className="object-pulse" aria-hidden="true" />
                  <span className="object-label">{interaction.label}</span>
                </button>
                <div className="object-observation" id={`observation-${stage.id}`} aria-hidden={!isOpen}>
                  <span>FIELD OBSERVATION / {stage.index}</span>
                  <strong>{interaction.title}</strong>
                  <p>{interaction.detail}</p>
                </div>
              </div>
            </section>
          );
        })}

        <section className="closing-panel panel" id="closing">
          <div className="closing-copy reveal"><span className="eyebrow">AFTERLIGHT / FIELD NOTE 01</span><h2>The forest keeps its own time.</h2><p>Every walk is a reminder: the unknown is not empty. It is simply closer than our usual point of view.</p><button className="return-button" onClick={() => jumpTo(0)}>Return to the edge <ArrowUpRight size={15} /></button></div>
          <div className="closing-mark"><img src={LOGO} alt="" /><span>FOREST DEPTHS</span></div>
        </section>
      </main>

      <footer className="site-footer"><span>AN OPEN FOREST STUDY</span><span>WALK POSITION {Math.round(progress * 100)}%</span><span>© 2026 FOREST DEPTHS</span></footer>
      <div className={`creature-reveal ${idleVisible ? "is-visible" : ""} creature-${hiddenCreature.kind}`} aria-live="polite" aria-hidden={!idleVisible}>
        <div className="creature-silhouette" aria-hidden="true" />
        <div className="creature-copy"><span>STILLNESS OBSERVATION</span><strong>{hiddenCreature.name}</strong><em>{hiddenCreature.scientific}</em><p>{hiddenCreature.cue}</p></div>
      </div>
      <div className="depth-readout"><MoveDown size={13} /><strong>{activeDepth}</strong><span>METERS</span></div>
    </div>
  );
}
