import React, { useState } from 'react';
import { TrendingUp, Landmark, ShieldAlert, CheckCircle2, Send, Filter, MapPin, DollarSign, Info } from 'lucide-react';
import { INVESTORS_CONNECT_DATA } from '../services/mockData';
import { api } from '../services/api';
import { useNotification } from '../context/NotificationContext';

export default function InvestorsConnectPage() {
  const [investors, setInvestors] = useState(INVESTORS_CONNECT_DATA);
  const [selectedInvestor, setSelectedInvestor] = useState(null);
  const [showPitchModal, setShowPitchModal] = useState(false);
  const [projectTitle, setProjectTitle] = useState('');
  const [producerName, setProducerName] = useState('');
  const [producerEmail, setProducerEmail] = useState('');
  const [budgetRequested, setBudgetRequested] = useState('');
  const [lookbookUrl, setLookbookUrl] = useState('');
  const [synopsis, setSynopsis] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { showNotification } = useNotification();

  const handlePitchSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.investors.expressInterest({
        investorId: selectedInvestor?.id,
        investorName: selectedInvestor?.name,
        projectTitle,
        producerName,
        producerEmail,
        budgetRequested,
        lookbookUrl,
        synopsis
      });
      setShowPitchModal(false);
      showNotification(`Pitch package submitted to ${selectedInvestor?.name} (Demo Pipeline Sync)!`, 'success');
      // Reset form
      setProjectTitle('');
      setBudgetRequested('');
      setLookbookUrl('');
      setSynopsis('');
    } catch (e) {
      showNotification('Failed to submit pitch', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ backgroundColor: 'var(--color-bg-page)', minHeight: '85vh', paddingBottom: '80px' }}>
      
      {/* Banner */}
      <section style={{ backgroundColor: 'var(--color-navy)', color: 'white', padding: '52px 0', borderBottom: '3px solid var(--color-maroon)' }}>
        <div className="container">
          <div style={{ maxWidth: '720px' }}>
            <span className="badge badge-maroon" style={{ marginBottom: '10px' }}>Film Financing & Co-Production Network</span>
            <h1 style={{ fontSize: '36px', color: 'white', marginBottom: '12px' }}>
              Investors Connect
            </h1>
            <p style={{ fontSize: '16px', color: '#CBD5E1', lineHeight: '1.6' }}>
              Connect packaged screenplays, slate budgets, and post-production pipelines 
              with verified media financiers, angel syndicates, and boutique production partners.
            </p>
          </div>
        </div>
      </section>

      {/* Legal & Regulatory Disclaimer Card (Mandatory Requirement) */}
      <div className="container" style={{ marginTop: '24px' }}>
        <div 
          style={{
            backgroundColor: '#FFFBEB',
            border: '1px solid #FDE68A',
            padding: '16px 20px',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '14px',
            marginBottom: '32px'
          }}
        >
          <Info size={22} color="#B45309" style={{ flexShrink: 0, marginTop: '2px' }} />
          <div style={{ fontSize: '13px', color: '#92400E', lineHeight: '1.5' }}>
            <strong>Regulatory & Compliance Notice:</strong> TrueHunt operates solely as a discovery directory 
            facilitating peer-to-peer introductions between registered filmmakers and media financiers. 
            TrueHunt does not offer investment advice, does not promise financial returns, does not act as an escrow agent, 
            and does not guarantee film funding. All co-production and financing arrangements are subject to 
            independent legal due diligence between participating parties.
          </div>
        </div>

        {/* Directory Grid */}
        <div 
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))',
            gap: '28px'
          }}
        >
          {investors.map((inv) => (
            <div 
              key={inv.id}
              className="card card-hoverable"
              style={{
                padding: '28px',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                backgroundColor: 'var(--color-white)',
                border: '1px solid var(--color-border)'
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                  <span className="badge badge-navy" style={{ fontWeight: 600 }}>
                    {inv.category}
                  </span>
                  <span style={{ fontSize: '12px', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <MapPin size={13} /> {inv.location}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
                  <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: 0 }}>
                    {inv.name}
                  </h3>
                  {inv.verified && (
                    <CheckCircle2 size={18} fill="#2563EB" color="#FFFFFF" title="Verified Financier" />
                  )}
                </div>

                <div style={{ fontSize: '14px', color: 'var(--color-maroon)', fontWeight: 700, marginBottom: '14px' }}>
                  Typical Ticket Size: {inv.ticketSize}
                </div>

                <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.5', marginBottom: '18px' }}>
                  {inv.description}
                </p>

                {/* Focus Genres */}
                <div style={{ marginBottom: '14px' }}>
                  <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-text-muted)', display: 'block', marginBottom: '6px' }}>
                    Focus Genres & Formats:
                  </span>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    {inv.focusGenres?.map((g, idx) => (
                      <span key={idx} className="badge" style={{ fontSize: '11px', backgroundColor: '#F1F5F9', color: '#1E293B' }}>
                        {g}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Submission Criteria */}
                <div style={{ padding: '12px', borderRadius: 'var(--radius-xs)', backgroundColor: 'var(--color-bg-page)', border: '1px solid var(--color-border)', fontSize: '12px', color: 'var(--color-text-secondary)', marginBottom: '20px' }}>
                  <strong>Prerequisite:</strong> {inv.criteria}
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', paddingTop: '16px', borderTop: '1px solid var(--color-border)' }}>
                <button 
                  onClick={() => setSelectedInvestor(inv)}
                  className="btn btn-sm btn-outline-subtle"
                >
                  View Criteria
                </button>
                <button 
                  onClick={() => {
                    setSelectedInvestor(inv);
                    setShowPitchModal(true);
                  }}
                  className="btn btn-sm btn-maroon"
                >
                  <Send size={13} />
                  <span>Pitch Project</span>
                </button>
              </div>

            </div>
          ))}
        </div>

      </div>

      {/* Criteria Details Modal */}
      {selectedInvestor && !showPitchModal && (
        <div className="modal-overlay" onClick={() => setSelectedInvestor(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <span className="badge badge-maroon">{selectedInvestor.category}</span>
            <h2 style={{ fontSize: '22px', color: 'var(--color-navy)', marginTop: '8px', marginBottom: '4px' }}>
              {selectedInvestor.name}
            </h2>
            <p style={{ fontSize: '13px', color: 'var(--color-maroon)', fontWeight: 600, marginBottom: '16px' }}>
              Investment Ticket: {selectedInvestor.ticketSize}
            </p>

            <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', lineHeight: '1.6', marginBottom: '20px' }}>
              {selectedInvestor.description}
            </p>

            <div style={{ backgroundColor: '#F8FAFC', padding: '16px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border)', marginBottom: '24px' }}>
              <h4 style={{ fontSize: '14px', color: 'var(--color-navy)', marginBottom: '6px' }}>Packaging Requirements:</h4>
              <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', margin: 0 }}>
                {selectedInvestor.criteria}
              </p>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button onClick={() => setSelectedInvestor(null)} className="btn btn-outline-subtle">
                Close
              </button>
              <button onClick={() => setShowPitchModal(true)} className="btn btn-maroon">
                Submit Pitch Package
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Pitch Submission Modal */}
      {showPitchModal && (
        <div className="modal-overlay" onClick={() => setShowPitchModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '4px' }}>
              Submit Pitch to {selectedInvestor?.name}
            </h3>
            <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '18px' }}>
              Provide verified project lookbook and synopsis for internal review.
            </p>

            <form onSubmit={handlePitchSubmit}>
              <div className="form-group">
                <label className="form-label">Project / Film Working Title *</label>
                <input 
                  type="text" 
                  className="form-control" 
                  value={projectTitle}
                  onChange={(e) => setProjectTitle(e.target.value)}
                  placeholder="e.g. The Ghats of Silence (Feature Film)" 
                  required 
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div className="form-group">
                  <label className="form-label">Producer / Lead Name *</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    value={producerName}
                    onChange={(e) => setProducerName(e.target.value)}
                    placeholder="e.g. Kabir Sen" 
                    required 
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Producer Email *</label>
                  <input 
                    type="email" 
                    className="form-control" 
                    value={producerEmail}
                    onChange={(e) => setProducerEmail(e.target.value)}
                    placeholder="producer@films.in" 
                    required 
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div className="form-group">
                  <label className="form-label">Financing Sought (INR) *</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    value={budgetRequested}
                    onChange={(e) => setBudgetRequested(e.target.value)}
                    placeholder="e.g. ₹2.5 Cr" 
                    required 
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Pitch Deck / Lookbook URL *</label>
                  <input 
                    type="url" 
                    className="form-control" 
                    value={lookbookUrl}
                    onChange={(e) => setLookbookUrl(e.target.value)}
                    placeholder="https://drive.google.com/pitch.pdf" 
                    required 
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">One-Line Logline & Synopsis *</label>
                <textarea 
                  className="form-control" 
                  rows={3}
                  value={synopsis}
                  onChange={(e) => setSynopsis(e.target.value)}
                  placeholder="Brief narrative premise and key attachments (director, cast, locations)..." 
                  required 
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button type="button" onClick={() => setShowPitchModal(false)} className="btn btn-outline-subtle">
                  Cancel
                </button>
                <button type="submit" disabled={submitting} className="btn btn-maroon">
                  {submitting ? 'Submitting...' : 'Submit Pitch Package'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
