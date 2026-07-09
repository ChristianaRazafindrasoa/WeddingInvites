import { useEffect, useRef, useState } from "react";
import "./index.css";
import AdminPanel, { ErrorBoundary } from "./Admin";
import ringsIcon from "./assets/icon-rings.svg";
import champagneIcon from "./assets/icon-champagne.svg";

const EVENT_ICONS = [ringsIcon, champagneIcon];

function Invitation() {
  const [wedding, setWedding] = useState(null);
  const [mainGuest, setMainGuest] = useState("");
  const [plusOne, setPlusOne] = useState("");
  const [allowPlusOne, setAllowPlusOne] = useState(true);
  const [token, setToken] = useState("");
  const [photos, setPhotos] = useState([]);
  const [showAllPhotos, setShowAllPhotos] = useState(false);
  const [visibleCount, setVisibleCount] = useState(12);
  const sentinelRef = useRef(null);
  const fileInputRef = useRef(null);
  const [amount, setAmount] = useState("");
  const [response, setResponse] = useState(null);
  const [showSuccess, setShowSuccess] = useState(false);
  const [showUpload, setShowUpload] = useState(false);
  const [showMessage, setShowMessage] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [donating, setDonating] = useState(false);
  const [donationError, setDonationError] = useState(null);
  const [noToken, setNoToken] = useState(false);
  const [guestName, setGuestName] = useState("");
  const [guestMessage, setGuestMessage] = useState("");
  const [guestbookSuccess, setGuestbookSuccess] = useState(false);
  const [guestbookError, setGuestbookError] = useState(null);
  const [submittingNote, setSubmittingNote] = useState(false);

  useEffect(() => {
    fetch("/api/info")
      .then((res) => res.json())
      .then((data) => setWedding(data));
    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get("token");
    if (!urlToken) {
      setNoToken(true);
      return;
    }

    setToken(urlToken);
    fetch(`/api/rsvp?token=${encodeURIComponent(urlToken)}`)
      .then((res) => {
        if (!res.ok) {
          setNoToken(true);
          return null;
        }
        return res.json();
      })
      .then((data) => {
        if (!data) return;
        setMainGuest(data.mainGuestName || "");
        setPlusOne(data.plusOneName || "");
        setAllowPlusOne(data.hasPlusOne === true);
        fetch("/api/photo-gallery")
          .then((res) => res.json())
          .then((photos) => setPhotos(photos));
      })
      .catch(() => setNoToken(true));
  }, []);

  const submitRSVP = async (attending) => {
    try {
      const confirmed = window.confirm(
        `Are you sure you want to ${attending ? "accept" : "decline"}? 1 submission allowed.`
      );
      if (!confirmed) {
        return;
      }
      const response = await fetch("/api/rsvp", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          token,
          mainGuestName: mainGuest,
          plusOneName: plusOne,
          isAccepted: attending,
        }),
      });
      const data = await response.json();
      setResponse({ message: data.message || data.error || "RSVP failed. Please try again later." });
      setShowMessage(true);
    } catch {
      setResponse({ message: "RSVP failed. Please try again later." });
    }
  };

  const handleFileChange = async (e) => {
    const selected = Array.from(e.target.files);
    if (selected.length === 0) return;
    await uploadPhotos(selected);
  };

  const compressImage = (file, maxWidth = 1200, quality = 0.75) => {
    return new Promise((resolve) => {
      const img = new Image();
      img.src = URL.createObjectURL(file);
      img.onload = () => {
        const canvas = document.createElement("canvas");
        const scale = Math.min(1, maxWidth / img.width);
        canvas.width = img.width * scale;
        canvas.height = img.height * scale;
        canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
        canvas.toBlob((blob) => resolve(
          new File([blob], file.name, { type: "image/jpeg" })), "image/jpeg", quality);
      };
    });
  };

  const uploadPhotos = async (selectedFiles) => {
    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get("token");
    if (!urlToken) {
      setResponse({ message: "You must be an invited guest to upload photos."});
      clearUpload();
      return;
    }
    if (selectedFiles.length > 5) {
      setResponse({ message: "Please upload up to 5 photos at a time."});
      clearUpload();
      return;
    }
    setUploading(true);
    try {
      for (const rawFile of selectedFiles) {
        const file = await compressImage(rawFile);
        const presignResponse = await fetch(
          "/api/photos/upload",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify({
              fileName: file.name,
              contentType: file.type,
              token: urlToken
            })
          }
        );

        const presignData = await presignResponse.json();
        const uploadResponse = await fetch(
          presignData.uploadUrl,
          {
            method: "PUT",
            headers: {
              "Content-Type": file.type
            },
            body: file
          }
        );
        if (!uploadResponse.ok) {
          throw new Error("Upload failed");
        }

        await fetch(
          "/api/photos/save",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify({
              s3Key: presignData.s3Key,
              token: urlToken
            })
          }
        );
      }
      fetch("/api/photo-gallery")
        .then((res) => res.json())
        .then((updated) => setPhotos(updated));
      setResponse({ message: "Upload succeeded. " });
    } catch {
      setResponse({ message: "Upload failed. Please try again later." });
    }
    clearUpload();
  };

  const clearUpload = () => {
    setUploading(false);
    setShowUpload(true);
  }

  const submitGuestbook = async () => {
    if (!guestMessage.trim()) {
      setGuestbookError("Please write a message before submitting.");
      return;
    }
    setSubmittingNote(true);
    setGuestbookError(null);
    try {
      const res = await fetch("/api/guestbook", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, message: guestMessage, name: guestName.trim() || "" }),
      });
      if (!res.ok) {
        const data = await res.json();
        setGuestbookError(data.error || "Something went wrong. Please try again.");
        return;
      }
      setGuestMessage("");
      setGuestName("");
      setGuestbookSuccess(true);
    } catch {
      setGuestbookError("Unable to submit. Please try again.");
    } finally {
      setSubmittingNote(false);
    }
  };

  const handleDonation = async () => {
    if (amount <= 0) {
      return;
    }
    setDonating(true);
    setDonationError(null);
    try {
      const response = await fetch("/api/honeymoon-fund", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          amount: amount,
          token: token,
          name: mainGuest
        })
      });
      const data = await response.json();
      if (!response.ok) {
        setDonationError(data.error || "Something went wrong. Please try again.");
        setDonating(false);
        return;
      }
      window.location.href = data.url;
    } catch {
      setDonationError("Unable to process payment. Please try again.");
      setDonating(false);
    }
  }

  useEffect(() => {
    const handlePageShow = (e) => { if (e.persisted) setDonating(false); };
    window.addEventListener("pageshow", handlePageShow);
    return () => window.removeEventListener("pageshow", handlePageShow);
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (!noToken && params.get("success") === "true") {
      setShowSuccess(true);
      const sessionId = params.get("id");
      const token = params.get("token");
      fetch(`/api/checkout-session/${sessionId}`)
      .then(res => {
        if (!res.ok) {
          throw new Error("Failed to retrieve session");
        }
        return res.json();
      })
      .then(data => { setAmount(data.amount); })
      .catch(err => { console.error(err); });
      window.history.replaceState({}, "", `/?token=${token}`);
    }
  }, [noToken]);

  useEffect(() => {
    if (!showAllPhotos) return;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setVisibleCount((c) => c + 8); },
      { threshold: 0.1 }
    );
    const el = sentinelRef.current;
    if (el) observer.observe(el);
    return () => { if (el) observer.unobserve(el); };
  }, [showAllPhotos, visibleCount]);

  useEffect(() => {
    if (!showAllPhotos) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = prevOverflow; };
  }, [showAllPhotos]);

  if (!wedding) {
    return <h2 className="loading">Loading wedding data...</h2>;
  }

  const rsvpDeadline = new Date(wedding.weddingDate);
  rsvpDeadline.setMonth(rsvpDeadline.getMonth() - 1);
  rsvpDeadline.setDate(rsvpDeadline.getDate() - 5);

  const scrollToRsvp = () => {
    const el = document.getElementById("rsvp");
    if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <>
      <div className="leaf-bg" />
      {noToken && (
        <div className="notice-banner">
          Token not found. Please contact us.
        </div>
      )}
      {!noToken && mainGuest && (
        <div className="notice-banner">
          You're invited — welcome, dear {allowPlusOne ? "guests" : "guest"}
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
              <button onClick={() => submitRSVP(true)} disabled={noToken}>Accept</button>
              <button className="btn-outline" onClick={() => submitRSVP(false)} disabled={noToken}>Decline</button>
            </div>
            <p className="rsvp-note">
              Kindly respond by {rsvpDeadline.toLocaleDateString("en-US",
                {month: "long", day: "numeric", year: "numeric"})}
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
            <label htmlFor={uploading || noToken ? undefined : "file-input"}
                className="upload-btn" style={uploading || noToken ? {opacity: 0.7, cursor: "default"} : {}}>
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
            <button onClick={handleDonation} disabled={donating || noToken}>
              {donating ? "Contributing..." : "Contribute"}</button>
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
              <button onClick={submitGuestbook} disabled={submittingNote || noToken}>
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
    <button className="rsvp-sticky-btn" onClick={scrollToRsvp}>RSVP Now</button>
    </>
  );
}

function NotFound() {
  return (
    <div className="page">
    <div className="container">
      <div className="hero">
        <div className="hero-overlay">
          <h1>Page Not Found</h1>
          <div className="hero-meta">Contact us for your personal invitation link. 🤍</div>
        </div>
      </div>
    </div>
    </div>
  );
}

const knownPaths = ["/", "/admin"];

export default function App() {
  const path = window.location.pathname;
  return (
    <ErrorBoundary>
      {!knownPaths.includes(path)
        ? <NotFound />
        : path === "/admin"
        ? <AdminPanel />
        : <Invitation />}
    </ErrorBoundary>
  );
}
