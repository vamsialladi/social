/**
 * Official TrueHunt Contact Information and Office Location Configuration
 * Centralized configuration used across Footer, Contact Us, About Us, Support, and Legal pages.
 */

export const OFFICIAL_CONTACT = {
  // Official Communication Channels
  email: 'sudhakar@appequal.com',
  phone: '+91 98480 74212',
  phoneTel: '+919848074212', // Formatted for international tel: dialing

  // Official Office & Organization Details
  organization: 'MJR Magnifique',
  locationName: 'K-Hub (India) — Office',
  address: 'Raidurgam, Prashant Hills, Hyderabad, Telangana – 500032, India.',

  // Map Search Information
  mapSearchQuery: 'MJR Magnifique, K-Hub (India) Office, Raidurgam, Prashant Hills, Hyderabad, Telangana 500032, India.',
  get mapUrl() {
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(this.mapSearchQuery)}`;
  }
};
