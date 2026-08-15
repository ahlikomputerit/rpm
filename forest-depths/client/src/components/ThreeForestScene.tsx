/* Forest Depths / Biophilic Editorial: live 3D environmental layer, real PNG depth objects, fog, roots, and scroll-led camera choreography. */
import { useEffect, useRef } from "react";
import * as THREE from "three";

type ThreeForestSceneProps = {
  progress: number;
  stageCount?: number;
  reducedMotion: boolean;
  creatureVisible?: boolean;
  creatureKind?: string;
};

function makeParticleField(count: number, spread: THREE.Vector3, color: number, size: number) {
  const positions = new Float32Array(count * 3);
  for (let i = 0; i < count; i += 1) {
    positions[i * 3] = (Math.random() - 0.5) * spread.x;
    positions[i * 3 + 1] = (Math.random() - 0.5) * spread.y;
    positions[i * 3 + 2] = (Math.random() - 0.5) * spread.z;
  }
  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
  const material = new THREE.PointsMaterial({ color, size, transparent: true, opacity: 0.62, depthWrite: false, blending: THREE.AdditiveBlending });
  return new THREE.Points(geometry, material);
}

function makeTrunk(x: number, z: number, height: number, radius: number, color: number) {
  const group = new THREE.Group();
  const trunk = new THREE.Mesh(new THREE.CylinderGeometry(radius * 0.78, radius, height, 10), new THREE.MeshStandardMaterial({ color, roughness: 1, flatShading: true }));
  trunk.position.y = height / 2 - 3.8;
  trunk.rotation.z = (x % 2) * 0.035;
  group.add(trunk);
  for (let i = 0; i < 3; i += 1) {
    const root = new THREE.Mesh(new THREE.CylinderGeometry(0.035, radius * 0.3, 1.8 + i * 0.32, 7), new THREE.MeshStandardMaterial({ color, roughness: 1, flatShading: true }));
    root.position.set((i - 1) * radius * 0.8, -3.35, 0.12);
    root.rotation.z = (i - 1) * 0.55;
    root.rotation.x = -0.25;
    group.add(root);
  }
  group.position.set(x, 0, z);
  return group;
}

function makeFireflyField(count: number) {
  const group = new THREE.Group();
  for (let i = 0; i < count; i += 1) {
    const light = new THREE.Mesh(new THREE.SphereGeometry(0.035 + (i % 3) * 0.012, 8, 8), new THREE.MeshBasicMaterial({ color: 0xd7a45a, transparent: true, opacity: 0.7, blending: THREE.AdditiveBlending }));
    light.position.set(Math.sin(i * 2.17) * 4.5, -1.8 + (i % 7) * 0.48, -3 - (i % 8) * 2.4);
    light.userData.phase = i * 0.63;
    group.add(light);
  }
  return group;
}


type CameraPath = { x: number; y: number; z: number; lookX: number; lookY: number; lookZ: number; roll: number; fov: number; swayX: number; swayY: number };

const OBJECT_PNG_URLS = {
  trees: "/manus-storage/tree_detailed_68f84c2c.png",
  rocks: "/manus-storage/stone_largeA_9bb59ccc.png",
  fern: "/manus-storage/plant_bushDetailed_3394e416.png",
  mushroom: "/manus-storage/mushroom_redGroup_cbc8ffd2.png",
};

const PLATE_URLS = [
  "/manus-storage/8PFWBcc6WCvj_a335e405.webp",
  "/manus-storage/jIj7rtVw2ZY5_f8a05998.webp",
  "/manus-storage/tZRokU1Ujc6o_82a9e6a4.webp",
  "/manus-storage/GDwyrw78SVr6_199cad02.webp",
  "/manus-storage/48R5xmBdrhpZ_c0803b1e.webp",
  "/manus-storage/jIj7rtVw2ZY5_f8a05998.webp",
  "/manus-storage/1aLT4ss2eJvp_4d5ddf1f.webp",
  "/manus-storage/48R5xmBdrhpZ_c0803b1e.webp",
  "/manus-storage/GDwyrw78SVr6_199cad02.webp",
];

