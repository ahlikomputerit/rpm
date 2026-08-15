/* Forest Depths / Biophilic Editorial: optional woodland sound layer, user-controlled, quiet, and reduced-motion safe. */
import { useCallback, useEffect, useRef, useState } from "react";

type AudioDirectorProps = {
  activeStage: number;
  progress: number;
  reducedMotion: boolean;
};

type Habitat = "edge" | "understory" | "heartwood";
type AudioContextWindow = Window & { webkitAudioContext?: typeof AudioContext };

const HABITATS: Habitat[] = ["edge", "understory", "heartwood"];
const CUE_FREQUENCIES = [196, 220, 247, 277, 311, 349, 392, 440, 523];
const CUE_FILTERS = [1500, 1200, 900, 780, 620, 520, 430, 340, 260];

function habitatForStage(stage: number): Habitat {
  if (stage <= 2) return "edge";
  if (stage <= 5) return "understory";
  return "heartwood";
}

function getAudioContext() {
  const ctor = window.AudioContext || (window as AudioContextWindow).webkitAudioContext;
  return ctor ? new ctor() : null;
}

function createNoiseBuffer(context: AudioContext) {
  const length = context.sampleRate * 4;
  const buffer = context.createBuffer(1, length, context.sampleRate);
  const channel = buffer.getChannelData(0);
  let last = 0;
  for (let i = 0; i < length; i += 1) {
    const white = Math.random() * 2 - 1;
    last = last * 0.985 + white * 0.15;
    channel[i] = last;
  }
  return buffer;
}

function connectHabitat(context: AudioContext, master: GainNode, habitat: Habitat, noiseBuffer: AudioBuffer) {
  const source = context.createBufferSource();
  source.buffer = noiseBuffer;
  source.loop = true;
  const filter = context.createBiquadFilter();
  filter.type = habitat === "edge" ? "lowpass" : habitat === "understory" ? "bandpass" : "lowpass";
  filter.frequency.value = habitat === "edge" ? 1200 : habitat === "understory" ? 680 : 330;
  filter.Q.value = habitat === "understory" ? 0.7 : 0.45;
  const gain = context.createGain();
  gain.gain.value = 0;
  source.connect(filter).connect(gain).connect(master);
  source.start();
  return { source, filter, gain };
}

