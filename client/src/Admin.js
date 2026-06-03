import { useEffect, useState } from "react";
import "./index.css";

export default function AdminPanel() {
  const [guests, setGuests] = useState([]);
  const [rsvps, setRsvps] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);

    Promise.all([
      fetch("http://localhost:8080/api/admin/guests", { cache: "no-store" }).then((res) => {
        if (!res.ok) throw new Error("Failed to load guests");
        return res.json();
      }),
      fetch("http://localhost:8080/api/admin/rsvps", { cache: "no-store" }).then((res) => {
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
        <h2>RSVP List</h2>
        <table className="admin-table">
          <thead>
            <tr>
              <th>Token</th>
              <th>Main Guest</th>
              <th>Plus One</th>
              <th>Responded</th>
              <th>Accepted</th>
            </tr>
          </thead>
          <tbody>
            {rsvps.map((rsvp) => (
              <tr key={rsvp.id}>
                <td>{rsvp.token}</td>
                <td>{rsvp.mainGuest?.fullName ?? "-"}</td>
                <td>{rsvp.plusOne?.fullName ?? "-"}</td>
                <td>{rsvp.respondedAt ? new Date(rsvp.respondedAt).toLocaleString() : "No"}</td>
                <td>{rsvp.accepted ? "Yes" : "No"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
