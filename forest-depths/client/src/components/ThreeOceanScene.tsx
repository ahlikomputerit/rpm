/* Ocean Depths / Pelagic Editorial: live 3D environmental layer, restrained motion, abyssal mint signal, scroll as camera choreography. */
import { useEffect, useRef } from "react";
import * as THREE from "three";

type ThreeOceanSceneProps = {
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

function makeKelp(x: number, z: number, height: number, color: number) {
  const group = new THREE.Group();
  const stem = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.08, height, 6), new THREE.MeshStandardMaterial({ color, roughness: 0.9, metalness: 0 }));
  stem.position.y = height / 2 - 3.7;
  group.add(stem);
  for (let i = 0; i < 4; i += 1) {
    const leaf = new THREE.Mesh(new THREE.SphereGeometry(0.2 + i * 0.035, 8, 5), new THREE.MeshStandardMaterial({ color: 0x104e50, roughness: 0.85 }));
    leaf.scale.set(0.35, 1.7, 0.12);
    leaf.position.set(Math.sin(i * 2.5) * 0.25, -2.8 + i * 0.72, Math.cos(i * 1.8) * 0.12);
    leaf.rotation.z = -0.35 + i * 0.18;
    group.add(leaf);
  }
  group.position.set(x, 0, z);
  return group;
}

type CameraPath = { x: number; y: number; z: number; lookX: number; lookY: number; lookZ: number; roll: number; fov: number; swayX: number; swayY: number };

const OBJECT_PNG_URLS = {
  kelp: "/manus-storage/ocean-depths-kelp-cutout-optimized_1fc68b2d.png",
  rocks: "/manus-storage/ocean-depths-trench-rocks-cutout-optimized_8837124f.png",
  coral: "/manus-storage/ocean-depths-coral-cutout-optimized_95c64522.png",
  jellyfish: "/manus-storage/ocean-depths-jellyfish-cutout-optimized_72882669.png",
};

const PLATE_URLS = [
  "/manus-storage/ocean-depths-surface-reference_44f6545e.jpg",
  "/manus-storage/ocean-depths-surface-reference_44f6545e.jpg",
  "/manus-storage/ocean-depths-reef_0e451337.jpg",
  "/manus-storage/ocean-depths-reef_0e451337.jpg",
  "/manus-storage/ocean-depths-twilight_da8c3d62.jpg",
  "/manus-storage/ocean-depths-twilight_da8c3d62.jpg",
  "/manus-storage/ocean-depths-abyss_7dc2d248.jpg",
  "/manus-storage/ocean-depths-abyss_7dc2d248.jpg",
  "/manus-storage/ocean-depths-abyss_7dc2d248.jpg",
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

function makeFish(x: number, y: number, z: number, scale: number, color: number) {
  const group = new THREE.Group();
  const body = new THREE.Mesh(new THREE.SphereGeometry(0.18, 10, 6), new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.58 }));
  body.scale.set(1.8, 0.62, 0.52);
  const tail = new THREE.Mesh(new THREE.ConeGeometry(0.16, 0.34, 4), new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.48 }));
  tail.rotation.z = Math.PI / 2;
  tail.position.x = -0.34;
  group.add(body, tail);
  group.position.set(x, y, z);
  group.scale.setScalar(scale);
  return group;
}

function seededNoise(x: number, z: number, seed = 0) {
  return Math.sin(x * 1.73 + z * 0.91 + seed * 2.37) * 0.5 + Math.sin(x * 0.47 - z * 1.31 + seed) * 0.3 + Math.sin(x * 3.9 + z * 2.2) * 0.2;
}

