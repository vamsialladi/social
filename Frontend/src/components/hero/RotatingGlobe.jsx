import { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import * as THREE from 'three';
import { WORLDWIDE_IMAGES } from './worldwide';

const IMAGE_MARKER_RADIUS = 0.055;
const MAX_CONCURRENT_MARKER_IMAGE_LOADS = 12;

function cropTextureToSquare(texture) {
  const { width, height } = texture.image;
  const aspectRatio = width / height;

  if (aspectRatio > 1) {
    texture.repeat.set(1 / aspectRatio, 1);
    texture.offset.set((1 - 1 / aspectRatio) / 2, 0);
  } else {
    texture.repeat.set(1, aspectRatio);
    texture.offset.set(0, (1 - aspectRatio) / 2);
  }
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.needsUpdate = true;
}

export default function RotatingGlobe({ size = 580, isRotating = true }) {
  const mountRef = useRef(null);
  const frameIdRef = useRef(null);
  const [selectedProfile, setSelectedProfile] = useState(null);
  const [isSelectedProfileImageLoaded, setIsSelectedProfileImageLoaded] = useState(false);

  useEffect(() => {
    const container = mountRef.current;
    if (!container) return;

    const width = size;
    const height = size;

    // 1. Scene & Camera Setup
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
    camera.position.z = 2.8;

    // 2. WebGL Renderer with Alpha transparency
    let renderer;
    try {
      renderer = new THREE.WebGLRenderer({
        antialias: true,
        alpha: true,
        powerPreference: 'high-performance'
      });
      renderer.setSize(width, height);
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
      container.appendChild(renderer.domElement);
    } catch (e) {
      console.warn('WebGL init failed, using fallback', e);
      return;
    }

    // 3. Earth surface and city-light maps
    const textureLoader = new THREE.TextureLoader();
    const earthTexture = textureLoader.load(
      `${import.meta.env.BASE_URL}assets/earth-atmos-2048.jpg`,
      undefined,
      undefined,
      (error) => console.error('Failed to load the Earth surface map.', error)
    );
    earthTexture.colorSpace = THREE.SRGBColorSpace;

    const nightLightsTexture = textureLoader.load(
      `${import.meta.env.BASE_URL}assets/earth-night-lights-2048.png`,
      undefined,
      undefined,
      (error) => console.error('Failed to load the Earth night-lights map.', error)
    );
    nightLightsTexture.colorSpace = THREE.SRGBColorSpace;

    // 4. Globe Sphere & Material
    const earthGroup = new THREE.Group();
    scene.add(earthGroup);

    const globeGeometry = new THREE.SphereGeometry(1, 64, 64);

    const globeMaterial = new THREE.MeshPhongMaterial({
      map: earthTexture,
      bumpMap: earthTexture,
      bumpScale: 0.018,
      shininess: 25,
      specular: new THREE.Color(0x4A6B8A)
    });

    const globeMesh = new THREE.Mesh(globeGeometry, globeMaterial);
    earthGroup.add(globeMesh);

    const markerGeometry = new THREE.CircleGeometry(IMAGE_MARKER_RADIUS, 48);
    const borderGeometry = new THREE.RingGeometry(IMAGE_MARKER_RADIUS - 0.002, IMAGE_MARKER_RADIUS + 0.002, 48);
    const borderMaterial = new THREE.MeshBasicMaterial({
      color: 0xF59E0B,
      side: THREE.DoubleSide
    });
    const markerMaterials = [];
    const markerMeshes = [];
    let isDisposed = false;
    const imageMarkers = WORLDWIDE_IMAGES;
    const markerImageQueue = [];
    let nextMarkerImage = 0;
    let activeMarkerImageLoads = 0;
    let completedMarkerImages = 0;
    let areMarkerImagesLoaded = false;
    const markerNormals = imageMarkers.map(({ latitude, longitude }) => {
      const lat = THREE.MathUtils.degToRad(latitude);
      const lon = THREE.MathUtils.degToRad(longitude);
      return new THREE.Vector3(
        Math.cos(lat) * Math.cos(lon),
        Math.sin(lat),
        -Math.cos(lat) * Math.sin(lon)
      );
    });

    imageMarkers.forEach(({ name, image }, index) => {
      const markerMaterial = new THREE.MeshBasicMaterial({
        color: 0x334155,
        side: THREE.DoubleSide,
        toneMapped: false
      });
      markerMaterials.push(markerMaterial);
      markerImageQueue.push({ name, image, index, markerMaterial });

      const markerGroup = new THREE.Group();

      const marker = new THREE.Mesh(markerGeometry, markerMaterial);
      const location = name.split(' — ').slice(1).join(' — ');
      marker.userData.profile = {
        name,
        image,
        location: location || 'Cinema professional'
      };
      markerMeshes.push(marker);
      markerGroup.add(marker);

      const border = new THREE.Mesh(borderGeometry, borderMaterial);
      border.position.z = 0.0008;
      markerGroup.add(border);

      const normal = markerNormals[index];
      const markerPosition = normal.clone().multiplyScalar(1.020);
      const markerScale = 1.0;

      markerGroup.scale.setScalar(markerScale);
      markerGroup.position.copy(markerPosition);
      markerGroup.quaternion.setFromUnitVectors(new THREE.Vector3(0, 0, 1), normal);
      markerGroup.renderOrder = 3;
      earthGroup.add(markerGroup);
    });

    areMarkerImagesLoaded = markerImageQueue.length === 0;

    const pumpMarkerImageQueue = () => {
      if (isDisposed) return;

      while (
        activeMarkerImageLoads < MAX_CONCURRENT_MARKER_IMAGE_LOADS
        && nextMarkerImage < markerImageQueue.length
      ) {
        const { name, image, index, markerMaterial } = markerImageQueue[nextMarkerImage];
        nextMarkerImage += 1;
        activeMarkerImageLoads += 1;

        const finishLoading = () => {
          activeMarkerImageLoads -= 1;
          completedMarkerImages += 1;
          areMarkerImagesLoaded = completedMarkerImages === markerImageQueue.length;
          pumpMarkerImageQueue();
        };
        const markerTexture = textureLoader.load(
          image,
          (loadedTexture) => {
            cropTextureToSquare(loadedTexture);
            if (isDisposed) {
              loadedTexture.dispose();
              finishLoading();
              return;
            }
            markerMaterial.map?.dispose();
            markerMaterial.map = loadedTexture;
            markerMaterial.color.set(0xffffff);
            markerMaterial.needsUpdate = true;
            finishLoading();
          },
          undefined,
          (error) => {
            console.error(`Failed to load globe marker for ${name}: ${image}`, error);
            if (isDisposed) {
              finishLoading();
              return;
            }

            const fallbackImage = FALLBACK_MARKER_IMAGES[index % FALLBACK_MARKER_IMAGES.length];
            textureLoader.load(
              fallbackImage,
              (fallbackTexture) => {
                cropTextureToSquare(fallbackTexture);
                if (isDisposed) {
                  fallbackTexture.dispose();
                  finishLoading();
                  return;
                }
                markerMaterial.map?.dispose();
                markerMaterial.map = fallbackTexture;
                markerMaterial.color.set(0xffffff);
                markerMaterial.needsUpdate = true;
                finishLoading();
              },
              undefined,
              (fallbackError) => {
                console.error(`Failed to load fallback globe marker for ${name}: ${fallbackImage}`, fallbackError);
                finishLoading();
              }
            );
          }
        );
        markerTexture.colorSpace = THREE.SRGBColorSpace;
      }
    };
    pumpMarkerImageQueue();

    const raycaster = new THREE.Raycaster();
    const pointer = new THREE.Vector2();
    const handleMarkerClick = (event) => {
      const bounds = renderer.domElement.getBoundingClientRect();
      pointer.x = ((event.clientX - bounds.left) / bounds.width) * 2 - 1;
      pointer.y = -((event.clientY - bounds.top) / bounds.height) * 2 + 1;
      raycaster.setFromCamera(pointer, camera);

      const markerHit = raycaster.intersectObjects(markerMeshes, false)[0];
      const globeHit = raycaster.intersectObject(globeMesh, false)[0];
      if (markerHit && (!globeHit || markerHit.distance < globeHit.distance)) {
        setSelectedProfile(markerHit.object.userData.profile);
        setIsSelectedProfileImageLoaded(false);
      } else {
        setSelectedProfile(null);
      }
    };
    renderer.domElement.addEventListener('click', handleMarkerClick);
    renderer.domElement.style.pointerEvents = 'auto';

    const nightLightsMaterial = new THREE.ShaderMaterial({
      uniforms: {
        uNightLights: { value: nightLightsTexture },
        uSunDirection: { value: new THREE.Vector3(5, 3, 5).normalize() }
      },
      vertexShader: `
        varying vec2 vUv;
        varying vec3 vWorldNormal;
        void main() {
          vUv = uv;
          vWorldNormal = normalize(mat3(modelMatrix) * normal);
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform sampler2D uNightLights;
        uniform vec3 uSunDirection;
        varying vec2 vUv;
        varying vec3 vWorldNormal;
        void main() {
          vec3 lights = texture2D(uNightLights, vUv).rgb;
          float brightness = dot(lights, vec3(0.299, 0.587, 0.114));
          float cityLights = smoothstep(0.16, 0.55, brightness);
          float nightSide = 1.0 - smoothstep(-0.15, 0.12, dot(normalize(vWorldNormal), uSunDirection));
          float glow = cityLights * nightSide;
          gl_FragColor = vec4(vec3(1.0, 0.62, 0.24) * brightness * 2.0, glow);
        }
      `,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      transparent: true
    });
    const nightLightsMesh = new THREE.Mesh(
      new THREE.SphereGeometry(1.003, 64, 64),
      nightLightsMaterial
    );
    earthGroup.add(nightLightsMesh);

    // 5. Atmospheric Outer Glow Shell (Luminous Blue & Warm Golden cinematic rim)
    const atmosphereGeometry = new THREE.SphereGeometry(1.045, 64, 64);
    const atmosphereMaterial = new THREE.ShaderMaterial({
      vertexShader: `
        varying vec3 vNormal;
        void main() {
          vNormal = normalize(normalMatrix * normal);
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        varying vec3 vNormal;
        void main() {
          float rim = pow(0.72 - dot(vNormal, vec3(0.0, 0.0, 1.0)), 2.0);
          vec3 blueGlow = vec3(0.14, 0.65, 1.0) * 1.8;
          vec3 warmRim = vec3(0.96, 0.62, 0.12) * 0.8;
          vec3 finalColor = mix(blueGlow, warmRim, clamp(vNormal.x * 0.5 + 0.5, 0.0, 1.0));
          gl_FragColor = vec4(finalColor, 1.0) * rim * 2.0;
        }
      `,
      blending: THREE.AdditiveBlending,
      side: THREE.BackSide,
      transparent: true
    });

    const atmosphereMesh = new THREE.Mesh(atmosphereGeometry, atmosphereMaterial);
    earthGroup.add(atmosphereMesh);

    // 6. Natural Sunlight, Golden Rim, Front Fill & Ambient Lighting
    const ambientLight = new THREE.AmbientLight(0x384B68, 1.4);
    scene.add(ambientLight);

    // Key Sunlight (Bright warm sunlight)
    const sunLight = new THREE.DirectionalLight(0xFFFBEB, 3.2);
    sunLight.position.set(5, 3.5, 4.5);
    scene.add(sunLight);

    // Front Camera Fill Light (Brings out vivid continent colors & portraits)
    const frontFillLight = new THREE.DirectionalLight(0xE0F2FE, 1.2);
    frontFillLight.position.set(0, 1.5, 5);
    scene.add(frontFillLight);

    // Golden Cinema Accent Light (Highlights edges and cinema talents)
    const goldenAccentLight = new THREE.DirectionalLight(0xF59E0B, 1.5);
    goldenAccentLight.position.set(-3.5, 2.5, 2);
    scene.add(goldenAccentLight);

    // Deep Cosmic Blue Rim Light (Backlighting)
    const blueBackLight = new THREE.DirectionalLight(0x0EA5E9, 1.4);
    blueBackLight.position.set(-5, -2.5, -4);
    scene.add(blueBackLight);

    // 7. Golden Cinematic Particle Nebula Dust around the Globe
    const particleCount = 180;
    const particleGeometry = new THREE.BufferGeometry();
    const particlePositions = new Float32Array(particleCount * 3);

    for (let i = 0; i < particleCount * 3; i += 3) {
      const radius = 1.35 + Math.random() * 0.9;
      const theta = Math.random() * Math.PI * 2;
      const phi = (Math.random() - 0.5) * Math.PI * 0.8;
      particlePositions[i] = radius * Math.cos(phi) * Math.sin(theta);
      particlePositions[i + 1] = radius * Math.sin(phi);
      particlePositions[i + 2] = radius * Math.cos(phi) * Math.cos(theta);
    }

    particleGeometry.setAttribute('position', new THREE.BufferAttribute(particlePositions, 3));
    const particleMaterial = new THREE.PointsMaterial({
      color: 0xF59E0B,
      size: 0.022,
      transparent: true,
      opacity: 0.7,
      blending: THREE.AdditiveBlending
    });

    const particles = new THREE.Points(particleGeometry, particleMaterial);
    earthGroup.add(particles);

    // Initial slight axial tilt (Earth's 23.5 degrees) and face India toward user on first load
    earthGroup.rotation.x = 0.22;
    earthGroup.rotation.y = 3.32; // ~190 degrees, lands directly facing India
    earthGroup.rotation.z = -0.15;

    // 8. 60 FPS Render Loop
    const animate = () => {
      frameIdRef.current = requestAnimationFrame(animate);

      // Continuous rotation around Y axis
      if (isRotating && areMarkerImagesLoaded && earthGroup) {
        earthGroup.rotation.y += 0.0075;
        particles.rotation.y += 0.001;
      }

      renderer.render(scene, camera);
    };

    animate();

    // 9. Cleanup on unmount
    return () => {
      isDisposed = true;
      renderer.domElement.removeEventListener('click', handleMarkerClick);
      if (frameIdRef.current) cancelAnimationFrame(frameIdRef.current);
      if (renderer) {
        renderer.dispose();
        if (renderer.domElement && container.contains(renderer.domElement)) {
          container.removeChild(renderer.domElement);
        }
      }
      globeGeometry.dispose();
      globeMaterial.dispose();
      nightLightsMesh.geometry.dispose();
      nightLightsMaterial.dispose();
      markerGeometry.dispose();
      borderGeometry.dispose();
      borderMaterial.dispose();
      markerMaterials.forEach((material) => {
        material.map?.dispose();
        material.dispose();
      });
      atmosphereGeometry.dispose();
      atmosphereMaterial.dispose();
      earthTexture.dispose();
      nightLightsTexture.dispose();
      particleGeometry.dispose();
      particleMaterial.dispose();
    };
  }, [size, isRotating]);

  return (
    <div className="globe-interaction-shell" style={{ width: `${size}px`, height: `${size}px` }}>
      <div
        ref={mountRef}
        className="rotating-globe-mount"
        style={{
          width: `${size}px`,
          height: `${size}px`,
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          filter: 'drop-shadow(0 0 45px rgba(14, 165, 233, 0.35))'
        }}
        aria-label="3D Rotating Earth Globe"
        role="img"
      />
      {selectedProfile && (
        <div className="globe-profile-card" role="dialog" aria-label={`${selectedProfile.name} profile`}>
          <button
            type="button"
            className="globe-profile-close"
            onClick={() => setSelectedProfile(null)}
            aria-label="Close profile card"
          >
            <X size={16} />
          </button>
          <img
            className={`globe-profile-image ${isSelectedProfileImageLoaded ? "loaded" : "hidden"}`}
            src={selectedProfile.image}
            alt={selectedProfile.name}
            loading="lazy"
            decoding="async"
            onLoad={() => setIsSelectedProfileImageLoaded(true)}
            onError={(event) => {
              event.currentTarget.onerror = null;
              event.currentTarget.src = FALLBACK_MARKER_IMAGES[0];
              setIsSelectedProfileImageLoaded(false);
            }}
          />
          <div className="globe-profile-label">Cinema talent</div>
          <h3>{selectedProfile.name}</h3>
          <p>{selectedProfile.location}</p>
        </div>
      )}
    </div>
  );
}
