import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Search, 
  Sparkles, 
  ArrowRight, 
  Users,
  Briefcase,
  TrendingUp,
  Smartphone,
  Film
} from 'lucide-react';
import TalentCard from '../components/cards/TalentCard';
import OpportunityCard from '../components/cards/OpportunityCard';
import CraftCard from '../components/cards/CraftCard';
import { 
  PLATFORM_STATS, 
  FILM_CRAFTS, 
  REGISTRATION_CRAFTS,
  FEATURED_TALENTS, 
  LATEST_OPPORTUNITIES 
} from '../services/mockData';
import { api } from '../services/api';

export default function HomePage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedQuickRole, setSelectedQuickRole] = useState('');
  const [talents, setTalents] = useState(FEATURED_TALENTS);
  const [jobs, setJobs] = useState(LATEST_OPPORTUNITIES);
  const [stats, setStats] = useState(PLATFORM_STATS);
  const [popularImageIndex, setPopularImageIndex] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    // Attempt async backend fetch with fallback to mock data
    async function loadData() {
      try {
        const fetchedStats = await api.stats.get();
        if (fetchedStats) setStats(fetchedStats);
        const fetchedTalents = await api.talents.getAll();
        if (fetchedTalents) setTalents(fetchedTalents);
        const fetchedJobs = await api.jobs.getAll();
        if (fetchedJobs) setJobs(fetchedJobs);
      } catch (e) {
        console.log('Using baseline demo data');
      }
    }
    loadData();
  }, []);

  useEffect(() => {
    const imageRotation = window.setInterval(() => {
      setPopularImageIndex((currentIndex) => (currentIndex + 1) % 4);
    }, 2000);

    return () => window.clearInterval(imageRotation);
  }, []);

  const craftCategories = {
    'Art Director': 'art-direction',
    'Assistant Director': 'direction',
    'Audiography (Sound Recording/Mixing)': 'sound-design',
    'Camera Operator': 'cinematography',
    'Cast (Actors)': 'acting',
    Choreographer: 'dance',
    'Cinematographer (DOP)': 'cinematography',
    'Costume Designer': 'makeup-costume',
    'Dialogue Writer': 'screenwriting',
    Director: 'direction',
    'Dubbing Artist': 'acting',
    Editor: 'editing',
    'Junior Artists & Agents': 'acting',
    'Makeup & Hair Stylist': 'makeup-costume',
    'Music Composer': 'music',
    'Outdoor Lightmen': 'cinematography',
    Producer: 'production',
    'Production Executive': 'production',
    'Publicity Designer': 'art-direction',
    'Screenplay Writer': 'screenwriting',
    'Still Photographer': 'cinematography',
    'Story Writer': 'screenwriting',
    'Stunt Director (Action)': 'acting',
    'Studio/Production Support Workers': 'production',
  };

  const popularRoleImages = {
    'Art Director': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Thota_Tharani.jpg/250px-Thota_Tharani.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Sabu_cyril_profile_picture.jpg/250px-Sabu_cyril_profile_picture.jpg',
    ],
    'Assistant Director': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/1/1e/Gautham_Menon_at_Dhuruvangal_Pathinaaru_Success_Meet_%28cropped%29.jpg/250px-Gautham_Menon_at_Dhuruvangal_Pathinaaru_Success_Meet_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4b/Teja.blue.green.JPG/250px-Teja.blue.green.JPG',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/7/74/Shekhar_Kammula.jpg/250px-Shekhar_Kammula.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/4/40/Boyapati_Srinu.jpg/250px-Boyapati_Srinu.jpg',
    ],
    'Audiography (Sound Recording/Mixing)': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/2/21/Resul_Pookutty_60th_Filmfare_Awards_%28cropped%29.jpg/250px-Resul_Pookutty_60th_Filmfare_Awards_%28cropped%29.jpg',
    ],
    'Camera Operator': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/7/76/Mani_cam.jpg/250px-Mani_cam.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/ff/Ravi_k._chandran.jpg/250px-Ravi_k._chandran.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0e/RD_Rajasekhar_isc_DOP.png/250px-RD_Rajasekhar_isc_DOP.png',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d5/Nirav_Shah_at_the_2.0_Trailer_Launch.jpg/250px-Nirav_Shah_at_the_2.0_Trailer_Launch.jpg',
    ],
    'Cast (Actors)': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/Allu_Arjun_at_Pushpa_2_The_Rule_meet.jpg/250px-Allu_Arjun_at_Pushpa_2_The_Rule_meet.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/NTR_Jr._%282026%29.jpg/250px-NTR_Jr._%282026%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9a/Mahesh_Babu_in_Spyder_%28cropped%29.jpg/250px-Mahesh_Babu_in_Spyder_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/2/22/Prabhas_by_Gage_Skidmore.jpg/250px-Prabhas_by_Gage_Skidmore.jpg',
    ],
    Choreographer: [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Farah_Khan_at_Lakme_Fashion_Week_2025.jpg/250px-Farah_Khan_at_Lakme_Fashion_Week_2025.jpg',
    ],
    'Cinematographer (DOP)': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/d/dc/Master_Class_on_Cinematography_by_K.K._Senthil_Kumar_%28Cinematographer%29%2C_at_the_46th_International_Film_Festival_of_India_%28IFFI-2015%29%2C_in_Panaji%2C_Goa_on_November_26%2C_2015.jpg/250px-thumbnail.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6f/Pc_sreeram_shooting.jpg/250px-Pc_sreeram_shooting.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Rathnavelu_dop.jpg/250px-Rathnavelu_dop.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9f/Santhosh_Shivan.jpg/250px-Santhosh_Shivan.jpg',
    ],
    'Costume Designer': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Neeta_Lulla_at_India_Resort_Fashion_Week_2012.jpg/250px-Neeta_Lulla_at_India_Resort_Fashion_Week_2012.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5d/Manish_Malhotra.jpg/250px-Manish_Malhotra.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2a/Bhanu_Athaiya_%28nee_Rajopadhye%29.jpg/250px-Bhanu_Athaiya_%28nee_Rajopadhye%29.jpg',
    ],
    'Dialogue Writer': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Trivikram-srinivas-director.jpg/250px-Trivikram-srinivas-director.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b8/Harish_Shankar.jpg/250px-Harish_Shankar.jpg',
      'https://upload.wikimedia.org/wikipedia/en/a/a2/Kona2.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5d/Kishore_Tirumala_at_Red_opening.png/250px-Kishore_Tirumala_at_Red_opening.png',
    ],
    Director: [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c1/SS_Rajamouli%2C_2021.jpg/250px-SS_Rajamouli%2C_2021.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/thumb/f/fe/Puri_Jagannadh_at_Ek_Niranjan_audio_release_event_%28cropped%29.jpg/250px-Puri_Jagannadh_at_Ek_Niranjan_audio_release_event_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/53/Koratala_Siva_%28cropped%29.jpg/250px-Koratala_Siva_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/54/Gunasekhar.jpg/250px-Gunasekhar.jpg',
    ],
    'Dubbing Artist': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Sunitha.jpg/250px-Sunitha.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/67/S._P._Balasubrahmanyam_at_the_%27Gurkha%27_Audio_Launch.jpg/250px-S._P._Balasubrahmanyam_at_the_%27Gurkha%27_Audio_Launch.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/de/S_Janaki_in_Pune%2C_India_2007.JPG/250px-S_Janaki_in_Pune%2C_India_2007.JPG',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f0/Melody_Queen_of_Indian_Cinema_Dr._K_S_Chithra_%28cropped%29.jpg/250px-Melody_Queen_of_Indian_Cinema_Dr._K_S_Chithra_%28cropped%29.jpg',
    ],
    Editor: [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Editor.B.Lenin_%28cropped%29.jpg/250px-Editor.B.Lenin_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c4/Sreekar_Prasad.png/250px-Sreekar_Prasad.png',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c5/Editor_Anthony_at_Oru_Naal_Iravil_Trailer_Launch.jpg/250px-Editor_Anthony_at_Oru_Naal_Iravil_Trailer_Launch.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/2/25/Kishore_TE.png/250px-Kishore_TE.png',
    ],
    'Junior Artists & Agents': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/The_Casting_Director_Mukesh_Chhabra_at_the_Master_Class_on_Characterisation_and_Casting_for_Cinema%2C_during_the_48th_International_Film_Festival_of_India_%28IFFI-2017%29%2C_in_Panaji%2C_Goa_on_November_25%2C_2017.jpg/250px-thumbnail.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9a/Nani_at_an_interview_for_film_companion_%28cropped%29.png/250px-Nani_at_an_interview_for_film_companion_%28cropped%29.png',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4b/Rashmika-Mandanna_at_the_music_launch_of_Chhaava_%28cropped%29.jpg/250px-Rashmika-Mandanna_at_the_music_launch_of_Chhaava_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b0/Rana_Daggubati_by_Gage_Skidmore.jpg/250px-Rana_Daggubati_by_Gage_Skidmore.jpg',
    ],
    'Makeup & Hair Stylist': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/Pattanam_Rasheed.jpg/250px-Pattanam_Rasheed.jpg',
    ],
    'Music Composer': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ee/Devi_sri_prasad.jpg/250px-Devi_sri_prasad.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/M._M._Keeravani_at_Inji_Iduppazhagi_Audio_Launch_%28cropped%29.jpg/250px-M._M._Keeravani_at_Inji_Iduppazhagi_Audio_Launch_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/9/99/Manisharma.jpg/250px-Manisharma.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/Mickeyjmeyer.jpg/250px-Mickeyjmeyer.jpg',
    ],
    'Outdoor Lightmen': [
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/3/39/Mahesh_Muthuswami.jpg/250px-Mahesh_Muthuswami.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/dc/Kumki_Latest_Unseen_Onlocation_Stills.jpg/250px-Kumki_Latest_Unseen_Onlocation_Stills.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/8/84/Vijay_Devarakonda_snapped_during_Liger_promotions.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/5/56/Anil_Ravipudi_at_71st_National_Film_Awards_%28cropped%29_%28cropped%29.jpg',
    ],
    Producer: [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/Dil_Raju_%28cropped%29.jpg/250px-Dil_Raju_%28cropped%29.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Daggubati_Suresh_Babu.jpg/250px-Daggubati_Suresh_Babu.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/8/86/SYM9482_%28cropped%29.jpg/250px-SYM9482_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6e/Chalasani_Aswani_Dutt_by_Gage_Skidmore.jpg/250px-Chalasani_Aswani_Dutt_by_Gage_Skidmore.jpg',
    ],
    'Production Executive': [
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Karan_Johar_at_2025_Cannes_Homebound_Photocall_2_%28cropped%29.jpg/250px-Karan_Johar_at_2025_Cannes_Homebound_Photocall_2_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f1/Kalanithi_Maran_%28cropped%29.jpg/250px-Kalanithi_Maran_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/9/93/Priyanka_Dutt_by_Gage_Skidmore.jpg/250px-Priyanka_Dutt_by_Gage_Skidmore.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4f/MS_Raju_by_Rudhra_Varma.jpg/250px-MS_Raju_by_Rudhra_Varma.jpg',
    ],
    'Publicity Designer': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/V_V_Rajendra_Kumar_01.jpg/250px-V_V_Rajendra_Kumar_01.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/0/02/Akkineni_Akhil.jpg/250px-Akkineni_Akhil.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/60/Tamannaah_Bhatia_at_a_song_launch_from_the_film_Vedaa_%28cropped%29.jpg/250px-Tamannaah_Bhatia_at_a_song_launch_from_the_film_Vedaa_%28cropped%29.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/2/24/Naga_Chaitanya_%28cropped%29.jpg/250px-Naga_Chaitanya_%28cropped%29.jpg',
    ],
    'Screenplay Writer': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/2/2f/V._Vijayendra_Prasad_announcing_1st_Jury_Choice_Award_in_Telangana_Avatharana_Filmotsavam_Award_ceremony_in_Ravindra_Bharathi_%2805.06.2018%29_02_%28cropped%29_%28cropped%29.jpg/250px-thumbnail.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Trivikram-srinivas-director.jpg/250px-Trivikram-srinivas-director.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b8/Harish_Shankar.jpg/250px-Harish_Shankar.jpg',
      'https://upload.wikimedia.org/wikipedia/en/a/a2/Kona2.jpg',
    ],
    'Still Photographer': [
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/Raghu_Rai_pix_by_Vikramjit_Kakati.jpg/250px-Raghu_Rai_pix_by_Vikramjit_Kakati.jpg',
    ],
    'Story Writer': [
      'https://upload.wikimedia.org/wikipedia/en/thumb/d/d6/Jandhyala_Subramanya_Sastry.jpg/250px-Jandhyala_Subramanya_Sastry.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Suddala_Ashokteja_05.jpg/250px-Suddala_Ashokteja_05.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/57/Anurag_Kashyap_02.jpg/250px-Anurag_Kashyap_02.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cd/RamGopalVarma.jpg/250px-RamGopalVarma.jpg',
    ],
    'Stunt Director (Action)': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/A_token_of_appreciation_presented_to_Peter_Hein_Offl_by_the_Deputy_Director%2C_Shri_Rizwan_Ahmed%2C_during_the_47th_International_Film_Festival_of_India_%28IFFI-2016%29%2C_in_Panaji%2C_Goa_on_November_24%2C_2016_%28cropped%29.jpg/250px-thumbnail.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ea/Tamil-movies-vijay-thalaiva-movie-on-location-stills08.jpg/250px-Tamil-movies-vijay-thalaiva-movie-on-location-stills08.jpg',
    ],
    'Studio/Production Support Workers': [
      'https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Nagarjuna_Akkineni_at_ANR_Awards.jpg/250px-Nagarjuna_Akkineni_at_ANR_Awards.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/0/02/Mohan_Babu_Manchu.jpg/250px-Mohan_Babu_Manchu.jpg',
      'https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a2/Venkatesh_Daggubati_at_ANR_Awards.jpg/250px-Venkatesh_Daggubati_at_ANR_Awards.jpg',
      'https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Shri_Konidela_Pawan_Kalyan.jpg/250px-Shri_Konidela_Pawan_Kalyan.jpg',
    ],
  };

  const popularRoles = REGISTRATION_CRAFTS.map((label) => ({
    label,
    category: craftCategories[label],
    image: popularRoleImages[label][popularImageIndex % popularRoleImages[label].length],
  }));

  const normalizedSearchTerm = searchTerm.trim().toLowerCase();
  const matchesSearch = (...values) => !normalizedSearchTerm
    || values.flat(Infinity)
      .filter((value) => typeof value === 'string' || typeof value === 'number')
      .join(' ')
      .toLowerCase()
      .includes(normalizedSearchTerm);

  const visibleTalents = talents.filter((talent) => matchesSearch(
    talent.name,
    talent.role,
    talent.category,
    talent.location,
    talent.experience,
    talent.bio,
    talent.skills,
    talent.credits?.map((credit) => [credit.title, credit.role, credit.format])
  ));
  const visibleJobs = jobs.filter((job) => matchesSearch(
    job.title,
    job.roleCategory,
    job.productionCompany,
    job.location,
    job.type,
    job.description,
    job.department,
    job.requirements
  ));
  const visibleCrafts = FILM_CRAFTS.filter((craft) => matchesSearch(craft.title, craft.desc));
  const visiblePopularRoles = popularRoles.filter((role) => matchesSearch(role.label));

  const handleHeroSearch = (e) => {
    e.preventDefault();
  };

  const handleQuickRoleClick = (roleObj) => {
    setSelectedQuickRole(roleObj.label);
    navigate(`/join-now?craft=${encodeURIComponent(roleObj.label)}`);
  };

  return (
    <div className="landing-page">

      <section className="hero-section">
        {/* Subtle cinematic gradient aura background */}
        <div className="hero-aura-bg" />

        <div className="container">
          <div className="hero-grid hero-grid-no-visual">
            
            {/* Hero Left Content */}
            <div className="hero-left-content">
              
              {/* Badge */}
              <div className="hero-badge">
                <Sparkles size={14} />
                <span>The Film & Entertainment Industry Network • truehunt.in</span>
              </div>

              {/* Main Headline */}
              <h1 className="hero-title">
                Connect. Grow. <br />
                <span className="hero-title-accent">Create the Next Story.</span>
              </h1>

              {/* Supporting Text */}
            
              {/* Talent Search Box */}
              <form onSubmit={handleHeroSearch} className="hero-search-form">
                <Search size={20} color="var(--color-text-muted)" style={{ marginRight: '10px', flexShrink: 0 }} />
                <input 
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search by talent name, professional role, skill, or opportunity..."
                  aria-label="Search by talent name, professional role, skill, or opportunity..."
                  className="hero-search-input"
                />
                <button 
                  type="submit"
                  className="hero-search-btn"
                >
                  <span>Search</span>
                  <ArrowRight size={16} />
                </button>
              </form>
              {normalizedSearchTerm && (
                <p className="home-search-status" role="status" aria-live="polite">
                  Showing {visibleTalents.length} talent matches, {visibleJobs.length} opportunity matches, and {visibleCrafts.length} craft matches on this page.
                </p>
              )}

              {/* Popular Professional Roles */}
              <div className="hero-roles-section">
                <span className="hero-roles-label">
                  POPULAR ROLES:
                </span>
                <div className="hero-roles-list">
                  {visiblePopularRoles.map((role) => (
                    <button
                      key={role.label}
                      type="button"
                      onClick={() => handleQuickRoleClick(role)}
                      className={`hero-role-pill ${selectedQuickRole === role.label ? 'active' : ''}`}
                    >
                      <img
                        className="hero-role-image"
                        src={role.image}
                        alt=""
                        aria-hidden="true"
                        onError={(event) => {
                          event.currentTarget.hidden = true;
                        }}
                      />
                      <span className="hero-role-name">{role.label}</span>
                    </button>
                  ))}
                </div>
                {normalizedSearchTerm && visiblePopularRoles.length === 0 && (
                  <p className="home-search-empty" role="status">No roles match “{searchTerm.trim()}”.</p>
                )}
              </div>

            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 2: PLATFORM STATISTICS
          ========================================================================= */}
      <section 
        style={{
          backgroundColor: 'var(--color-white)',
          paddingTop: '36px',
          paddingBottom: '36px',
          borderBottom: '1px solid var(--color-border)'
        }}
      >
        <div className="container">
          
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '8px' }}>
            <span className="badge-demo">Illustrative Demo Figures</span>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '24px'
            }}
          >
            {stats.map((stat) => (
              <div 
                key={stat.id}
                style={{
                  padding: '20px 24px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-bg-page)',
                  border: '1px solid var(--color-border)',
                  textAlign: 'center'
                }}
              >
                <div 
                  style={{
                    fontSize: '36px',
                    fontWeight: 800,
                    color: 'var(--color-navy)',
                    fontFamily: 'var(--font-display)',
                    lineHeight: '1.1',
                    marginBottom: '4px'
                  }}
                >
                  {stat.value}
                </div>
                <div style={{ fontSize: '15px', fontWeight: 600, color: 'var(--color-navy-surface)', marginBottom: '4px' }}>
                  {stat.label}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                  {stat.detail}
                </div>
              </div>
            ))}
          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 3: EXPLORE OPPORTUNITIES
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: 'var(--color-bg-page)' }}>
        <div className="container">
          
          <div className="section-header">
            <h2 className="section-title">Explore Opportunities on TrueHunt</h2>
            <p className="section-subtitle">
              Everything you need to discover talent, pitch to investors, secure crew roles, 
              or manage on-set operations in one unified cinematic network.
            </p>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
              gap: '24px'
            }}
          >
            {/* Card 1: Find Talents */}
            <div 
              className="card card-hoverable" 
              style={{
                padding: '32px 24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <div>
                <div 
                  style={{
                    width: '54px',
                    height: '54px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#EFF6FF',
                    color: '#1D4ED8',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '20px'
                  }}
                >
                  <Users size={28} />
                </div>
                <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '10px' }}>Find Talents</h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: '1.5', marginBottom: '24px' }}>
                  Discover professionals based on skills, experience, location, and film-industry role.
                </p>
              </div>
              <Link to="/talents" className="btn btn-outline-navy" style={{ width: '100%' }}>
                <span>Explore Talents</span>
                <ArrowRight size={15} />
              </Link>
            </div>
            {/* Card 3: Investors Connect */}
            <div 
              className="card card-hoverable" 
              style={{
                padding: '32px 24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <div>
                <div 
                  style={{
                    width: '54px',
                    height: '54px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#FEF3C7',
                    color: '#B45309',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '20px'
                  }}
                >
                  <TrendingUp size={28} />
                </div>
                <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '10px' }}>Investors Connect</h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: '1.5', marginBottom: '24px' }}>
                  Help suitable production teams and entertainment businesses discover investor opportunities.
                </p>
              </div>
              <Link to="/investors-connect" className="btn btn-outline-navy" style={{ width: '100%' }}>
                <span>Explore Investors</span>
                <ArrowRight size={15} />
              </Link>
            </div>

            {/* Card 4: LCM App */}
            <div 
              className="card card-hoverable" 
              style={{
                padding: '32px 24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <div>
                <div 
                  style={{
                    width: '54px',
                    height: '54px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#F3E8FF',
                    color: '#7E22CE',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '20px'
                  }}
                >
                  <Smartphone size={28} />
                </div>
                <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '10px' }}>LCM App</h3>
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: '1.5', marginBottom: '24px' }}>
                  Introduce the Location, Crew & Movie Management application through its dedicated portal.
                </p>
              </div>
              <Link to="/lcm-app" className="btn btn-outline-navy" style={{ width: '100%' }}>
                <span>Explore LCM App</span>
                <ArrowRight size={15} />
              </Link>
            </div>

          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 4: FEATURED TALENTS
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)' }}>
        <div className="container">
          
          <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '40px', gap: '16px' }}>
            <div>
              <span className="badge badge-maroon" style={{ marginBottom: '8px' }}>Verified Directory</span>
              <h2 className="section-title" style={{ marginBottom: '6px' }}>Featured Talents</h2>
              <p className="section-subtitle" style={{ margin: 0, textAlign: 'left' }}>
                Browse top film-industry professionals vetted for verified showreels and on-screen credits.
              </p>
            </div>
            <Link to="/talents" className="btn btn-outline-navy">
              <span>View All Talents ({visibleTalents.length}+)</span>
              <ArrowRight size={15} />
            </Link>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
              gap: '24px'
            }}
          >
            {visibleTalents.slice(0, 4).map((talent) => (
              <TalentCard key={talent.id} talent={talent} />
            ))}
          </div>
          {normalizedSearchTerm && visibleTalents.length === 0 && (
            <p className="home-search-empty" role="status">No talents match “{searchTerm.trim()}”.</p>
          )}

        </div>
      </section>

      {/* =========================================================================
          SECTION 5: BUILD YOUR CAREER IN CINEMA
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: '#F8FAFC' }}>
        <div className="container">
          
          <div className="section-header">
            <h2 className="section-title">Build Your Career in Cinema</h2>
            <p className="section-subtitle">
              TrueHunt provides the professional scaffolding you need to showcase verified filmography, 
              find auditions, and get hired by prestigious studios.
            </p>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
              gap: '28px'
            }}
          >
            {/* Step 1 */}
            <div 
              className="card"
              style={{
                padding: '32px 28px',
                borderRadius: 'var(--radius-md)',
                position: 'relative',
                borderTop: '4px solid var(--color-navy)'
              }}
            >
              <div 
                style={{
                  fontSize: '14px',
                  fontWeight: 800,
                  color: 'var(--color-navy)',
                  backgroundColor: 'var(--color-bg-subtle)',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                01
              </div>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '12px' }}>
                Create Your Talent Profile
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '24px' }}>
                Add your official film department, skills, verified experience, physical traits (for actors), 
                and private contact preferences.
              </p>
              <Link to="/join-now" className="btn btn-sm btn-outline-navy">
                <span>Start Your Profile</span>
              </Link>
            </div>

            {/* Step 2 */}
            <div 
              className="card"
              style={{
                padding: '32px 28px',
                borderRadius: 'var(--radius-md)',
                position: 'relative',
                borderTop: '4px solid var(--color-maroon)'
              }}
            >
              <div 
                style={{
                  fontSize: '14px',
                  fontWeight: 800,
                  color: 'var(--color-maroon)',
                  backgroundColor: 'var(--color-maroon-light)',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                02
              </div>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '12px' }}>
                Showcase Your Work
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '24px' }}>
                Display high-definition showreels, Vimeo links, theatrical credits, festival laurels, 
                and behind-the-scenes portfolios.
              </p>
              <Link to="/talents" className="btn btn-sm btn-outline-maroon">
                <span>See Profile Samples</span>
              </Link>
            </div>

            {/* Step 3 */}
            <div 
              className="card"
              style={{
                padding: '32px 28px',
                borderRadius: 'var(--radius-md)',
                position: 'relative',
                borderTop: '4px solid var(--color-brown-arrow)'
              }}
            >
              <div 
                style={{
                  fontSize: '14px',
                  fontWeight: 800,
                  color: 'var(--color-brown-arrow)',
                  backgroundColor: 'var(--color-brown-light)',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '18px'
                }}
              >
                03
              </div>
              <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '12px' }}>
                Connect with Filmmakers
              </h3>
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '24px' }}>
                Directly pitch to production houses, apply for verified auditions, collaborate with cinematographers, 
                and receive opportunities.
              </p>
              <Link to="/jobs" className="btn btn-sm btn-navy">
                <span>Browse Opportunities</span>
              </Link>
            </div>

          </div>

        </div>
      </section>

      {/* =========================================================================
          SECTION 6: MADE FOR EVERY FILM CRAFT
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: 'var(--color-white)', borderBottom: '1px solid var(--color-border)' }}>
        <div className="container">
          
          <div className="section-header">
            <span className="badge badge-navy" style={{ marginBottom: '8px' }}>12 Specialized Disciplines</span>
            <h2 className="section-title">Made for Every Film Craft</h2>
            <p className="section-subtitle">
              Cinema thrives when every department excels. Explore dedicated directories for each artistic and technical role.
            </p>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: '20px'
            }}
          >
            {visibleCrafts.map((craft) => (
              <CraftCard key={craft.id} craft={craft} />
            ))}
          </div>
          {normalizedSearchTerm && visibleCrafts.length === 0 && (
            <p className="home-search-empty" role="status">No film crafts match “{searchTerm.trim()}”.</p>
          )}

        </div>
      </section>

      {/* =========================================================================
          SECTION 7: LATEST INDUSTRY OPPORTUNITIES
          ========================================================================= */}
      <section className="section-padding" style={{ backgroundColor: 'var(--color-bg-page)' }}>
        <div className="container">
          
          <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '40px', gap: '16px' }}>
            <div>
              <span className="badge badge-maroon" style={{ marginBottom: '8px' }}>Active Production Calls</span>
              <h2 className="section-title" style={{ marginBottom: '6px' }}>Latest Industry Opportunities</h2>
              <p className="section-subtitle" style={{ margin: 0, textAlign: 'left' }}>
                Auditions, crew call sheets, and technical openings currently recruiting cast & crew.
              </p>
            </div>
            <Link to="/jobs" className="btn btn-outline-navy">
              <span>View All Opportunities ({visibleJobs.length}+)</span>
              <ArrowRight size={15} />
            </Link>
          </div>

          <div 
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
              gap: '24px'
            }}
          >
            {visibleJobs.slice(0, 3).map((job) => (
              <OpportunityCard key={job.id} job={job} />
            ))}
          </div>
          {normalizedSearchTerm && visibleJobs.length === 0 && (
            <p className="home-search-empty" role="status">No opportunities match “{searchTerm.trim()}”.</p>
          )}

        </div>
      </section>

      {/* =========================================================================
          SECTION 8: CALL TO ACTION BANNER (MAROON)
          ========================================================================= */}
      <section 
        style={{
          backgroundColor: 'var(--color-maroon)',
          color: 'var(--color-white)',
          paddingTop: '72px',
          paddingBottom: '72px',
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        {/* Subtle decorative cinema graphic */}
        <div 
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundImage: 'radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.12) 0%, transparent 60%)',
            pointerEvents: 'none'
          }}
        />

        <div className="container" style={{ position: 'relative', zIndex: 1, maxWidth: '780px' }}>
          
          <div 
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 14px',
              borderRadius: 'var(--radius-full)',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              color: 'var(--color-white)',
              fontSize: '13px',
              fontWeight: 600,
              marginBottom: '20px'
            }}
          >
            <Film size={15} />
            <span>truehunt.in • Join 50,000+ Film Creators</span>
          </div>

          <h2 
            style={{
              fontSize: 'clamp(32px, 4.5vw, 46px)',
              fontWeight: 800,
              color: 'var(--color-white)',
              marginBottom: '18px',
              letterSpacing: '-0.02em',
              lineHeight: '1.2'
            }}
          >
            Your next story starts here.
          </h2>

          <p 
            style={{
              fontSize: '18px',
              color: '#FEE2E2',
              lineHeight: '1.6',
              marginBottom: '36px',
              maxWidth: '620px',
              marginLeft: 'auto',
              marginRight: 'auto'
            }}
          >
            Join a growing professional community built exclusively for the people behind cinema. 
            Claim your talent profile or post your project opportunities today.
          </p>

          <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: '16px' }}>
            <Link 
              to="/join-now" 
              className="btn btn-lg"
              style={{
                backgroundColor: 'var(--color-white)',
                color: 'var(--color-maroon)',
                fontWeight: 700,
                boxShadow: '0 8px 20px rgba(0, 0, 0, 0.2)'
              }}
            >
              <span>Join TrueHunt</span>
              <ArrowRight size={18} />
            </Link>

            <Link 
              to="/talents" 
              className="btn btn-lg"
              style={{
                backgroundColor: 'transparent',
                color: 'var(--color-white)',
                border: '1.5px solid var(--color-white)',
                fontWeight: 600
              }}
            >
              <span>Explore Talents</span>
            </Link>
          </div>

        </div>
      </section>

    </div>
  );
}