function makeProceduralTrench() {
  const group = new THREE.Group();
  group.name = "procedural-trench";

  const floorGeometry = new THREE.PlaneGeometry(34, 34, 34, 34);
  floorGeometry.rotateX(-Math.PI / 2);
  const floorPositions = floorGeometry.attributes.position;
  for (let i = 0; i < floorPositions.count; i += 1) {
    const x = floorPositions.getX(i);
    const z = floorPositions.getZ(i);
    const ridge = Math.abs(Math.sin(x * 0.75 + z * 0.14)) * 0.55;
    const basin = Math.sin(z * 0.38) * 0.34;
    floorPositions.setY(i, -4.35 + seededNoise(x, z, 4) * 0.22 + ridge * 0.2 + basin);
  }
  floorGeometry.computeVertexNormals();
  const floorMaterial = new THREE.MeshStandardMaterial({ color: 0x101b1d, roughness: 1, metalness: 0, flatShading: true });
  const floor = new THREE.Mesh(floorGeometry, floorMaterial);
  floor.name = "sediment-floor";
  floor.position.set(0, 0, -10);
  group.add(floor);

  [-1, 1].forEach((side) => {
    const wallGeometry = new THREE.PlaneGeometry(8, 22, 18, 34);
    wallGeometry.rotateY(side * Math.PI / 2);
    const wallPositions = wallGeometry.attributes.position;
    for (let i = 0; i < wallPositions.count; i += 1) {
      const localY = wallPositions.getY(i);
      const localZ = wallPositions.getZ(i);
      const depthLift = Math.max(0, (-localZ - 3) / 14);
      wallPositions.setX(i, side * (4.5 + seededNoise(localY, localZ, side) * 0.35 + depthLift * 1.1));
    }
    wallGeometry.computeVertexNormals();
    const wallMaterial = new THREE.MeshStandardMaterial({ color: side < 0 ? 0x172b2d : 0x0d2026, roughness: 0.96, metalness: 0, side: THREE.DoubleSide, flatShading: true });
    const wall = new THREE.Mesh(wallGeometry, wallMaterial);
    wall.name = side < 0 ? "trench-wall-left" : "trench-wall-right";
    wall.position.set(0, -0.1, -11);
    group.add(wall);
  });

  const ridge = new THREE.Group();
  ridge.name = "rock-ridge";
  for (let i = 0; i < 18; i += 1) {
    const rock = new THREE.Mesh(new THREE.DodecahedronGeometry(0.22 + (i % 4) * 0.12, 0), new THREE.MeshStandardMaterial({ color: i % 2 ? 0x253b3a : 0x1a2c30, roughness: 1, flatShading: true }));
    rock.position.set((i % 2 ? -1 : 1) * (3.2 + (i % 5) * 0.62), -3.65 + (i % 3) * 0.12, -4.5 - i * 1.05);
    rock.scale.y = 1.2 + (i % 3) * 0.55;
    rock.rotation.set(i * 0.34, i * 0.52, i * 0.2);
    ridge.add(rock);
  }
  group.add(ridge);

  const fissures = new THREE.Group();
  fissures.name = "bioluminescent-fissures";
  for (let i = 0; i < 7; i += 1) {
    const points = [
      new THREE.Vector3(-2.8 + i * 0.9, -4.05, -5 - i * 2.3),
      new THREE.Vector3(-2.1 + i * 0.9, -4.01, -5.65 - i * 2.3),
      new THREE.Vector3(-1.4 + i * 0.9, -4.04, -6.1 - i * 2.3),
    ];
    const line = new THREE.Line(new THREE.BufferGeometry().setFromPoints(points), new THREE.LineBasicMaterial({ color: 0x63cdbb, transparent: true, opacity: 0.28, blending: THREE.AdditiveBlending }));
    fissures.add(line);
  }
  group.add(fissures);
  return group;
}

function makeDepthRing(z: number, scale: number, color: number) {
  const material = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.14, side: THREE.DoubleSide, blending: THREE.AdditiveBlending });
  const ring = new THREE.Mesh(new THREE.RingGeometry(2.3, 2.34, 64), material);
  ring.position.z = z;
  ring.scale.setScalar(scale);
  ring.rotation.x = Math.PI / 2.2;
  return ring;
}

function makeJellyfish(x: number, y: number, z: number) {
  const group = new THREE.Group();
  const dome = new THREE.Mesh(new THREE.SphereGeometry(0.33, 16, 10, 0, Math.PI * 2, 0, Math.PI / 2), new THREE.MeshBasicMaterial({ color: 0x8de8d2, transparent: true, opacity: 0.32, blending: THREE.AdditiveBlending }));
  dome.scale.y = 0.62;
  group.add(dome);
  for (let i = 0; i < 5; i += 1) {
    const tentacle = new THREE.Mesh(new THREE.CylinderGeometry(0.008, 0.02, 0.55 + Math.random() * 0.4, 5), new THREE.MeshBasicMaterial({ color: 0x8de8d2, transparent: true, opacity: 0.36, blending: THREE.AdditiveBlending }));
    tentacle.position.set((i - 2) * 0.11, -0.35, 0);
    tentacle.rotation.z = (i - 2) * 0.16;
    group.add(tentacle);
  }
  group.position.set(x, y, z);
  return group;
}