const CAMERA_PATHS: CameraPath[] = [
  { x: 0, y: 0.1, z: 0.1, lookX: 0, lookY: 0, lookZ: 0, roll: 0, fov: 46, swayX: 0.2, swayY: 0.03 },
  { x: -0.9, y: 0.35, z: 0.8, lookX: -0.4, lookY: 0.15, lookZ: -0.8, roll: -0.035, fov: 49, swayX: 0.34, swayY: 0.05 },
  { x: 0.95, y: -0.25, z: 0.15, lookX: 0.65, lookY: -0.15, lookZ: 0.4, roll: 0.045, fov: 51, swayX: 0.42, swayY: 0.07 },
  { x: -1.05, y: 0.55, z: -0.55, lookX: -0.55, lookY: 0.35, lookZ: -0.4, roll: -0.06, fov: 48, swayX: 0.28, swayY: 0.05 },
  { x: 1.35, y: 0.2, z: -0.7, lookX: 0.9, lookY: 0.12, lookZ: -1.2, roll: 0.08, fov: 54, swayX: 0.52, swayY: 0.08 },
  { x: -0.45, y: -1.2, z: 1.05, lookX: -0.25, lookY: -0.6, lookZ: 0.8, roll: -0.025, fov: 50, swayX: 0.2, swayY: 0.035 },
  { x: 1.2, y: 0.65, z: -0.9, lookX: 0.65, lookY: 0.28, lookZ: -1.5, roll: 0.065, fov: 56, swayX: 0.4, swayY: 0.06 },
  { x: -0.3, y: -1.5, z: 1.35, lookX: -0.35, lookY: -0.75, lookZ: 1.15, roll: -0.045, fov: 52, swayX: 0.16, swayY: 0.025 },
  { x: 0, y: -1.85, z: 1.8, lookX: 0, lookY: -0.95, lookZ: 1.7, roll: 0, fov: 48, swayX: 0.08, swayY: 0.015 },
];

function mixPath(a: CameraPath, b: CameraPath, amount: number): CameraPath {
  const mix = (from: number, to: number) => from + (to - from) * amount;
  return {
    x: mix(a.x, b.x), y: mix(a.y, b.y), z: mix(a.z, b.z),
    lookX: mix(a.lookX, b.lookX), lookY: mix(a.lookY, b.lookY), lookZ: mix(a.lookZ, b.lookZ),
    roll: mix(a.roll, b.roll), fov: mix(a.fov, b.fov), swayX: mix(a.swayX, b.swayX), swayY: mix(a.swayY, b.swayY),
  };
}


function seededNoise(x: number, z: number, seed = 0) {
  return Math.sin(x * 1.73 + z * 0.91 + seed * 2.37) * 0.5 + Math.sin(x * 0.47 - z * 1.31 + seed) * 0.3 + Math.sin(x * 3.9 + z * 2.2) * 0.2;
}


function makeDepthRing(z: number, scale: number, color: number) {
  const material = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.14, side: THREE.DoubleSide, blending: THREE.AdditiveBlending });
  const ring = new THREE.Mesh(new THREE.RingGeometry(2.3, 2.34, 64), material);
  ring.position.z = z;
  ring.scale.setScalar(scale);
  ring.rotation.x = Math.PI / 2.2;
  return ring;
}


