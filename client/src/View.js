import ringsIcon from "./assets/icon-rings.svg";
import champagneIcon from "./assets/icon-champagne.svg";

const EVENT_ICONS = [ringsIcon, champagneIcon];
const IS_PROD_SITE = window.location.hostname === process.env.REACT_APP_PROD_HOSTNAME;
const IS_LOCAL_SITE = window.location.hostname === process.env.REACT_APP_LOCAL_HOSTNAME;
const IS_DEMO_SITE = !IS_PROD_SITE && !IS_LOCAL_SITE;
const HONEYFUND_URL = process.env.REACT_APP_HONEYFUND_URL;

export default function InvitationView({
  wedding,
  noToken,
  phone,
  setPhone,
  lookingUp,
  lookupError,
  setLookupError,
  lookupByPhone,
  mainGuest,
  setMainGuest,
  plusOne,
  setPlusOne,
  allowPlusOne,
  token,
  submitRSVP,
  rsvpSubmitted,
  rsvpAccepted,
  rsvpDeadline,
  showMessage,
  setShowMessage,
  response,
  photos,
  showAllPhotos,
  setShowAllPhotos,
  visibleCount,
  setVisibleCount,
  sentinelRef,
  fileInputRef,
  uploading,
  handleFileChange,
  showUpload,
  setShowUpload,
  amount,
  setAmount,
  donating,
  handleDonation,
  donationError,
  setDonationError,
  showSuccess,
  setShowSuccess,
  guestName,
  setGuestName,
  guestMessage,
  setGuestMessage,
  submitGuestbook,
  submittingNote,
  guestbookError,
  setGuestbookError,
  guestbookSuccess,
  setGuestbookSuccess,
}) {
  if (noToken) {
    return (
      <>
        <div className="leaf-bg" />
        <div className="page">
          <div className="container">
            <div className="hero">
              <div className="hero-overlay">
                <div className="hero-kicker">The Wedding of</div>
                <h1>
                  {wedding.groomName}
                  <span className="hero-amp">&amp;</span>
                  {wedding.brideName}
                </h1>
              </div>
            </div>
            <section className="section" id="rsvp">
              <span className="section-kicker">Welcome</span>
              <h2>Find Your Invitation</h2>
              <div className="rsvp-form">
                <div>
                  <input className="name"
                    type="tel"
                    placeholder="e.g. 1234567890"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value.replace(/\D/g, ""))}
                    onKeyDown={(e) => { if (e.key === "Enter") lookupByPhone(); }} />
                  <div className="rsvp-actions">
                    <button onClick={lookupByPhone} disabled={lookingUp}>
                      {lookingUp ? "Looking up..." : "Find"}
                    </button>
                  </div>
                  <p className="rsvp-note">Enter the phone number your invitation was sent to.</p>
                  {lookupError && (
                    <div className="banner">
                      {lookupError} <br></br>
                      <p className="banner-signature">- {wedding.groomName} & {wedding.brideName} 🤍</p>
                      <button onClick={() => setLookupError(null)}>Close</button>
                    </div>
                  )}
                </div>
              </div>
            </section>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <div className="leaf-bg" />
      {mainGuest && (
        <div className="notice-banner">
          You're invited - welcome, dear {allowPlusOne ? "guests" : "guest"}!
        </div>
      )}
    <div className="page">
    <div className="container">
      <div className="hero">
        <div className="hero-overlay">
          <div className="hero-kicker">The Wedding of</div>
          <h1>
            {wedding.groomName}
            <span className="hero-amp">&amp;</span>
            {wedding.brideName}
          </h1>
          <div className="hero-meta">
            {new Date(wedding.weddingDate).toLocaleDateString("en-US",
              {weekday: "long", month: "long", day: "numeric", year: "numeric"})
              .replace(",", " •")
              .replace(",", " •")}
            <br />
            {wedding.city}
          </div>
        </div>
      </div>

      <section className="section" id="events">
        <span className="section-kicker">Join us for</span>
        <h2>Events</h2>
        <ul className="event-list">
          {wedding.events.map((event, index) => (
            <li key={index}>
              <span className="event-icon-badge">
                <img src={EVENT_ICONS[index % EVENT_ICONS.length]} alt="" />
              </span>
              <strong>{event.name}</strong>
              <div>{event.location}</div>
              <div><a href={`https://maps.apple.com/?q=${encodeURIComponent(event.address)}`}
                target="_blank" rel="noreferrer"
                className="address-link">{event.address}</a></div>
              <div className="event-time">{new Date(event.startTime).toLocaleTimeString([], {
                  hour: "2-digit",
                  minute: "2-digit"})}</div>
            </li>
          ))}
        </ul>
      </section>

      <section className="section" id="rsvp">
        <span className="section-kicker">Will you celebrate with us?</span>
        <h2>RSVP</h2>
        <div className="rsvp-form">
          <div>
            <input className="name"
              placeholder="e.g. John Doe"
              value={mainGuest}
              onChange={(e) => setMainGuest(e.target.value)}
              readOnly={!!token} />
            {allowPlusOne && (
              <input className="name"
                title="Plus one"
                placeholder="e.g. Jane Doe (optional)"
                value={plusOne}
                onChange={(e) => setPlusOne(e.target.value)}
                readOnly={!!token} />
            )}
            <div className="rsvp-actions">
              <button onClick={() => submitRSVP(true)} disabled={rsvpSubmitted}>Accept</button>
              <button className="btn-outline" onClick={() => submitRSVP(false)} disabled={rsvpSubmitted}>Decline</button>
            </div>
            <p className="rsvp-note">
              {rsvpSubmitted
                ? (rsvpAccepted ? "Accepted - see you there!" : "Declined - we'll miss you!")
                : `Kindly respond by ${rsvpDeadline.toLocaleDateString("en-US",
                    {month: "long", day: "numeric", year: "numeric"})}`}
            </p>
            {showMessage &&
              <div className="banner">
                {response.message} <br></br>
                <p className="banner-signature">- {wedding.groomName} & {wedding.brideName} 🤍</p>
                <button onClick={() => setShowMessage(false)}>Close</button>
              </div>}
          </div>
        </div>
      </section>

      <section className="section" id="gallery">
        <span className="section-kicker">Captured moments</span>
        <h2>Gallery</h2>
        <p className="section-lead">Find and upload photos here after the wedding.</p>
        <div style={{ marginTop: "40px" }}>
          <div className="gallery">
            {photos.slice(-5).map((photo, index) => (
              <img
                src={photo.url}
                alt={`${index + 1}`}
                key={photo.s3Key}
                className="gallery-img"
                title={`By ${photo.uploadedBy}`}
                loading="lazy"
              />
            ))}
          </div>
          <div className="gallery-actions">
            {photos.length > 0 && (
              <button className="view-all-btn" onClick={
                () => { setVisibleCount(12); setShowAllPhotos(true); }}>
                View All ({photos.length})
              </button>
            )}
            <input
              id="file-input"
              ref={fileInputRef}
              className="file-input"
              type="file"
              multiple
              accept="image/*"
              onChange={handleFileChange}/>
            <label htmlFor={uploading ? undefined : "file-input"}
                className="upload-btn" style={uploading ? {opacity: 0.7, cursor: "default"} : {}}>
              {uploading ? "Uploading..." : "Upload Photo"}
            </label>
          </div>
          {showAllPhotos && (
            <div className="photo-overlay">
              <div className="photo-overlay-header">
                <button className="photo-overlay-close" onClick={
                  () => setShowAllPhotos(false)}>Close</button>
              </div>
              <div className="photo-grid">
                {photos.slice(0, visibleCount).map((photo, index) => (
                  <img
                    src={photo.url}
                    alt={`${index + 1}`}
                    key={photo.s3Key}
                    className="photo-grid-img"
                    title={`By ${photo.uploadedBy}`}
                    loading="lazy"
                  />
                ))}
                {visibleCount < photos.length && (
                  <div ref={sentinelRef} style={{ gridColumn: "1 / -1", height: 40 }} />
                )}
              </div>
            </div>
          )}
          {showUpload &&
            <div className="banner">
              {response.message} <br></br>
              <p className="banner-signature">- {wedding.groomName} & {wedding.brideName} 🤍</p>
              <button onClick={() => { setShowUpload(false);
                  if (fileInputRef.current) {
                    fileInputRef.current.value = "";
                  }
                }}>Close</button>
            </div>}
        </div>
      </section>

      <section className="section" id="fund">
        <div className="fund-guestbook">
          <div className="registry">
            <span className="section-kicker">A little something</span>
            <h2>Honeymoon Fund</h2>
            <p className="section-lead">Your presence is the greatest gift, but if you'd like to contribute
              to our honeymoon fund, you can do so below.</p>
            {IS_DEMO_SITE && (
              <div className="registry-amount">
                <span className="dollar-sign">$</span>
                <input className="amount"
                  placeholder="0"
                  onKeyDown={(e) => {
                    if (!/[0-9]/.test(e.key) &&
                      !["Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab"].includes(e.key)) {
                      e.preventDefault();
                    }
                  }}
                  onChange={(e) => setAmount(e.target.value)}/>
              </div>
            )}
            {IS_DEMO_SITE && (
              <button onClick={handleDonation} disabled={donating}>
                {donating ? "Contributing..." : "Contribute"}</button>
            )}
            {!IS_DEMO_SITE && (
              <>
                <p className="fund-note">
                  <a href={HONEYFUND_URL} target="_blank" rel="noopener noreferrer">{HONEYFUND_URL}</a>
                </p>
                <button
                  onClick={() => window.open(HONEYFUND_URL, "_blank", "noopener,noreferrer")}
                  >
                  Contribute
                </button>
              </>
            )}
            {donationError && (
              <div className="banner">
                {donationError}<br/><br/>
                <p className="banner-signature">- {wedding.groomName} & {wedding.brideName} 🤍</p>
                <button onClick={() => setDonationError(null)}>Close</button>
              </div>
            )}
            {showSuccess && (
              <div className="banner">
                <p>Payment received: ${amount}</p>
                <p>Thank you for contributing to our honeymoon fund.</p>
                <p className="banner-signature">- {wedding.groomName} & {wedding.brideName} 🤍</p>
                <button onClick={() => {setShowSuccess(false); setAmount("");}}>Close</button>
              </div>
            )}
          </div>

          <div className="fund-guestbook-divider" />

          <div className="guestbook">
            <span className="section-kicker">Leave us a note</span>
            <h2>Guestbook</h2>
            <p className="section-lead">We'd love to hear from you — share a memory, a wish, or just a little love.</p>
            <div>
              <input
                className="name"
                placeholder="Your name (optional)"
                value={guestName}
                onChange={(e) => setGuestName(e.target.value)}
              />
              <textarea
                className="guestbook-textarea"
                placeholder="Write your message here..."
                value={guestMessage}
                onChange={(e) => {
                  setGuestMessage(e.target.value);
                  e.target.style.height = "auto";
                  e.target.style.height = e.target.scrollHeight + "px";
                }}
              />
              <button onClick={submitGuestbook} disabled={submittingNote}>
                {submittingNote ? "Submitting..." : "Submit"}
              </button>
            </div>
            {guestbookError && (
              <div className="banner">
                {guestbookError}<br/><br/>
                <p className="banner-signature">- {wedding.groomName} & {wedding.brideName} 🤍</p>
                <button onClick={() => setGuestbookError(null)}>Close</button>
              </div>
            )}
            {guestbookSuccess && (
              <div className="banner">
                <p>Your message has been added to the guestbook.</p>
                <p className="banner-signature">- {wedding.groomName} & {wedding.brideName} 🤍</p>
                <button onClick={() => setGuestbookSuccess(false)}>Close</button>
              </div>
            )}
          </div>
        </div>
      </section>

      <footer className="footer">
        <div className="footer-monogram">
          {wedding.groomName?.[0]} &amp; {wedding.brideName?.[0]}
        </div>
        <div className="footer-meta">
          {new Date(wedding.weddingDate).toLocaleDateString("en-US",
            {month: "long", day: "numeric", year: "numeric"})} · {wedding.city}
        </div>
      </footer>
    </div>
    </div>
    </>
  );
}