export default function ThreeOceanScene({ progress, stageCount = 9, reducedMotion, creatureVisible = false, creatureKind = "jellyfish" }: ThreeOceanSceneProps) {
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
      console.warn("Ocean Depths 3D layer unavailable; using image fallback.", error);
      return undefined;
    }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.7));
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 0.72;
    renderer.domElement.className = "ocean-three-canvas";
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
      { key: "kelp", width: 3.8, height: 7.2, x: -4.8, y: -1.5, z: -2.2, stage: 0.08, opacity: 0.82 },
      { key: "rocks", width: 5.6, height: 3.8, x: 3.7, y: -3.1, z: -5.8, stage: 0.34, opacity: 0.9 },
      { key: "coral", width: 3.5, height: 5.6, x: 4.5, y: -1.1, z: -9.8, stage: 0.56, opacity: 0.78 },
      { key: "jellyfish", width: 2.6, height: 4.5, x: -3.8, y: 0.5, z: -14.5, stage: 0.78, opacity: 0.82 },
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

    const seabed = new THREE.Mesh(new THREE.PlaneGeometry(46, 46, 16, 16), new THREE.MeshStandardMaterial({ color: 0x07161b, roughness: 1, metalness: 0 }));
    seabed.rotation.x = -Math.PI / 2;
    seabed.position.y = -4.05;
    seabed.position.z = -4;
    world.add(seabed);

    const trench = makeProceduralTrench();
    world.add(trench);

    const kelp = new THREE.Group();
    [-7, -5.8, -4.4, 4.7, 5.5, 7.2].forEach((x, index) => kelp.add(makeKelp(x, -5 + index * 0.4, 2.8 + (index % 3) * 0.9, index % 2 ? 0x0d3f43 : 0x0b353b)));
    world.add(kelp);

    const plankton = makeParticleField(700, new THREE.Vector3(22, 12, 24), 0xb4f6e0, 0.028);
    const marineSnow = makeParticleField(320, new THREE.Vector3(18, 11, 18), 0x86bfc4, 0.045);
    world.add(plankton, marineSnow);

    const bubbles = new THREE.Group();
    for (let i = 0; i < 18; i += 1) {
      const bubble = new THREE.Mesh(new THREE.SphereGeometry(0.035 + Math.random() * 0.06, 8, 8), new THREE.MeshBasicMaterial({ color: 0x8de8d2, transparent: true, opacity: 0.34, wireframe: true }));
      bubble.position.set((Math.random() - 0.5) * 12, -3.8 + Math.random() * 6, (Math.random() - 0.5) * 7);
      bubble.userData.speed = 0.0012 + Math.random() * 0.0026;
      bubbles.add(bubble);
    }
    world.add(bubbles);

    const jellies = new THREE.Group();
    jellies.add(makeJellyfish(-3.8, 0.8, -3.5), makeJellyfish(3.2, -0.8, -4.4), makeJellyfish(0.8, 1.2, -7));
    world.add(jellies);

    const depthTunnel = new THREE.Group();
    [-3, -6.5, -10, -14, -18, -22, -26].forEach((z, index) => depthTunnel.add(makeDepthRing(z, 1 + index * 0.22, index % 2 ? 0x4ea7b1 : 0x8de8d2)));
    world.add(depthTunnel);

    const fishSchool = new THREE.Group();
    fishSchool.add(
      makeFish(-3.6, 1.2, -5.5, 0.72, 0xb4f6e0),
      makeFish(3.8, 0.3, -8.5, 0.48, 0x75d3d0),
      makeFish(-2.6, -1.3, -13, 0.55, 0x9be5d5),
      makeFish(4.6, -1.8, -17, 0.9, 0x4f9caf),
    );
    fishSchool.visible = false;
    world.add(fishSchool);

    const lightOrb = new THREE.Mesh(new THREE.SphereGeometry(0.08, 12, 12), new THREE.MeshBasicMaterial({ color: 0x8de8d2, transparent: true, opacity: 0.8 }));
    const orbLight = new THREE.PointLight(0x8de8d2, 3.2, 7);
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
      trench.position.y = -depth * 0.38;
      trench.position.z = depth * 3.2;
      trench.rotation.y = Math.sin(orbit * 0.22) * 0.035 + transitionPulse * 0.025;
      trench.rotation.x = Math.sin(orbit * 0.14) * 0.012;
      world.rotation.y = Math.sin(elapsed * 0.1) * 0.055;
      depthTunnel.position.z = depth * 3.8;
      depthTunnel.rotation.z = Math.sin(elapsed * 0.08) * 0.04 + depth * 0.32 + transitionPulse * 0.22;
      fishSchool.visible = depth > 0.12 && depth < 0.92;
      fishSchool.position.x = Math.sin(elapsed * 0.18) * 0.4 + depth * 0.8;
      fishSchool.position.z = depth * 4.5;
      fishSchool.rotation.y = Math.sin(elapsed * 0.2) * 0.08;
      plankton.rotation.y = elapsed * 0.008;
      plankton.rotation.x = Math.sin(elapsed * 0.15) * 0.04;
      marineSnow.rotation.y = -elapsed * 0.004;
      kelp.rotation.z = Math.sin(elapsed * 0.55) * 0.018;
      const creatureTarget = creatureVisibleRef.current && !reducedMotionRef.current ? 1 : 0;
      creatureReveal += (creatureTarget - creatureReveal) * 0.035;
      jellies.visible = creatureReveal > 0.01;
      jellies.scale.setScalar(0.72 + creatureReveal * 0.28);
      jellies.position.x = creatureKindRef.current === "manta" ? -1.2 : creatureKindRef.current === "turtle" ? 1.1 : 0;
      jellies.position.y = Math.sin(elapsed * 0.4) * 0.12 - (1 - creatureReveal) * 0.3;
      jellies.rotation.y = Math.sin(elapsed * 0.22) * 0.06;
      lightOrb.position.y = -0.6 + Math.sin(elapsed * 0.7) * 0.35 - depth * 1.2;
      bubbles.children.forEach((bubble) => {
        bubble.position.y += (bubble.userData.speed as number) * (reducedMotionRef.current ? 0 : 1);
        bubble.position.x += Math.sin(elapsed * 0.7 + bubble.id) * 0.0007;
        if (bubble.position.y > 3) bubble.position.y = -4;
      });
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
