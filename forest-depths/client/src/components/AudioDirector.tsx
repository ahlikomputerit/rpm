/* Pelagic Editorial audio layer: quiet, optional, and always user-controlled. */
import { useEffect, useRef, useState } from "react";

const AUDIO_URLS = [
  "/manus-storage/ocean-depths-surface-ambience_41844ad4.wav",
  "/manus-storage/ocean-depths-twilight-ambience_49e45c03.wav",
  "/manus-storage/ocean-depths-abyss-ambience_ec6ba0d9.wav",
];

const CUE_FREQUENCIES = [196, 220, 247, 277, 311, 349, 392, 440, 523];

type AudioDirectorProps = {
  activeStage: number;
  progress: number;
  reducedMotion: boolean;
};

type AudioContextWindow = Window & { webkitAudioContext?: typeof AudioContext };

function trackForStage(stage: number) {
  if (stage <= 2) return 0;
  if (stage <= 5) return 1;
  return 2;
}

export default function AudioDirector({ activeStage, progress, reducedMotion }: AudioDirectorProps) {
  const [muted, setMuted] = useState(() => window.localStorage.getItem("ocean-depths-muted") === "true");
  const [unlocked, setUnlocked] = useState(false);
  const audioRefs = useRef<HTMLAudioElement[]>([]);
  const contextRef = useRef<AudioContext | null>(null);
  const lastStageRef = useRef(activeStage);
  const currentTrackRef = useRef(trackForStage(activeStage));
  const fadeFrameRef = useRef<number | null>(null);
  const mutedRef = useRef(muted);

  useEffect(() => {
    mutedRef.current = muted;
    window.localStorage.setItem("ocean-depths-muted", String(muted));
    audioRefs.current.forEach((audio) => { audio.muted = muted; });
  }, [muted]);

  useEffect(() => {
    audioRefs.current = AUDIO_URLS.map((url) => {
      const audio = new Audio(url);
      audio.loop = true;
      audio.preload = "metadata";
      audio.volume = 0;
      audio.muted = mutedRef.current;
      return audio;
    });
    return () => {
      if (fadeFrameRef.current) cancelAnimationFrame(fadeFrameRef.current);
      audioRefs.current.forEach((audio) => { audio.pause(); audio.src = ""; });
      contextRef.current?.close().catch(() => undefined);
    };
  }, []);

  const playCue = (stage: number) => {
    if (mutedRef.current || reducedMotion) return;
    const AudioContextCtor = window.AudioContext || (window as AudioContextWindow).webkitAudioContext;
    if (!AudioContextCtor) return;
    const context = contextRef.current ?? new AudioContextCtor();
    contextRef.current = context;
    if (context.state === "suspended") void context.resume();
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    oscillator.type = stage >= 6 ? "sine" : stage >= 3 ? "triangle" : "sine";
    oscillator.frequency.setValueAtTime(CUE_FREQUENCIES[stage] ?? 220, context.currentTime);
    oscillator.frequency.exponentialRampToValueAtTime((CUE_FREQUENCIES[stage] ?? 220) * 0.72, context.currentTime + 1.25);
    gain.gain.setValueAtTime(0.0001, context.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.055, context.currentTime + 0.08);
    gain.gain.exponentialRampToValueAtTime(0.0001, context.currentTime + 1.35);
    oscillator.connect(gain).connect(context.destination);
    oscillator.start();
    oscillator.stop(context.currentTime + 1.4);
  };

  const unlock = () => {
    if (unlocked) return;
    setUnlocked(true);
    const context = contextRef.current;
    if (context?.state === "suspended") void context.resume();
    if (mutedRef.current) return;
    const track = audioRefs.current[currentTrackRef.current];
    if (track) void track.play().catch(() => undefined);
  };

  useEffect(() => {
    const onPointerDown = () => unlock();
    window.addEventListener("pointerdown", onPointerDown, { once: true, passive: true });
    return () => window.removeEventListener("pointerdown", onPointerDown);
  });

  useEffect(() => {
    if (!unlocked) return;
    const nextTrack = trackForStage(activeStage);
    const activeTrack = audioRefs.current[nextTrack];
    if (!activeTrack) return;
    if (nextTrack !== currentTrackRef.current) {
      const previousTrack = audioRefs.current[currentTrackRef.current];
      currentTrackRef.current = nextTrack;
      activeTrack.volume = 0;
      void activeTrack.play().catch(() => undefined);
      const startedAt = performance.now();
      const fade = (now: number) => {
        const amount = Math.min(1, (now - startedAt) / 1800);
        activeTrack.volume = mutedRef.current ? 0 : amount * 0.18;
        if (previousTrack) previousTrack.volume = mutedRef.current ? 0 : (1 - amount) * 0.18;
        if (amount < 1) fadeFrameRef.current = requestAnimationFrame(fade);
        else previousTrack?.pause();
      };
      fadeFrameRef.current = requestAnimationFrame(fade);
    }
    if (activeStage !== lastStageRef.current) playCue(activeStage);
    lastStageRef.current = activeStage;
  }, [activeStage, unlocked, reducedMotion]);

  useEffect(() => {
    if (!unlocked || muted || reducedMotion) return;
    const track = audioRefs.current[currentTrackRef.current];
    if (track) track.volume = Math.min(0.2, 0.08 + progress * 0.04);
  }, [progress, unlocked, muted, reducedMotion]);

  const toggleMute = () => {
    setMuted((value) => !value);
    if (!unlocked) unlock();
  };

  return (
    <button className={`audio-toggle ${muted ? "is-muted" : ""}`} onClick={toggleMute} aria-pressed={!muted} aria-label={muted ? "Enable ocean sound" : "Mute ocean sound"}>
      <span className="audio-bars" aria-hidden="true"><i /><i /><i /></span>
      <span>{muted ? "SOUND OFF" : "SOUND ON"}</span>
    </button>
  );
}