export default function ThreeForestScene({ progress, stageCount = 9, reducedMotion, creatureVisible = false, creatureKind = "moth" }: ThreeForestSceneProps) {
  const mountRef = useRef<HTMLDivElement>(null);
  const progressRef = useRef(progress);
  const stageCountRef = useRef(stageCount);
  const reducedMotionRef = useRef(reducedMotion);
  const creatureVisibleRef = useRef(creatureVisible);
  const creatureKindRef = useRef(creatureKind);

  useEffect(() => {
    progressRef.current = progress;
    stageCountRef.current = stageCount;
    reducedMotionRef.current = reducedMotion;
    creatureVisibleRef.current = creatureVisible;
    creatureKindRef.current = creatureKind;
  }, [progress, stageCount, reducedMotion, creatureVisible, creatureKind]);

  useEffect(() => {
    const mount = mountRef.current;
    if (!mount) return undefined;

    const scene = new THREE.Scene();
    const fog = new THREE.FogExp2(0x06151f, 0.038);
    scene.fog = fog;
    const camera = new THREE.PerspectiveCamera(46, window.innerWidth / window.innerHeight, 0.1, 100);
    camera.position.set(0, 0.4, 10.5);

    const probe = document.createElement("canvas");
    const webglAvailable = Boolean(probe.getContext("webgl2") || probe.getContext("webgl"));
    if (!webglAvailable) return undefined;

    let renderer: THREE.WebGLRenderer;
    try {
      renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: "high-performance" });
    } catch (error) {
      console.warn("Forest Depths 3D layer unavailable; using image fallback.", error);
      return undefined;
    }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.7));
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 0.72;
    renderer.domElement.className = "forest-three-canvas";
    renderer.domElement.setAttribute("aria-hidden", "true");
    mount.appendChild(renderer.domElement);

    const world = new THREE.Group();
    scene.add(world);

    const plateGroup = new THREE.Group();
    const plateGeometry = new THREE.PlaneGeometry(32, 18, 1, 1);
    const plateMaterials = PLATE_URLS.map(() => new THREE.MeshBasicMaterial({ color: 0x8fb9bb, transparent: true, opacity: 0, depthWrite: false, side: THREE.DoubleSide }));
    const plateMeshes = plateMaterials.map((material, index) => {
      const plate = new THREE.Mesh(plateGeometry, material);
      plate.position.set(0, 0.2 - index * 0.08, -4.2 - index * 2.65);
      plate.scale.setScalar(1 + index * 0.018);
      plate.userData.baseX = (index % 2 ? -1 : 1) * (0.12 + index * 0.018);
      plateGroup.add(plate);
      return plate;
    });
    world.add(plateGroup);
    const plateLoader = new THREE.TextureLoader();
    PLATE_URLS.forEach((url, index) => {
      plateLoader.load(url, (texture) => {
        texture.colorSpace = THREE.SRGBColorSpace;
        texture.minFilter = THREE.LinearFilter;
        plateMaterials[index].map = texture;
        plateMaterials[index].needsUpdate = true;
      }, undefined, () => {
        plateMaterials[index].color.setHex(index < 4 ? 0x0b3d49 : index < 6 ? 0x112846 : 0x080f20);
      });
    });

    const objectGroup = new THREE.Group();
    objectGroup.name = "png-object-depth-layers";
    const objectSpecs = [
      { key: "trees", width: 4.6, height: 8.2, x: -4.8, y: -0.8, z: -2.2, stage: 0.08, opacity: 0.82 },
      { key: "rocks", width: 5.6, height: 3.8, x: 3.7, y: -3.1, z: -5.8, stage: 0.34, opacity: 0.9 },
      { key: "fern", width: 3.8, height: 5.6, x: 4.5, y: -1.1, z: -9.8, stage: 0.56, opacity: 0.78 },
      { key: "mushroom", width: 2.6, height: 3.5, x: -3.8, y: -1.2, z: -14.5, stage: 0.78, opacity: 0.82 },
    ];
    const pngObjects = objectSpecs.map((spec) => {
      const material = new THREE.MeshBasicMaterial({ color: 0xb7f5e8, transparent: true, opacity: 0, depthWrite: false, side: THREE.DoubleSide });
      const mesh = new THREE.Mesh(new THREE.PlaneGeometry(spec.width, spec.height), material);
      mesh.name = `png-${spec.key}`;
      mesh.position.set(spec.x, spec.y, spec.z);
      mesh.userData.baseX = spec.x;
      mesh.userData.baseY = spec.y;
      mesh.userData.baseZ = spec.z;
      mesh.userData.stage = spec.stage;
      mesh.userData.baseOpacity = spec.opacity;
      mesh.userData.loaded = false;
      objectGroup.add(mesh);
      new THREE.TextureLoader().load(OBJECT_PNG_URLS[spec.key as keyof typeof OBJECT_PNG_URLS], (texture) => {
        texture.colorSpace = THREE.SRGBColorSpace;
        texture.minFilter = THREE.LinearFilter;
        texture.magFilter = THREE.LinearFilter;
        texture.generateMipmaps = false;
        material.map = texture;
        mesh.userData.loaded = true;
        material.needsUpdate = true;
      }, undefined, () => {
        material.color.setHex(0x17464a);
        material.opacity = 0;
      });
      return mesh;
    });
    world.add(objectGroup);

    const ambient = new THREE.HemisphereLight(0x8ddbe0, 0x02070b, 1.15);
    scene.add(ambient);
    const shaft = new THREE.DirectionalLight(0xc7fff4, 2.2);
    shaft.position.set(-5, 9, 3);
    scene.add(shaft);

    const forestFloor = new THREE.Mesh(new THREE.PlaneGeometry(46, 46, 18, 18), new THREE.MeshStandardMaterial({ color: 0x162319, roughness: 1, metalness: 0 }));
    forestFloor.rotation.x = -Math.PI / 2;
    forestFloor.position.y = -4.05;
    forestFloor.position.z = -4;
    world.add(forestFloor);

    const canopy = new THREE.Group();
    [-7.2, -5.4, 4.7, 6.2, 8.1].forEach((x, index) => canopy.add(makeTrunk(x, -4.8 - index * 1.4, 8.2 + (index % 3) * 1.4, 0.34 + (index % 2) * 0.12, index % 2 ? 0x2a3c28 : 0x1d3022)));
    world.add(canopy);

    const dust = makeParticleField(620, new THREE.Vector3(22, 12, 24), 0xb8c9a8, 0.022);
    const pollen = makeParticleField(220, new THREE.Vector3(18, 10, 18), 0xd7a45a, 0.038);
    const leafFall = makeParticleField(150, new THREE.Vector3(18, 14, 20), 0x9fb77e, 0.045);
    const mistParticles = makeParticleField(90, new THREE.Vector3(24, 7, 18), 0x9bb2a3, 0.065);
    (leafFall.material as THREE.PointsMaterial).opacity = 0.34;
    (mistParticles.material as THREE.PointsMaterial).opacity = 0.16;
    world.add(dust, pollen, leafFall, mistParticles);

    const fogCurtains = new THREE.Group();
    [-8, -14, -20].forEach((z, index) => {
      const material = new THREE.MeshBasicMaterial({ color: 0x9bb2a3, transparent: true, opacity: 0.035 + index * 0.012, depthWrite: false, side: THREE.DoubleSide, blending: THREE.NormalBlending });
      const curtain = new THREE.Mesh(new THREE.PlaneGeometry(28, 8), material);
      curtain.position.set(index % 2 ? -1.4 : 1.2, -0.5 + index * 0.18, z);
      curtain.userData.baseX = curtain.position.x;
      curtain.userData.baseY = curtain.position.y;
      curtain.userData.phase = index * 1.7;
      fogCurtains.add(curtain);
    });
    world.add(fogCurtains);

    const fireflies = makeFireflyField(18);
    world.add(fireflies);

    const forestFrames = new THREE.Group();
    [-3, -6.5, -10, -14, -18, -22, -26].forEach((z, index) => forestFrames.add(makeDepthRing(z, 1 + index * 0.22, index % 2 ? 0x64785a : 0xd7a45a)));
    world.add(forestFrames);

    const hiddenGlow = new THREE.Group();
    [-2.2, 2.6, 0.8].forEach((x, index) => {
      const glow = new THREE.Mesh(new THREE.SphereGeometry(0.08, 10, 10), new THREE.MeshBasicMaterial({ color: 0xd7a45a, transparent: true, opacity: 0.72, blending: THREE.AdditiveBlending }));
      glow.position.set(x, -0.2 + index * 0.42, -6 - index * 2.2);
      hiddenGlow.add(glow);
    });
    world.add(hiddenGlow);

    const lightOrb = new THREE.Mesh(new THREE.SphereGeometry(0.08, 12, 12), new THREE.MeshBasicMaterial({ color: 0xd7a45a, transparent: true, opacity: 0.8 }));
    const orbLight = new THREE.PointLight(0xd7a45a, 2.8, 7);
    lightOrb.add(orbLight);
    lightOrb.position.set(1.7, -0.6, -3.2);
    world.add(lightOrb);

    let raf = 0;
    let smoothProgress = progressRef.current;
    let creatureReveal = 0;
    const timer = new THREE.Timer();
    const render = () => {
      timer.update();
      const elapsed = timer.getElapsed();
      const target = progressRef.current;
      smoothProgress += (target - smoothProgress) * (reducedMotionRef.current ? 1 : 0.055);
      const depth = smoothProgress;
      const chapterPosition = depth * Math.max(1, stageCountRef.current - 1);
      const chapterIndex = Math.min(CAMERA_PATHS.length - 2, Math.floor(chapterPosition));
      const chapterLocal = chapterPosition - Math.floor(chapterPosition);
      const easedLocal = chapterLocal * chapterLocal * (3 - 2 * chapterLocal);
      const transitionPulse = Math.sin(chapterLocal * Math.PI);
      const path = mixPath(CAMERA_PATHS[chapterIndex], CAMERA_PATHS[chapterIndex + 1], easedLocal);
      const motionScale = reducedMotionRef.current ? 0 : 1;
      const orbit = elapsed * (0.12 + chapterIndex * 0.012);
      plateMeshes.forEach((plate, index) => {
        const distance = Math.abs(chapterPosition - index);
        const visibility = Math.max(0, 1 - distance);
        const material = plate.material as THREE.MeshBasicMaterial;
        material.opacity = visibility * (0.74 + depth * 0.18);
        plate.position.x = (plate.userData.baseX as number) + (index - chapterPosition) * 0.16 + Math.sin(orbit + index) * 0.06 * motionScale;
        plate.position.y = 0.2 - index * 0.08 + Math.sin(orbit * 0.5 + index) * 0.05 * motionScale;
        plate.position.z = -4.2 - index * 2.65 + transitionPulse * (index % 2 ? 0.3 : -0.18);
      });
      pngObjects.forEach((object) => {
        const stage = object.userData.stage as number;
        const distance = Math.abs(depth - stage);
        const reveal = Math.max(0, 1 - distance * 3.4);
        const material = object.material as THREE.MeshBasicMaterial;
        material.opacity = object.userData.loaded ? reveal * (object.userData.baseOpacity as number) * (reducedMotionRef.current ? 0.88 : 1) : 0;
        object.position.x = (object.userData.baseX as number) + (stage - depth) * 2.2 + Math.sin(orbit * 0.7 + stage * 8) * 0.16 * motionScale;
        object.position.y = (object.userData.baseY as number) + Math.cos(orbit * 0.52 + stage * 7) * 0.12 * motionScale - depth * 0.18;
        object.position.z = (object.userData.baseZ as number) + (stage - depth) * 5.2;
        object.rotation.z = Math.sin(orbit * 0.42 + stage * 6) * 0.025 * motionScale;
      });
      camera.position.x = path.x + Math.sin(orbit) * path.swayX * motionScale + Math.sin(depth * Math.PI * 1.8) * 0.26;
      camera.position.y = 0.7 - depth * 1.5 + path.y + Math.sin(orbit * 0.8) * path.swayY * motionScale;
      camera.position.z = 10.5 - depth * 8.4 + path.z + transitionPulse * 0.5;
      camera.fov = path.fov + transitionPulse * 2.2;
      camera.updateProjectionMatrix();
      camera.rotation.z = path.roll + Math.sin(orbit * 0.7) * 0.012 * motionScale;
      camera.lookAt(Math.sin(depth * 3.6) * 0.35 + path.lookX, -0.4 - depth * 1.8 + path.lookY, -4.8 - depth * 10 + path.lookZ);
      fog.density = 0.025 + depth * 0.065 + transitionPulse * 0.018;
      ambient.intensity = 1.25 - depth * 0.72;
      shaft.intensity = 2.2 - depth * 1.8;
      world.position.y = -depth * 2.1 - transitionPulse * 0.3;
      world.position.z = transitionPulse * 0.8;
      forestFloor.position.y = -4.05 - depth * 0.38;
      forestFloor.position.z = -4 + depth * 3.2;
      forestFloor.rotation.z = Math.sin(orbit * 0.14) * 0.012;
      world.rotation.y = Math.sin(elapsed * 0.1) * 0.055;
      forestFrames.position.z = depth * 3.8;
      forestFrames.rotation.z = Math.sin(elapsed * 0.08) * 0.04 + depth * 0.14 + transitionPulse * 0.12;
      canopy.position.x = Math.sin(elapsed * 0.18) * 0.18 + depth * 0.35;
      canopy.position.z = depth * 2.6;
      dust.rotation.y = elapsed * 0.008;
      dust.rotation.x = Math.sin(elapsed * 0.15) * 0.04;
      pollen.rotation.y = -elapsed * 0.004;
      const weatherMotion = reducedMotionRef.current ? 0 : 1;
      leafFall.rotation.z = Math.sin(elapsed * 0.12) * 0.035 * weatherMotion;
      leafFall.rotation.y = elapsed * 0.01 * weatherMotion;
      mistParticles.rotation.y = -elapsed * 0.004 * weatherMotion;
      mistParticles.position.x = Math.sin(elapsed * 0.16) * 0.55 * weatherMotion;
      mistParticles.position.y = Math.cos(elapsed * 0.12) * 0.18 * weatherMotion;
      const fogIntensity = Math.max(0, Math.min(1, (depth - 0.22) * 1.35)) + transitionPulse * 0.16;
      (leafFall.material as THREE.PointsMaterial).opacity = (0.08 + Math.max(0, 0.28 - depth * 0.12)) * (reducedMotionRef.current ? 0.55 : 1);
      (mistParticles.material as THREE.PointsMaterial).opacity = (0.035 + fogIntensity * 0.15) * (reducedMotionRef.current ? 0.45 : 1);
      fogCurtains.children.forEach((curtain: THREE.Object3D) => {
        const material = (curtain as THREE.Mesh).material as THREE.MeshBasicMaterial;
        const phase = curtain.userData.phase as number;
        material.opacity = (0.018 + fogIntensity * 0.052) * (reducedMotionRef.current ? 0.58 : 1);
        curtain.position.x = (curtain.userData.baseX as number) + Math.sin(elapsed * 0.11 + phase) * 0.42 * weatherMotion;
        curtain.position.y = (curtain.userData.baseY as number) + Math.cos(elapsed * 0.09 + phase) * 0.12 * weatherMotion;
      });
      fireflies.children.forEach((light: THREE.Object3D) => {
        const phase = light.userData.phase as number;
        light.position.y += Math.sin(elapsed * 1.2 + phase) * 0.0008;
        const material = (light as THREE.Mesh).material as THREE.MeshBasicMaterial;
        material.opacity = 0.26 + (Math.sin(elapsed * 2.1 + phase) + 1) * 0.22;
      });
      const creatureTarget = creatureVisibleRef.current && !reducedMotionRef.current ? 1 : 0;
      creatureReveal += (creatureTarget - creatureReveal) * 0.035;
      hiddenGlow.visible = creatureReveal > 0.01;
      hiddenGlow.scale.setScalar(0.72 + creatureReveal * 0.28);
      hiddenGlow.position.x = creatureKindRef.current === "owl" ? -1.2 : creatureKindRef.current === "deer" ? 1.1 : 0;
      hiddenGlow.position.y = Math.sin(elapsed * 0.4) * 0.12 - (1 - creatureReveal) * 0.3;
      lightOrb.position.y = -0.6 + Math.sin(elapsed * 0.7) * 0.35 - depth * 0.6;
      pollen.position.y = Math.sin(elapsed * 0.3) * 0.08;
      renderer.render(scene, camera);
      raf = requestAnimationFrame(render);
    };
    render();

    const resize = () => {
      const width = window.innerWidth;
      const height = window.innerHeight;
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, width < 720 ? 1.2 : 1.7));
      renderer.setSize(width, height);
    };
    window.addEventListener("resize", resize);

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", resize);
      renderer.dispose();
      scene.traverse((object) => {
        const mesh = object as THREE.Mesh;
        if (mesh.geometry) mesh.geometry.dispose();
        if (Array.isArray(mesh.material)) mesh.material.forEach((material) => material.dispose());
        else if (mesh.material) mesh.material.dispose();
      });
      renderer.domElement.remove();
    };
  }, []);

  return <div ref={mountRef} className="three-layer" aria-hidden="true" />;
}
