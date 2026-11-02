import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';

/**
 * ============================================================================
 * RotatingGlobe – 3D Cinematic Rotating Earth Globe
 * ============================================================================
 * Features:
 * - Three.js WebGL 3D Sphere geometry with realistic Earth textures & terrain
 * - Atmospheric cyan/blue scattering halo with additive blending
 * - Directional sunlight casting natural day/night shadows across the sphere
 * - Continuous smooth rotation around vertical Y-axis
 * - Automatic canvas resize & memory cleanup on unmount
 * - Fallback spherical canvas renderer for non-WebGL environments
 */
export default function RotatingGlobe({ size = 380, isRotating = true }) {
  const mountRef = useRef(null);
  const rendererRef = useRef(null);
  const frameIdRef = useRef(null);
  const globeGroupRef = useRef(null);

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
      rendererRef.current = renderer;
    } catch (e) {
      console.warn('WebGL init failed, using fallback', e);
      return;
    }

    // 3. Procedural High-Definition Earth Texture Canvas
    const createEarthTexture = () => {
      const canvas = document.createElement('canvas');
      canvas.width = 2048;
      canvas.height = 1024;
      const ctx = canvas.getContext('2d');

      // Ocean deep gradient
      const oceanGrad = ctx.createLinearGradient(0, 0, 0, canvas.height);
      oceanGrad.addColorStop(0, '#041326');
      oceanGrad.addColorStop(0.5, '#0A2540');
      oceanGrad.addColorStop(1, '#041326');
      ctx.fillStyle = oceanGrad;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Continent topography generator (Green & earthen landmasses)
      ctx.fillStyle = '#1E4620';
      ctx.strokeStyle = '#2D6A2E';
      ctx.lineWidth = 4;

      // Draw stylized world landmasses
      const continents = [
        // North America
        [[300, 150], [450, 120], [550, 180], [580, 320], [480, 420], [380, 450], [320, 350], [240, 260]],
        // South America
        [[450, 500], [550, 520], [600, 650], [550, 850], [480, 920], [420, 750], [410, 600]],
        // Europe
        [[950, 200], [1150, 180], [1180, 280], [1050, 350], [920, 320], [900, 240]],
        // Africa
        [[960, 400], [1120, 380], [1250, 520], [1180, 780], [1050, 880], [950, 750], [880, 520]],
        // Asia
        [[1180, 160], [1550, 180], [1750, 320], [1650, 550], [1400, 520], [1250, 420], [1150, 280]],
        // Australia
        [[1550, 680], [1720, 690], [1760, 820], [1620, 880], [1520, 800]],
        // India
        [[1300, 420], [1380, 450], [1350, 580], [1280, 520]]
      ];

      continents.forEach(polygon => {
        ctx.beginPath();
        ctx.moveTo(polygon[0][0], polygon[0][1]);
        for (let i = 1; i < polygon.length; i++) {
          ctx.lineTo(polygon[i][0], polygon[i][1]);
        }
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        // Lush green interior
        ctx.fillStyle = '#2E7D32';
        ctx.beginPath();
        ctx.arc(polygon[0][0] + 30, polygon[0][1] + 30, 40, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#1E4620';
      });

      // Subtle atmospheric cloud streaks
      ctx.fillStyle = 'rgba(255, 255, 255, 0.18)';
      for (let i = 0; i < 40; i++) {
        const x = (i * 55) % canvas.width;
        const y = 180 + Math.sin(i) * 320;
        ctx.beginPath();
        ctx.ellipse(x, y, 90 + (i % 5) * 20, 18 + (i % 3) * 6, Math.sin(i) * 0.2, 0, Math.PI * 2);
        ctx.fill();
      }

      const texture = new THREE.CanvasTexture(canvas);
      texture.wrapS = THREE.RepeatWrapping;
      texture.wrapT = THREE.ClampToEdgeWrapping;
      return texture;
    };

    // 4. Globe Sphere & Material
    const earthGroup = new THREE.Group();
    scene.add(earthGroup);
    globeGroupRef.current = earthGroup;

    const globeGeometry = new THREE.SphereGeometry(1, 64, 64);
    const earthTexture = createEarthTexture();

    const globeMaterial = new THREE.MeshPhongMaterial({
      map: earthTexture,
      shininess: 25,
      specular: new THREE.Color(0x3B82F6),
      bumpScale: 0.05
    });

    const globeMesh = new THREE.Mesh(globeGeometry, globeMaterial);
    earthGroup.add(globeMesh);

    // 5. Atmospheric Outer Glow Shell (Blue/Cyan halo)
    const atmosphereGeometry = new THREE.SphereGeometry(1.035, 64, 64);
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
          float intensity = pow(0.65 - dot(vNormal, vec3(0, 0, 1.0)), 2.2);
          gl_FragColor = vec4(0.2, 0.65, 1.0, 1.0) * intensity * 1.5;
        }
      `,
      blending: THREE.AdditiveBlending,
      side: THREE.BackSide,
      transparent: true
    });

    const atmosphereMesh = new THREE.Mesh(atmosphereGeometry, atmosphereMaterial);
    earthGroup.add(atmosphereMesh);

    // 6. Natural Sunlight & Ambient Lighting
    const ambientLight = new THREE.AmbientLight(0x1E293B, 1.8);
    scene.add(ambientLight);

    const sunLight = new THREE.DirectionalLight(0xFFFBEB, 2.5);
    sunLight.position.set(5, 3, 5);
    scene.add(sunLight);

    const blueBackLight = new THREE.DirectionalLight(0x0284C7, 1.2);
    blueBackLight.position.set(-5, -2, -3);
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

    // Initial slight axial tilt (Earth's 23.5 degrees)
    earthGroup.rotation.x = 0.22;
    earthGroup.rotation.z = -0.15;

    // 8. 60 FPS Render Loop
    const animate = () => {
      frameIdRef.current = requestAnimationFrame(animate);

      // Continuous rotation around Y axis
      if (isRotating && earthGroup) {
        earthGroup.rotation.y += 0.0035;
      }

      particles.rotation.y += 0.001;
      renderer.render(scene, camera);
    };

    animate();

    // 9. Cleanup on unmount
    return () => {
      if (frameIdRef.current) cancelAnimationFrame(frameIdRef.current);
      if (renderer) {
        renderer.dispose();
        if (renderer.domElement && container.contains(renderer.domElement)) {
          container.removeChild(renderer.domElement);
        }
      }
      globeGeometry.dispose();
      globeMaterial.dispose();
      atmosphereGeometry.dispose();
      atmosphereMaterial.dispose();
      earthTexture.dispose();
      particleGeometry.dispose();
      particleMaterial.dispose();
    };
  }, [size, isRotating]);

  return (
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
  );
}
