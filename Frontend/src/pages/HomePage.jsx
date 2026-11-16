import { useState, useEffect } from 'react';
import CinematicHero from '../components/hero/CinematicHero';
import { PLATFORM_STATS } from '../services/mockData';
import { api } from '../services/api';

export default function HomePage() {
  const [stats, setStats] = useState(PLATFORM_STATS);

  useEffect(() => {
    async function loadData() {
  try {
    const fetchedStats = await api.stats.get();
    if (fetchedStats) setStats(fetchedStats);
  } catch {
    console.log('Using baseline demo data');
  }
    }
    loadData();
  }, []);

  return (
    <div className="landing-page">

      {/* 3D Cinematic Earth Globe & Rotating Film Strip Hero Banner */}
      <CinematicHero />
    </div>
  );
}
