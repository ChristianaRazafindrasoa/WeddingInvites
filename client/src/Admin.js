import { useEffect, useState } from "react";
import "./index.css";

export default function AdminPanel() {
  const [guests, setGuests] = useState([]);
  const [rsvps, setRsvps] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const guestCount = guests.length;
  const attendingCount = guests.filter((guest) => guest.attending).length;

  useEffect(() => {
    setLoading(true);
    setError(null);

    Promise.all([
      fetch("http://localhost:8080/api/admin/guests").then((res) => {
        if (!res.ok) throw new Error("Failed to load guests");
        return res.json();
      }),
      fetch("http://localhost:8080/api/admin/rsvps").then((res) => {
        if (!res.ok) throw new Error("Failed to load RSVPs");
        return res.json();
      }),
    ])
      .then(([guestData, rsvpData]) => {
        setGuests(guestData);
        setRsvps(rsvpData);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <h2>Loading admin data...</h2>;
  }

  if (error) {
    return (
      <div className="container">
        <h2>Error loading dashboard...</h2>
        <p className="error">{error}</p>
      </div>
    );
  }

  return (
    <div className="container">
      <section>
        <h2>Guest List</h2>
        <p>Total guests: {guestCount} | Attending: {attendingCount}</p>
        <table className="admin-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Attending</th>
            </tr>
          </thead>
          <tbody>
            {guests.map((guest) => (
              <tr key={guest.id}>
                <td>{guest.fullName}</td>
                <td>{guest.attending ? "Yes" : "No"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2>RSVP Dashboard</h2>
        <table className="admin-table">
          <thead>
            <tr>
              <th>Token</th>
              <th>Main Guest</th>
              <th>Plus One</th>
              <th>Responded</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {rsvps.map((rsvp) => {
              const status = !rsvp.respondedAt ? "Pending" : rsvp.accepted ? "Accepted" : "Declined";
              return (
                <tr key={rsvp.id}>
                  <td>{rsvp.token}</td>
                  <td>{rsvp.mainGuest?.fullName ?? "-"}</td>
                  <td>{rsvp.plusOne?.fullName ?? "-"}</td>
                  <td>{rsvp.respondedAt ? new Date(rsvp.respondedAt).toLocaleString() : "-"}</td>
                  <td>
                    <span className={`status-dot ${status.toLowerCase()}`} />
                    {status}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
    </div>
  );
}