export default function AudioDirector({ activeStage, progress, reducedMotion }: AudioDirectorProps) {
  const [muted, setMuted] = useState(() => window.localStorage.getItem("forest-depths-muted") === "true");
  const [unlocked, setUnlocked] = useState(false);
  const contextRef = useRef<AudioContext | null>(null);
  const masterRef = useRef<GainNode | null>(null);
  const habitatsRef = useRef<Record<Habitat, ReturnType<typeof connectHabitat> | null>>({ edge: null, understory: null, heartwood: null });
  const noiseRef = useRef<AudioBuffer | null>(null);
  const activeHabitatRef = useRef<Habitat>(habitatForStage(activeStage));
  const lastStageRef = useRef(activeStage);
  const mutedRef = useRef(muted);
  const reducedMotionRef = useRef(reducedMotion);

  useEffect(() => {
    mutedRef.current = muted;
    window.localStorage.setItem("forest-depths-muted", String(muted));
    if (masterRef.current) masterRef.current.gain.value = muted ? 0 : 0.16;
  }, [muted]);

  useEffect(() => {
    reducedMotionRef.current = reducedMotion;
  }, [reducedMotion]);

  const unlock = useCallback(() => {
    if (unlocked) {
      if (contextRef.current?.state === "suspended") void contextRef.current.resume();
      return;
    }
    const context = contextRef.current ?? getAudioContext();
    if (!context) return;
    contextRef.current = context;
    const master = context.createGain();
    master.gain.value = mutedRef.current ? 0 : 0.16;
    master.connect(context.destination);
    const noiseBuffer = createNoiseBuffer(context);
    noiseRef.current = noiseBuffer;
    habitatsRef.current = {
      edge: connectHabitat(context, master, "edge", noiseBuffer),
      understory: connectHabitat(context, master, "understory", noiseBuffer),
      heartwood: connectHabitat(context, master, "heartwood", noiseBuffer),
    };
    const initialHabitat = habitatForStage(activeStage);
    activeHabitatRef.current = initialHabitat;
    if (!mutedRef.current && !reducedMotionRef.current) habitatsRef.current[initialHabitat]?.gain.gain.setValueAtTime(1, context.currentTime);
    masterRef.current = master;
    setUnlocked(true);
    if (context.state === "suspended") void context.resume();
  }, [unlocked]);

  useEffect(() => {
    const onUnlock = () => unlock();
    window.addEventListener("pointerdown", onUnlock, { once: true, passive: true });
    window.addEventListener("keydown", onUnlock, { once: true });
    return () => {
      window.removeEventListener("pointerdown", onUnlock);
      window.removeEventListener("keydown", onUnlock);
    };
  }, [unlock]);

  useEffect(() => {
    if (!unlocked) return;
    const context = contextRef.current;
    const target = habitatForStage(activeStage);
    const previous = activeHabitatRef.current;
    if (!context || target === previous) return;
    activeHabitatRef.current = target;
    const now = context.currentTime;
    HABITATS.forEach((habitat) => {
      const track = habitatsRef.current[habitat];
      if (!track) return;
      track.gain.gain.cancelScheduledValues(now);
      track.gain.gain.setTargetAtTime(habitat === target && !mutedRef.current ? 1 : 0, now, 1.8);
    });
  }, [activeStage, unlocked]);

  useEffect(() => {
    if (!unlocked || !masterRef.current) return;
    const context = contextRef.current;
    if (!context) return;
    masterRef.current.gain.setTargetAtTime(muted || reducedMotion ? 0 : Math.min(0.2, 0.09 + progress * 0.07), context.currentTime, 0.18);
  }, [progress, muted, reducedMotion, unlocked]);

  useEffect(() => {
    if (!unlocked || reducedMotion || activeStage === lastStageRef.current) return;
    const context = contextRef.current;
    if (!context || mutedRef.current) {
      lastStageRef.current = activeStage;
      return;
    }
    const oscillator = context.createOscillator();
    const tone = context.createGain();
    const filter = context.createBiquadFilter();
    const now = context.currentTime;
    oscillator.type = activeStage >= 6 ? "sine" : activeStage >= 3 ? "triangle" : "sine";
    oscillator.frequency.setValueAtTime(CUE_FREQUENCIES[activeStage] ?? 220, now);
    oscillator.frequency.exponentialRampToValueAtTime((CUE_FREQUENCIES[activeStage] ?? 220) * 0.72, now + 1.05);
    filter.type = "lowpass";
    filter.frequency.setValueAtTime(CUE_FILTERS[activeStage] ?? 500, now);
    tone.gain.setValueAtTime(0.0001, now);
    tone.gain.exponentialRampToValueAtTime(0.035, now + 0.06);
    tone.gain.exponentialRampToValueAtTime(0.0001, now + 1.2);
    oscillator.connect(filter).connect(tone).connect(context.destination);
    oscillator.start(now);
    oscillator.stop(now + 1.25);
    lastStageRef.current = activeStage;
  }, [activeStage, reducedMotion, unlocked]);

  useEffect(() => () => {
    Object.values(habitatsRef.current).forEach((track) => track?.source.stop());
    contextRef.current?.close().catch(() => undefined);
  }, []);

  const toggleMute = () => {
    unlock();
    setMuted((value) => !value);
  };

  return (
    <button className={`audio-toggle ${muted ? "is-muted" : ""}`} onClick={toggleMute} aria-pressed={!muted} aria-label={muted ? "Enable forest sound" : "Mute forest sound"}>
      <span className="audio-bars" aria-hidden="true"><i /><i /><i /></span>
      <span>{muted ? "SOUND OFF" : "SOUND ON"}</span>
    </button>
  );
}
